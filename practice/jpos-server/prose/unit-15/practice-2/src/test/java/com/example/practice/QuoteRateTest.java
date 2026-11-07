package com.example.practice;

import org.jpos.iso.ISOMsg;
import org.jpos.q2.Q2;
import org.jpos.space.SpaceFactory;
import org.jpos.transaction.AbortParticipant;
import org.jpos.transaction.Context;
import org.jpos.transaction.ContextConstants;
import org.jpos.transaction.TransactionParticipant;
import org.jpos.util.NameRegistrar;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs QuoteRate in a real TransactionManager with ONE session, deployed by a real Q2 in this JVM.
 * After QuoteRate comes AskIssuer, which pauses too until the issuer answers, then Decide, which
 * answers 00 with the issuer's approval or 91 without it. The exchange-rate service and the
 * issuer answer when the test says so. QuoteRate's timeout is 1000 ms.
 */
class QuoteRateTest {
    private static final String TM = "practice-dcc";
    private static final String QUEUE = "txn.dcc";
    private static final TestFx FX = new TestFx();

    @Test
    void aQuoteInTimeIsOffered() throws Exception {
        FX.instant.put(4410L, 1084500L);
        Run run = Run.start(purchase("000000004410"), "ISSUER_NOW");
        assertEquals("00 rate=1084500", run.outcome());
    }

    @Test
    void noQuoteInTimeMeansNoOffer() throws Exception {
        Run run = Run.start(purchase("000000004420"), "ISSUER_NOW");
        assertEquals("00 rate=none", run.outcome(), "after the timeout the purchase goes on without a rate");
    }

    @Test
    void aFailedQuoteMeansNoOffer() throws Exception {
        FX.failing.add(4430L);
        Run run = Run.start(purchase("000000004430"), "ISSUER_NOW");
        assertEquals("00 rate=none", run.outcome());
    }

    @Test
    void aLateQuoteLeavesTheIssuerPauseAlone() throws Exception {
        Run run = Run.start(purchase("000000004440"));
        FX.awaitAsked(4440L);
        run.awaitIssuerAsked();                  // the quote timed out and the issuer is being asked
        FX.release(4440L, 1091200L);             // now the quote arrives, late
        assertNull(run.outcomeWithin(1500), "the purchase must still be waiting for the issuer");
        run.issuerAnswers();
        assertEquals("00", run.outcome().substring(0, 2), "the issuer's approval reaches the answer");
    }

    @Test
    void aFailureFollowedByTheTimeoutLeavesTheIssuerPauseAlone() throws Exception {
        FX.failing.add(4450L);
        Run run = Run.start(purchase("000000004450"));
        run.awaitIssuerAsked();                  // the quote failed and the issuer is being asked
        assertNull(run.outcomeWithin(2500), "the purchase must still be waiting for the issuer after the timeout");
        run.issuerAnswers();
        assertEquals("00", run.outcome().substring(0, 2), "the issuer's approval reaches the answer");
    }

    /** Lets every transaction of the test finish, whatever the code under test did. */
    @AfterEach
    void releaseEverything() {
        for (Run r : STARTED)
            r.issuerAnswers();
        STARTED.clear();
    }

    // ---- requests ----

    private static ISOMsg purchase(String amount) throws Exception {
        ISOMsg m = new ISOMsg("0200");
        m.set(2, "5555555555554444");
        m.set(3, "000000");
        m.set(4, amount);
        m.set(11, String.format("%06d", 900 + STANS.incrementAndGet()));
        m.set(41, "29110093");
        m.set(49, "826");
        return m;
    }

    // ---- test support: Q2 in this JVM, a one-session TransactionManager ----

    private static final AtomicInteger STANS = new AtomicInteger();
    private static final AtomicInteger RUNS = new AtomicInteger();
    private static final List<Run> STARTED = new CopyOnWriteArrayList<>();
    private static final Map<String, Run> BY_ID = new ConcurrentHashMap<>();
    private static Q2 q2;

    @BeforeAll
    static void startQ2() throws Exception {
        NameRegistrar.register("practice-fx", FX);
        Path deploy = Files.createTempDirectory("deploy");
        Files.writeString(deploy.resolve("30_txnmgr.xml"),
            "<txnmgr name='" + TM + "' class='org.jpos.transaction.TransactionManager'>\n"
          + "  <property name='queue' value='" + QUEUE + "'/>\n"
          + "  <property name='sessions' value='1'/>\n"
          + "  <property name='debug' value='false'/>\n"
          + "  <participant class='com.example.practice.QuoteRate'>\n"
          + "    <property name='service' value='practice-fx'/>\n"
          + "    <property name='timeout' value='1000'/>\n"
          + "  </participant>\n"
          + "  <participant class='com.example.practice.QuoteRateTest$AskIssuer'/>\n"
          + "  <participant class='com.example.practice.QuoteRateTest$Decide'/>\n"
          + "  <participant class='com.example.practice.QuoteRateTest$Finished'/>\n"
          + "</txnmgr>\n");
        q2 = new Q2(deploy.toString());
        q2.start();
        assertNotNull(NameRegistrar.get(TM, 30000), "the TransactionManager did not start");
    }

    @AfterAll
    static void stopQ2() {
        q2.shutdown(true);
        NameRegistrar.unregister("practice-fx");
    }

    /** One transaction, queued as IncomingListener queues it. */
    static final class Run {
        final String id = "run." + RUNS.incrementAndGet();
        final CountDownLatch issuerAsked = new CountDownLatch(1);
        volatile Context ctx;

        static Run start(ISOMsg request, String... flags) {
            Run run = new Run();
            STARTED.add(run);
            BY_ID.put(run.id, run);
            Context ctx = new Context();
            ctx.put(ContextConstants.REQUEST.toString(), request);
            ctx.put("RUN", run.id);
            for (String f : flags)
                ctx.put(f, Boolean.TRUE);
            SpaceFactory.getSpace().out(QUEUE, ctx);
            return run;
        }

        /** The answer Decide gave ("00 rate=..." or "91 rate=..."), or null if not finished within ms. */
        String outcomeWithin(long ms) {
            return (String) SpaceFactory.getSpace().in(id, ms);
        }

        String outcome() {
            String o = outcomeWithin(10000);
            assertNotNull(o, "the transaction did not finish");
            return o;
        }

        void awaitIssuerAsked() throws InterruptedException {
            assertTrue(issuerAsked.await(20, TimeUnit.SECONDS), "the issuer was never asked");
        }

        /** The issuer approves, once. */
        synchronized void issuerAnswers() {
            Context c = ctx;
            ctx = null;
            if (c != null) {
                c.put("ISSUER", "00");
                c.resume();
            }
        }
    }

    /** The exchange-rate service: answers at once, fails at once, or holds the quote until released. */
    static final class TestFx implements QuoteRate.FxService {
        final Map<Long, Long> instant = new ConcurrentHashMap<>();
        final List<Long> failing = new CopyOnWriteArrayList<>();
        private final Map<Long, Listener> held = new ConcurrentHashMap<>();
        private final Map<Long, CountDownLatch> asked = new ConcurrentHashMap<>();
        private final ExecutorService threads = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "fx-service");
            t.setDaemon(true);
            return t;
        });

        @Override
        public void quote(String currency, long amount, Listener listener) {
            Long rate = instant.get(amount);
            if (rate != null)
                threads.submit(() -> listener.quoted(rate));
            else if (failing.contains(amount))
                threads.submit(() -> listener.failed(new IllegalStateException("rates unavailable")));
            else
                held.put(amount, listener);
            latch(amount).countDown();
        }

        void awaitAsked(long amount) throws InterruptedException {
            assertTrue(latch(amount).await(20, TimeUnit.SECONDS), "the rate service was never asked");
        }

        /** Delivers a held quote, and returns once the listener has run. */
        void release(long amount, long rate) throws Exception {
            Listener l = held.remove(amount);
            assertNotNull(l, "no quote held for " + amount);
            threads.submit(() -> l.quoted(rate)).get(10, TimeUnit.SECONDS);
        }

        private CountDownLatch latch(long amount) {
            return asked.computeIfAbsent(amount, a -> new CountDownLatch(1));
        }
    }

    /** Asks the issuer and pauses until it answers: at once with ISSUER_NOW, else when the test says. */
    public static class AskIssuer implements TransactionParticipant {
        @Override
        public int prepare(long id, Serializable context) {
            Context ctx = (Context) context;
            Run run = BY_ID.get(ctx.getString("RUN"));
            run.ctx = ctx;
            run.issuerAsked.countDown();
            if (ctx.get("ISSUER_NOW") != null)
                new Thread(run::issuerAnswers, "issuer").start();
            return PREPARED | PAUSE | NO_JOIN | READONLY;
        }
    }

    /** Answers 00 with the issuer's approval, 91 without it, and notes the rate if there is one. */
    public static class Decide implements TransactionParticipant {
        @Override
        public int prepare(long id, Serializable context) {
            Context ctx = (Context) context;
            Object rate = ctx.get(QuoteRate.RATE);
            String rc = ctx.get("ISSUER") != null ? "00" : "91";
            ctx.put("OUTCOME", rc + " rate=" + (rate != null ? rate : "none"));
            return rc.equals("00") ? PREPARED | NO_JOIN : ABORTED | NO_JOIN;
        }
    }

    /** Last in the chain, on both paths: tells the test what Decide answered. */
    public static class Finished implements AbortParticipant {
        @Override
        public int prepare(long id, Serializable context) {
            return PREPARED | READONLY;
        }

        @Override
        public void commit(long id, Serializable context) {
            report((Context) context);
        }

        @Override
        public void abort(long id, Serializable context) {
            report((Context) context);
        }

        private static void report(Context ctx) {
            String outcome = ctx.getString("OUTCOME");
            SpaceFactory.getSpace().out(ctx.getString("RUN"), outcome != null ? outcome : "no decision");
        }
    }
}

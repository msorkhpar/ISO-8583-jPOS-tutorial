package com.example.practice;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOSource;
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
 * Runs ScoreFraud in a real TransactionManager with ONE session, deployed by a real Q2, with the
 * real SendResponse. The scorer answers at once for some STANs and holds the others until the
 * test releases them.
 */
class ScoreFraudTest {
    private static final String REQUEST = ContextConstants.REQUEST.toString();
    private static final String SOURCE = ContextConstants.SOURCE.toString();
    private static final TestScorer SCORER = new TestScorer();

    @Test
    void aLowScoreIsApproved() throws Exception {
        SCORER.instant.put("000611", 140);
        Run run = Run.start(purchase("29110081", "000611"));
        assertTrue(run.finish(), "the transaction commits");
        assertEquals("00", run.answer().getString(39));
    }

    @Test
    void aHighScoreIsDeclined() throws Exception {
        SCORER.instant.put("000612", 930);
        Run run = Run.start(purchase("29110082", "000612"));
        run.finish();
        assertEquals("59", run.answer().getString(39));
    }

    @Test
    void aSlowScoreDoesNotHoldUpTheNextPurchase() throws Exception {
        Run slow = Run.start(purchase("29110083", "000613"));
        SCORER.awaitAsked("000613");
        try {
            SCORER.instant.put("000614", 220);
            Run quick = Run.start(purchase("29110084", "000614"));
            assertNotNull(quick.finishWithin(10000),
                "the second purchase was answered while the first one's score was still pending");
            assertEquals("00", quick.answer().getString(39));
            assertNull(slow.finishWithin(0), "the first purchase is still waiting for its score");
        } finally {
            SCORER.release("000613", 310);
        }
        assertTrue(slow.finish());
        assertEquals("00", slow.answer().getString(39));
    }

    @Test
    void scoresArrivingInReverseOrderReachTheirOwnPurchases() throws Exception {
        Run first = Run.start(purchase("29110085", "000615"));
        SCORER.awaitAsked("000615");
        Run second = Run.start(purchase("29110086", "000616"));
        SCORER.awaitAsked("000616");
        SCORER.release("000616", 870);
        SCORER.release("000615", 90);
        assertNotNull(second.finishWithin(10000), "the second purchase was answered");
        assertNotNull(first.finishWithin(10000), "the first purchase was answered");
        assertEquals("59", second.answer().getString(39), "the second purchase scored 870");
        assertEquals("00", first.answer().getString(39), "the first purchase scored 90");
    }

    @Test
    void aPurchaseTheScorerCannotScoreStillGetsItsAnswer() throws Exception {
        Run run = Run.start(purchase("29110087", "000617"));
        SCORER.awaitAsked("000617");
        SCORER.giveUp("000617");
        assertNotNull(run.finishWithin(10000), "the purchase was answered");
        assertEquals("00", run.answer().getString(39), "without a score the purchase goes on");
    }

    /** Lets every purchase of the test finish, whatever the code under test did, so the next test starts clean. */
    @AfterEach
    void answerEverythingStillWaiting() {
        for (String stan : STARTED) {
            SCORER.instant.putIfAbsent(stan, 0);
            SCORER.release(stan, 0);
        }
        STARTED.clear();
    }

    // ---- requests ----

    private static ISOMsg purchase(String terminal, String stan) throws Exception {
        ISOMsg m = new ISOMsg("0200");
        m.set(2, "4012888888881881");
        m.set(3, "000000");
        m.set(4, "000000008425");
        m.set(11, stan);
        m.set(41, terminal);
        return m;
    }

    // ---- test support: Q2 in this JVM, deploying a one-session TransactionManager ----

    private static Q2 q2;

    @BeforeAll
    static void startQ2() throws Exception {
        NameRegistrar.register("practice-scorer", SCORER);
        Path deploy = Files.createTempDirectory("deploy");
        Files.writeString(deploy.resolve("30_txnmgr.xml"),
            "<txnmgr name='practice-scoring' class='org.jpos.transaction.TransactionManager'>\n"
          + "  <property name='queue' value='txn.scoring'/>\n"
          + "  <property name='sessions' value='1'/>\n"
          + "  <property name='debug' value='false'/>\n"
          + "  <participant class='com.example.practice.ScoreFraud'>\n"
          + "    <property name='scorer' value='practice-scorer'/>\n"
          + "  </participant>\n"
          + "  <participant class='com.example.practice.ScoreFraudTest$Decide'/>\n"
          + "  <participant class='org.jpos.transaction.participant.SendResponse'/>\n"
          + "  <participant class='com.example.practice.ScoreFraudTest$Finished'/>\n"
          + "</txnmgr>\n");
        q2 = new Q2(deploy.toString());
        q2.start();
        assertNotNull(NameRegistrar.get("practice-scoring", 30000), "the TransactionManager did not start");
    }

    @AfterAll
    static void stopQ2() {
        q2.shutdown(true);
        NameRegistrar.unregister("practice-scorer");
    }

    /** One transaction, queued as IncomingListener queues it. */
    static final class Run {
        final List<ISOMsg> sent = new CopyOnWriteArrayList<>();
        final String id = "run." + RUNS.incrementAndGet();

        ISOMsg answer() {
            assertEquals(1, sent.size(), "the terminal gets one answer");
            return sent.get(0);
        }

        static Run start(ISOMsg request) {
            Run run = new Run();
            STARTED.add(request.getString(11));
            Context ctx = new Context();
            ctx.put(REQUEST, request);
            ctx.put(SOURCE, new ISOSource() {
                @Override public void send(ISOMsg m) { run.sent.add(m); }
                @Override public boolean isConnected() { return true; }
            });
            ctx.put("RUN", run.id);
            SpaceFactory.getSpace().out("txn.scoring", ctx);
            return run;
        }

        /** True if committed, false if aborted, null if not finished within ms. */
        Boolean finishWithin(long ms) {
            return ms > 0 ? (Boolean) SpaceFactory.getSpace().in(id, ms) : (Boolean) SpaceFactory.getSpace().inp(id);
        }

        boolean finish() {
            Boolean c = finishWithin(20000);
            assertNotNull(c, "the transaction did not finish");
            return c;
        }
    }

    private static final AtomicInteger RUNS = new AtomicInteger();
    private static final List<String> STARTED = new CopyOnWriteArrayList<>();

    /** The scorer: answers at once, on its own thread, for STANs in `instant`; holds the rest. */
    static final class TestScorer implements ScoreFraud.Scorer {
        final Map<String, Integer> instant = new ConcurrentHashMap<>();
        private final Map<String, Object[]> held = new ConcurrentHashMap<>();
        private final Map<String, CountDownLatch> asked = new ConcurrentHashMap<>();
        private final ExecutorService threads = Executors.newCachedThreadPool();

        @Override
        public void score(ISOMsg request, Listener listener, Object handBack) {
            String stan = request.getString(11);
            Integer score = instant.get(stan);
            if (score != null)
                threads.submit(() -> listener.scored(score, handBack));
            else
                held.put(stan, new Object[] { listener, handBack });
            latch(stan).countDown();
        }

        void awaitAsked(String stan) throws InterruptedException {
            if (!latch(stan).await(20, TimeUnit.SECONDS))
                throw new AssertionError("the scorer was never asked about " + stan);
        }

        void release(String stan, int score) {
            Object[] h = held.remove(stan);
            if (h != null) threads.submit(() -> ((Listener) h[0]).scored(score, h[1]));
        }

        void giveUp(String stan) {
            Object[] h = held.remove(stan);
            if (h != null) threads.submit(() -> ((Listener) h[0]).unavailable(h[1]));
        }

        private CountDownLatch latch(String stan) {
            return asked.computeIfAbsent(stan, s -> new CountDownLatch(1));
        }
    }

    /** Decides on the score: 800 and above is declined with 59, anything else (or none) approved. */
    public static class Decide implements TransactionParticipant {
        @Override
        public int prepare(long id, Serializable context) {
            Context ctx = (Context) context;
            try {
                ISOMsg response = (ISOMsg) ((ISOMsg) ctx.get(REQUEST)).clone();
                response.setResponseMTI();
                Integer score = ctx.get(ScoreFraud.SCORE);
                boolean fraud = score != null && score >= 800;
                response.set(39, fraud ? "59" : "00");
                ctx.put(ContextConstants.RESPONSE.toString(), response);
                return fraud ? ABORTED | NO_JOIN : PREPARED | NO_JOIN;
            } catch (Exception e) {
                return ABORTED | NO_JOIN;
            }
        }
    }

    /** Last in the chain, on both paths: tells the test how the transaction ended. */
    public static class Finished implements AbortParticipant {
        @Override
        public int prepare(long id, Serializable context) {
            return PREPARED | READONLY;
        }

        @Override
        public void commit(long id, Serializable context) {
            SpaceFactory.getSpace().out(((Context) context).getString("RUN"), Boolean.TRUE);
        }

        @Override
        public void abort(long id, Serializable context) {
            SpaceFactory.getSpace().out(((Context) context).getString("RUN"), Boolean.FALSE);
        }
    }
}

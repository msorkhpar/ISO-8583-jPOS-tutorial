package com.example.practice;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOSource;
import org.jpos.q2.Q2;
import org.jpos.space.SpaceFactory;
import org.jpos.transaction.Context;
import org.jpos.transaction.ContextConstants;
import org.jpos.transaction.TransactionManager;
import org.jpos.transaction.TransactionParticipant;
import org.jpos.util.NameRegistrar;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StuckWatchTest {
    private static final long LIMIT = 30_000;

    @Test
    void aHungTransactionIsNamed() throws Exception {
        TransactionManager tm = (TransactionManager) NameRegistrar.get("practice-watch");
        StuckWatch watch = new StuckWatch(LIMIT);

        BlockingQueue<ISOMsg> hungAnswer = send(purchase("29110042", "000901"));
        assertTrue(Hang.ENTERED.await(20, TimeUnit.SECONDS), "the hung purchase did not reach the issuer call");
        long hungId = Hang.ID.get();

        assertNotNull(send(purchase("29110001", "000902")).poll(20, TimeUnit.SECONDS), "a second session answers");
        assertEquals(-1, watch.poll(tm.getHead(), tm.getTail(), 1_000), "one second in, nothing is stuck yet");

        assertNotNull(send(purchase("29110001", "000903")).poll(20, TimeUnit.SECONDS), "a second session answers");
        assertEquals(hungId, watch.poll(tm.getHead(), tm.getTail(), 40_000), "the hung transaction, after 39 seconds");

        Hang.RELEASE.countDown();
        assertNotNull(hungAnswer.poll(20, TimeUnit.SECONDS), "the released purchase is answered");
        long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        while (tm.getTail() != tm.getHead() && System.nanoTime() < until)
            Thread.onSpinWait();                            // waits for the condition, not for a time
        assertEquals(tm.getHead(), tm.getTail(), "every transaction has finished");
        assertEquals(-1, watch.poll(tm.getHead(), tm.getTail(), 41_000), "once it finishes, nothing is stuck");
    }

    @Test
    void anIdleServerIsNotStuck() {
        StuckWatch watch = new StuckWatch(LIMIT);
        for (long now = 0; now <= 300_000; now += 20_000)
            assertEquals(-1, watch.poll(58, 58, now), "nothing is running at " + now + " ms");
    }

    @Test
    void aQuietNightStillShowsTheHungTransaction() {
        StuckWatch watch = new StuckWatch(LIMIT);
        assertEquals(-1, watch.poll(213, 212, 0));
        assertEquals(-1, watch.poll(213, 212, 20_000));
        assertEquals(212, watch.poll(213, 212, 40_000), "no new traffic, and 212 has been running for 40 s");
    }

    @Test
    void aBusyServerWhoseOldestKeepsFinishingIsNotStuck() {
        StuckWatch watch = new StuckWatch(LIMIT);
        long[][] polls = { { 410, 403 }, { 418, 409 }, { 427, 415 }, { 433, 422 }, { 441, 430 } };
        long now = 0;
        for (long[] p : polls) {
            assertEquals(-1, watch.poll(p[0], p[1], now), "the oldest transaction changes at every poll; at " + now + " ms");
            now += 20_000;
        }
    }

    // ---- test support: Q2 in this JVM, a TransactionManager with two sessions and an issuer call that can hang ----

    private static Q2 q2;

    @BeforeAll
    static void startQ2() throws Exception {
        Path deploy = Files.createTempDirectory("deploy");
        Files.writeString(deploy.resolve("30_txnmgr.xml"),
            "<txnmgr name='practice-watch' class='org.jpos.transaction.TransactionManager'>\n"
          + "  <property name='queue' value='txn.watch'/>\n"
          + "  <property name='sessions' value='2'/>\n"
          + "  <property name='debug' value='false'/>\n"
          + "  <participant class='com.example.practice.StuckWatchTest$Hang'/>\n"
          + "  <participant class='com.example.practice.StuckWatchTest$Approve'/>\n"
          + "  <participant class='org.jpos.transaction.participant.SendResponse'/>\n"
          + "</txnmgr>\n");
        q2 = new Q2(deploy.toString());
        q2.start();
        assertNotNull(NameRegistrar.get("practice-watch", 30000), "the TransactionManager did not start");
    }

    @AfterAll
    static void stopQ2() {
        Hang.RELEASE.countDown();
        q2.shutdown(true);
    }

    private static ISOMsg purchase(String terminal, String stan) throws Exception {
        ISOMsg m = new ISOMsg("0200");
        m.set(3, "000000");
        m.set(4, "000000001275");
        m.set(11, stan);
        m.set(41, terminal);
        return m;
    }

    /** Queues one request for the TransactionManager; the answer arrives on the returned queue. */
    private static BlockingQueue<ISOMsg> send(ISOMsg request) {
        BlockingQueue<ISOMsg> answers = new LinkedBlockingQueue<>();
        Context ctx = new Context();
        ctx.put(ContextConstants.REQUEST.toString(), request);
        ctx.put(ContextConstants.SOURCE.toString(), new ISOSource() {
            @Override public void send(ISOMsg m) { answers.add(m); }
            @Override public boolean isConnected() { return true; }
        });
        SpaceFactory.getSpace().out("txn.watch", ctx);
        return answers;
    }

    /** Stands in for a call to the issuer with no timeout: for terminal 29110042 it waits until released. */
    public static class Hang implements TransactionParticipant {
        static final CountDownLatch ENTERED = new CountDownLatch(1);
        static final CountDownLatch RELEASE = new CountDownLatch(1);
        static final AtomicLong ID = new AtomicLong(-1);

        @Override
        public int prepare(long id, Serializable context) {
            ISOMsg request = ((Context) context).get(ContextConstants.REQUEST.toString());
            if ("29110042".equals(request.getString(41))) {
                ID.set(id);
                ENTERED.countDown();
                try {
                    RELEASE.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            return PREPARED | NO_JOIN | READONLY;
        }
    }

    /** Approves everything. */
    public static class Approve implements TransactionParticipant {
        @Override
        public int prepare(long id, Serializable context) {
            Context ctx = (Context) context;
            try {
                ISOMsg response = (ISOMsg) ((ISOMsg) ctx.get(ContextConstants.REQUEST.toString())).clone();
                response.setResponseMTI();
                response.set(39, "00");
                ctx.put(ContextConstants.RESPONSE.toString(), response);
                return PREPARED | NO_JOIN;
            } catch (Exception e) {
                return ABORTED | NO_JOIN;
            }
        }
    }
}

package com.example.practice;

import org.jpos.iso.ISOMsg;
import org.jpos.q2.Q2;
import org.jpos.space.SpaceFactory;
import org.jpos.transaction.AbortParticipant;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs RedeemPoints in a real TransactionManager, deployed by a real Q2 in this JVM, with its
 * journal in a Berkeley DB space on disk. A crash is played by a later participant that pauses the
 * transaction and never resumes it; the test then stops the TransactionManager and starts it
 * again, and the TransactionManager recovers from the journal.
 */
class RedeemPointsTest {
    private static final String TM = "practice-points";
    private static final String QUEUE = "txn.points";
    private static final TestPoints POINTS = new TestPoints();

    @Test
    void aPurchaseWithPointsConfirmsTheRedemption() throws Exception {
        Run run = Run.start(purchase("LOY-2201", "000250"));
        assertTrue(run.finish(), "the purchase commits");
        String r = POINTS.redemptionFor("LOY-2201");
        assertEquals(List.of("confirm"), POINTS.callsFor(r), "the redemption is confirmed once");
    }

    @Test
    void aDeclinedPurchaseCancelsTheRedemption() throws Exception {
        Run run = Run.start(purchase("LOY-2202", "000075"), "DECLINE");
        assertFalse(run.finish(), "the purchase is declined after the redemption");
        String r = POINTS.redemptionFor("LOY-2202");
        assertEquals(List.of("cancel"), POINTS.callsFor(r), "the redemption is cancelled once");
    }

    @Test
    void aCrashRightAfterTheRedemptionGivesThePointsBack() throws Exception {
        Run run = Run.start(purchase("LOY-2203", "000420"), "FREEZE");
        run.awaitFrozen();
        restart();
        String r = POINTS.redemptionFor("LOY-2203");
        assertEquals(List.of("cancel"), POINTS.callsFor(r),
            "after the restart, the redemption of the purchase that was in flight is cancelled");
    }

    @Test
    void aCrashLaterInTheChainGivesThePointsBack() throws Exception {
        Run run = Run.start(purchase("LOY-2204", "000310"), "TOUCH", "FREEZE");
        run.awaitFrozen();
        restart();
        String r = POINTS.redemptionFor("LOY-2204");
        assertEquals(List.of("cancel"), POINTS.callsFor(r),
            "after the restart, the redemption of the purchase that was in flight is cancelled");
    }

    @Test
    void aRecoveredPurchaseWithoutPointsCallsNoService() throws Exception {
        Run withPoints = Run.start(purchase("LOY-2205", "000060"), "TOUCH", "FREEZE");
        withPoints.awaitFrozen();
        ISOMsg cash = purchase("LOY-2206", null);
        Run withoutPoints = Run.start(cash, "TOUCH", "FREEZE");
        withoutPoints.awaitFrozen();
        int nullsBefore = POINTS.nullCalls.get();
        restart();
        assertEquals(List.of("cancel"), POINTS.callsFor(POINTS.redemptionFor("LOY-2205")),
            "the purchase that used points gets them back");
        assertEquals(nullsBefore, POINTS.nullCalls.get(),
            "the purchase that used no points must not reach the points service");
    }

    // ---- requests ----

    private static ISOMsg purchase(String loyaltyAccount, String points) throws Exception {
        ISOMsg m = new ISOMsg("0200");
        m.set(2, "5555555555554444");
        m.set(3, "000000");
        m.set(4, "000000003150");
        m.set(11, String.format("%06d", 700 + STANS.incrementAndGet()));
        m.set(41, "29110091");
        if (points != null)
            m.set(48, points);
        m.set(102, loyaltyAccount);
        return m;
    }

    // ---- test support: Q2 in this JVM, a TransactionManager with its journal on disk ----

    private static final AtomicInteger STANS = new AtomicInteger();
    private static final AtomicInteger RUNS = new AtomicInteger();
    private static final Map<String, CountDownLatch> FROZEN = new ConcurrentHashMap<>();
    private static Q2 q2;

    @BeforeAll
    static void startQ2() throws Exception {
        NameRegistrar.register("practice-points-service", POINTS);
        Path dir = Files.createTempDirectory("points");
        Path deploy = Files.createDirectory(dir.resolve("deploy"));
        Files.writeString(deploy.resolve("30_txnmgr.xml"),
            "<txnmgr name='" + TM + "' class='org.jpos.transaction.TransactionManager'>\n"
          + "  <property name='queue' value='" + QUEUE + "'/>\n"
          + "  <property name='sessions' value='1'/>\n"
          + "  <property name='debug' value='false'/>\n"
          + "  <property name='persistent-space' value='je:practice-points-journal:" + dir.resolve("journal") + "'/>\n"
          + "  <participant class='com.example.practice.RedeemPoints'>\n"
          + "    <property name='service' value='practice-points-service'/>\n"
          + "  </participant>\n"
          + "  <participant class='com.example.practice.RedeemPointsTest$Touch'/>\n"
          + "  <participant class='com.example.practice.RedeemPointsTest$Freeze'/>\n"
          + "  <participant class='com.example.practice.RedeemPointsTest$Decide'/>\n"
          + "  <participant class='com.example.practice.RedeemPointsTest$Finished'/>\n"
          + "</txnmgr>\n");
        q2 = new Q2(deploy.toString());
        q2.start();
        assertNotNull(NameRegistrar.get(TM, 30000), "the TransactionManager did not start");
    }

    @AfterAll
    static void stopQ2() {
        q2.shutdown(true);
        NameRegistrar.unregister("practice-points-service");
    }

    /** The process dies and starts again: the TransactionManager stops, then starts and recovers. */
    private static void restart() {
        TransactionManager tm = NameRegistrar.getIfExists(TM);
        assertNotNull(tm);
        tm.stop();
        tm.start();
    }

    /** One transaction, queued as IncomingListener queues it, with test flags in the Context. */
    static final class Run {
        final String id = "run." + RUNS.incrementAndGet();

        static Run start(ISOMsg request, String... flags) {
            Run run = new Run();
            FROZEN.put(run.id, new CountDownLatch(1));
            Context ctx = new Context();
            ctx.put(ContextConstants.REQUEST.toString(), request);
            ctx.put("RUN", run.id);
            for (String f : flags)
                ctx.put(f, Boolean.TRUE);
            SpaceFactory.getSpace().out(QUEUE, ctx);
            return run;
        }

        /** True if committed, false if aborted. */
        boolean finish() {
            Boolean c = (Boolean) SpaceFactory.getSpace().in(id, 20000);
            assertNotNull(c, "the transaction did not finish");
            return c;
        }

        void awaitFrozen() throws InterruptedException {
            assertTrue(FROZEN.get(id).await(20, TimeUnit.SECONDS), "the transaction never reached the crash point");
        }
    }

    /** The loyalty service: records every call, per redemption id. */
    static final class TestPoints implements RedeemPoints.PointsService {
        private final Map<String, String> byAccount = new ConcurrentHashMap<>();
        private final Map<String, List<String>> calls = new ConcurrentHashMap<>();
        private final AtomicInteger ids = new AtomicInteger();
        final AtomicInteger nullCalls = new AtomicInteger();

        @Override
        public String redeem(String account, long points) {
            String id = "RD-" + (8100 + ids.incrementAndGet());
            byAccount.put(account, id);
            calls.put(id, new ArrayList<>());
            return id;
        }

        @Override
        public void confirm(String redemptionId) {
            record(redemptionId, "confirm");
        }

        @Override
        public void cancel(String redemptionId) {
            record(redemptionId, "cancel");
        }

        private synchronized void record(String redemptionId, String call) {
            List<String> list = redemptionId == null ? null : calls.get(redemptionId);
            if (list == null)
                nullCalls.incrementAndGet();
            else
                list.add(call);
        }

        String redemptionFor(String account) {
            String id = byAccount.get(account);
            assertNotNull(id, "no points were redeemed for " + account);
            return id;
        }

        synchronized List<String> callsFor(String redemptionId) {
            return new ArrayList<>(calls.get(redemptionId));
        }
    }

    /** Changes the Context (so the TransactionManager takes a snapshot) when the test asks for it. */
    public static class Touch implements TransactionParticipant {
        @Override
        public int prepare(long id, Serializable context) {
            Context ctx = (Context) context;
            if (ctx.get("TOUCH") == null)
                return PREPARED | NO_JOIN | READONLY;
            ctx.put("TOUCHED", Boolean.TRUE);
            return PREPARED | NO_JOIN;
        }
    }

    /** The crash point: pauses the transaction and never resumes it, when the test asks for it. */
    public static class Freeze implements TransactionParticipant {
        @Override
        public int prepare(long id, Serializable context) {
            Context ctx = (Context) context;
            if (ctx.get("FREEZE") == null)
                return PREPARED | NO_JOIN | READONLY;
            FROZEN.get(ctx.getString("RUN")).countDown();
            return PREPARED | PAUSE | NO_JOIN | READONLY;
        }
    }

    /** Declines when the test asks for it, after the points were redeemed. */
    public static class Decide implements TransactionParticipant {
        @Override
        public int prepare(long id, Serializable context) {
            return ((Context) context).get("DECLINE") != null ? ABORTED | NO_JOIN : PREPARED | NO_JOIN | READONLY;
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
            String run = ((Context) context).getString("RUN");
            if (run != null)
                SpaceFactory.getSpace().out(run, Boolean.TRUE);
        }

        @Override
        public void abort(long id, Serializable context) {
            String run = ((Context) context).getString("RUN");
            if (run != null)
                SpaceFactory.getSpace().out(run, Boolean.FALSE);
        }
    }
}

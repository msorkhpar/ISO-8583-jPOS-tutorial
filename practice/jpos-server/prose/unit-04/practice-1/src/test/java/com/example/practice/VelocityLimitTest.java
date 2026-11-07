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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Runs VelocityLimit in a real TransactionManager, deployed by a real Q2, with the real SendResponse. */
class VelocityLimitTest {
    private static final String REQUEST = ContextConstants.REQUEST.toString();
    private static final String SOURCE = ContextConstants.SOURCE.toString();
    private static final VelocityLimit.DailyCounts COUNTS = new VelocityLimit.DailyCounts(3);

    @Test
    void aPurchaseIsApprovedAndCounted() throws Exception {
        Run run = Run.of(purchase("4012888888881881", "29110071", "000052"), false);
        assertTrue(run.committed, "the transaction commits");
        assertEquals("00", run.answer().getString(39));
        assertEquals(1, COUNTS.count("4012888888881881"));
    }

    @Test
    void aPurchaseOverTheDailyCountIsRefused() throws Exception {
        String card = "5105105105105100";
        for (int i = 0; i < 3; i++)
            assertEquals("00", Run.of(purchase(card, "29110072", "00081" + i), false).answer().getString(39));
        Run fourth = Run.of(purchase(card, "29110072", "000814"), false);
        assertFalse(fourth.committed, "the fourth purchase of the day is refused");
        assertEquals("65", fourth.answer().getString(39));
        assertEquals("000814", fourth.answer().getString(11));
    }

    @Test
    void purchasesTheAuthorizerDeclinedDoNotUseUpTheDay() throws Exception {
        String card = "4000056655665556";
        for (int i = 0; i < 2; i++) {
            Run declined = Run.of(purchase(card, "29110073", "00090" + i), true);
            assertEquals("51", declined.answer().getString(39), "the authorizer declines this one");
        }
        for (int i = 0; i < 3; i++) {
            Run approved = Run.of(purchase(card, "29110073", "00091" + i), false);
            assertEquals("00", approved.answer().getString(39),
                "approved purchase " + (i + 1) + " of 3, after two declines");
        }
        assertEquals(3, COUNTS.count(card));
    }

    @Test
    void aRefusedPurchaseDoesNotFreeAPlaceForTheNextOne() throws Exception {
        String card = "5555555555554444";
        for (int i = 0; i < 3; i++)
            Run.of(purchase(card, "29110074", "00030" + i), false);
        assertEquals("65", Run.of(purchase(card, "29110074", "000303"), false).answer().getString(39));
        assertEquals("65", Run.of(purchase(card, "29110074", "000304"), false).answer().getString(39),
            "the card still has had three purchases today");
        assertEquals(3, COUNTS.count(card));
    }

    // ---- requests ----

    private static ISOMsg purchase(String card, String terminal, String stan) throws Exception {
        ISOMsg m = new ISOMsg("0200");
        m.set(2, card);
        m.set(3, "000000");
        m.set(4, "000000003150");
        m.set(11, stan);
        m.set(41, terminal);
        return m;
    }

    // ---- test support: Q2 in this JVM, deploying a TransactionManager around VelocityLimit ----

    private static Q2 q2;

    @BeforeAll
    static void startQ2() throws Exception {
        NameRegistrar.register("practice-counts", COUNTS);
        Path deploy = Files.createTempDirectory("deploy");
        Files.writeString(deploy.resolve("30_txnmgr.xml"),
            "<txnmgr name='practice-velocity' class='org.jpos.transaction.TransactionManager'>\n"
          + "  <property name='queue' value='txn.velocity'/>\n"
          + "  <property name='sessions' value='2'/>\n"
          + "  <property name='debug' value='false'/>\n"
          + "  <participant class='com.example.practice.VelocityLimit'>\n"
          + "    <property name='counts' value='practice-counts'/>\n"
          + "  </participant>\n"
          + "  <participant class='com.example.practice.VelocityLimitTest$Authorizer'/>\n"
          + "  <participant class='org.jpos.transaction.participant.SendResponse'/>\n"
          + "  <participant class='com.example.practice.VelocityLimitTest$Finished'/>\n"
          + "</txnmgr>\n");
        q2 = new Q2(deploy.toString());
        q2.start();
        assertNotNull(NameRegistrar.get("practice-velocity", 30000), "the TransactionManager did not start");
    }

    @AfterAll
    static void stopQ2() {
        q2.shutdown(true);
        NameRegistrar.unregister("practice-counts");
    }

    /** One transaction, run to its end. */
    static final class Run {
        final List<ISOMsg> sent = new CopyOnWriteArrayList<>();
        final String id = "run." + RUNS.incrementAndGet();
        boolean committed;

        ISOMsg answer() {
            assertEquals(1, sent.size(), "the terminal gets one answer");
            return sent.get(0);
        }

        static Run of(ISOMsg request, boolean decline) {
            Run run = new Run();
            Context ctx = new Context();
            ctx.put(REQUEST, request);
            ctx.put(SOURCE, new ISOSource() {
                @Override public void send(ISOMsg m) { run.sent.add(m); }
                @Override public boolean isConnected() { return true; }
            });
            ctx.put("RUN", run.id);
            if (decline) ctx.put("DECLINE", Boolean.TRUE);
            SpaceFactory.getSpace().out("txn.velocity", ctx);
            Boolean c = (Boolean) SpaceFactory.getSpace().in(run.id, 20000);
            assertNotNull(c, "the transaction did not finish");
            run.committed = c;
            return run;
        }
    }

    private static final AtomicInteger RUNS = new AtomicInteger();

    /** Stands in for the authorizer: approves, or declines with 51 when the test says so. */
    public static class Authorizer implements TransactionParticipant {
        @Override
        public int prepare(long id, Serializable context) {
            Context ctx = (Context) context;
            try {
                ISOMsg response = (ISOMsg) ((ISOMsg) ctx.get(REQUEST)).clone();
                response.setResponseMTI();
                boolean decline = ctx.get("DECLINE") != null;
                response.set(39, decline ? "51" : "00");
                ctx.put(ContextConstants.RESPONSE.toString(), response);
                return decline ? ABORTED | NO_JOIN : PREPARED | NO_JOIN;
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

package com.example.practice;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOSource;
import org.jpos.q2.Q2;
import org.jpos.space.Space;
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
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Runs ApprovalJournal in real TransactionManagers, deployed by a real Q2, with the real SendResponse. */
class ApprovalJournalTest {
    private static final String REQUEST = ContextConstants.REQUEST.toString();
    private static final String SOURCE = ContextConstants.SOURCE.toString();

    @Test
    void anApprovedPurchaseGetsOneLine() throws Exception {
        Run run = Run.of("txn.one", purchase("29110061", "000871", "000000004410"), null);
        assertTrue(run.committed, "the transaction commits");
        assertEquals("00", run.answer().getString(39));
        assertEquals(List.of("29110061 000871 000000004410"), linesFor("29110061"));
    }

    @Test
    void twoPurchasesCommittingTogetherEachKeepTheirOwnLine() throws Exception {
        Run[] both = Run.together("txn.two",
            purchase("29110062", "000044", "000000000750"),
            purchase("29110063", "000913", "000000012000"));
        assertTrue(both[0].committed && both[1].committed);
        assertEquals(List.of("29110062 000044 000000000750"), linesFor("29110062"));
        assertEquals(List.of("29110063 000913 000000012000"), linesFor("29110063"));
    }

    @Test
    void aPurchaseAbortedLaterLeavesNoLine() throws Exception {
        Run first = Run.of("txn.one", purchase("29110064", "000301", "000000002000"), null);
        Run second = Run.of("txn.one", purchase("29110064", "000302", "000000003000"), "FAIL_LATER");
        assertTrue(first.committed);
        assertTrue(!second.committed, "a participant after the journal aborted the second purchase");
        assertEquals(List.of("29110064 000301 000000002000"), linesFor("29110064"),
            "only the purchase whose transaction committed is in the journal");
    }

    @Test
    void aDeclinedPurchaseLeavesNoLine() throws Exception {
        Run declined = Run.of("txn.one", purchase("29110065", "000512", "000000099900"), "DECLINE");
        Run approved = Run.of("txn.one", purchase("29110065", "000513", "000000001100"), null);
        assertTrue(declined.committed, "a decline is a normal, committed transaction");
        assertEquals("05", declined.answer().getString(39));
        assertEquals(List.of("29110065 000513 000000001100"), linesFor("29110065"));
        assertTrue(approved.committed);
    }

    @Test
    void anApprovedEchoLeavesNoLine() throws Exception {
        ISOMsg echo = new ISOMsg("0800");
        echo.set(11, "000620");
        echo.set(41, "29110066");
        echo.set(70, "301");
        Run run = Run.of("txn.one", echo, null);
        Run purchase = Run.of("txn.one", purchase("29110066", "000621", "000000000999"), null);
        assertTrue(run.committed);
        assertEquals("00", run.answer().getString(39));
        assertEquals(List.of("29110066 000621 000000000999"), linesFor("29110066"), "an echo moves no money");
        assertTrue(purchase.committed);
    }

    // ---- requests and the journal ----

    private static ISOMsg purchase(String terminal, String stan, String amount) throws Exception {
        ISOMsg m = new ISOMsg("0200");
        m.set(3, "000000");
        m.set(4, amount);
        m.set(11, stan);
        m.set(41, terminal);
        return m;
    }

    private static final List<String> LINES = new CopyOnWriteArrayList<>();

    private static List<String> linesFor(String terminal) {
        return LINES.stream().filter(l -> l.startsWith(terminal + " ")).collect(Collectors.toList());
    }

    // ---- test support: Q2 in this JVM, deploying two TransactionManagers around ApprovalJournal ----

    private static Q2 q2;

    @BeforeAll
    static void startQ2() throws Exception {
        NameRegistrar.register("practice-journal", (ApprovalJournal.Lines) (t, s, a) -> LINES.add(t + " " + s + " " + a));
        Path deploy = Files.createTempDirectory("deploy");
        String journal = "  <participant class='com.example.practice.ApprovalJournal'>\n"
                       + "    <property name='journal' value='practice-journal'/>\n"
                       + "  </participant>\n";
        String tail = "  <participant class='org.jpos.transaction.participant.SendResponse'/>\n"
                    + "  <participant class='com.example.practice.ApprovalJournalTest$Finished'/>\n"
                    + "</txnmgr>\n";
        Files.writeString(deploy.resolve("30_txnmgr_one.xml"),
            "<txnmgr name='practice-one' class='org.jpos.transaction.TransactionManager'>\n"
          + "  <property name='queue' value='txn.one'/>\n"
          + "  <property name='debug' value='false'/>\n"
          + "  <participant class='com.example.practice.ApprovalJournalTest$Authorizer'/>\n"
          + journal
          + "  <participant class='com.example.practice.ApprovalJournalTest$LaterCheck'/>\n"
          + tail);
        Files.writeString(deploy.resolve("31_txnmgr_two.xml"),
            "<txnmgr name='practice-two' class='org.jpos.transaction.TransactionManager'>\n"
          + "  <property name='queue' value='txn.two'/>\n"
          + "  <property name='sessions' value='2'/>\n"
          + "  <property name='debug' value='false'/>\n"
          + "  <participant class='com.example.practice.ApprovalJournalTest$Authorizer'/>\n"
          + journal
          + "  <participant class='com.example.practice.ApprovalJournalTest$Meet'/>\n"
          + tail);
        q2 = new Q2(deploy.toString());
        q2.start();
        assertNotNull(NameRegistrar.get("practice-one", 30000), "practice-one did not start");
        assertNotNull(NameRegistrar.get("practice-two", 30000), "practice-two did not start");
    }

    @AfterAll
    static void stopQ2() {
        q2.shutdown(true);
        NameRegistrar.unregister("practice-journal");
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

        Context context(ISOMsg request, String flag) {
            Context ctx = new Context();
            ctx.put(REQUEST, request);
            ctx.put(SOURCE, new ISOSource() {
                @Override public void send(ISOMsg m) { sent.add(m); }
                @Override public boolean isConnected() { return true; }
            });
            ctx.put("RUN", id);
            if (flag != null) ctx.put(flag, Boolean.TRUE);
            return ctx;
        }

        void await() {
            Boolean c = (Boolean) SpaceFactory.getSpace().in(id, 20000);
            assertNotNull(c, "the transaction did not finish");
            committed = c;
        }

        static Run of(String queue, ISOMsg request, String flag) {
            Run run = new Run();
            SpaceFactory.getSpace().out(queue, run.context(request, flag));
            run.await();
            return run;
        }

        static Run[] together(String queue, ISOMsg a, ISOMsg b) {
            Run ra = new Run(), rb = new Run();
            Space<String, Object> sp = SpaceFactory.getSpace();
            sp.out(queue, ra.context(a, null));
            sp.out(queue, rb.context(b, null));
            ra.await();
            rb.await();
            return new Run[] { ra, rb };
        }
    }

    private static final AtomicInteger RUNS = new AtomicInteger();

    /** Stands in for the real authorizer: approves, or declines with 05 when the test says so. */
    public static class Authorizer implements TransactionParticipant {
        @Override
        public int prepare(long id, Serializable context) {
            Context ctx = (Context) context;
            try {
                ISOMsg response = (ISOMsg) ((ISOMsg) ctx.get(REQUEST)).clone(0, 3, 4, 7, 11, 41, 70);
                response.setResponseMTI();
                response.set(39, ctx.get("DECLINE") != null ? "05" : "00");
                ctx.put(ContextConstants.RESPONSE.toString(), response);
                return PREPARED | NO_JOIN;
            } catch (Exception e) {
                return ABORTED;
            }
        }
    }

    /** A participant after the journal that fails when the test says so, as a database write might. */
    public static class LaterCheck implements TransactionParticipant {
        @Override
        public int prepare(long id, Serializable context) {
            return ((Context) context).get("FAIL_LATER") != null ? ABORTED | NO_JOIN : PREPARED | NO_JOIN | READONLY;
        }
    }

    /** Holds each transaction here until two have arrived: both are past the journal's prepare. */
    public static class Meet implements TransactionParticipant {
        private static final CyclicBarrier BOTH = new CyclicBarrier(2);

        @Override
        public int prepare(long id, Serializable context) {
            try {
                BOTH.await(20, TimeUnit.SECONDS);
                return PREPARED | NO_JOIN | READONLY;
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

package com.example.practice;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOSource;
import org.jpos.q2.Q2;
import org.jpos.rc.CMF;
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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** Runs AnswerFailures in a real TransactionManager deployed by Q2 in this JVM. */
class AnswerFailuresTest {

    @Test
    void aReportedCauseGetsTheNetworksCode() throws Exception {
        Outcome low = withdraw("000000002000", "000311", "report:NOT_SUFFICIENT_FUNDS");
        assertFalse(low.committed);
        assertEquals("0210", low.answer.getMTI());
        assertEquals("000311", low.answer.getString(11), "the answer goes to the request it came from");
        assertEquals("51", low.answer.getString(39));
        assertEquals("91", withdraw("000000002000", "000312", "report:HOST_UNREACHABLE").answer.getString(39));
        assertEquals("55", withdraw("000000002000", "000313", "report:INCORRECT_PIN").answer.getString(39));
    }

    @Test
    void aFailureNobodyPlannedForIsTheServersOwn() throws Exception {
        assertEquals("96", withdraw("000000002000", "000321", "throw").answer.getString(39),
            "a participant that threw reported nothing; the server failed, the card did not");
        assertEquals("96", withdraw("000000002000", "000322", "report:EXPIRED").answer.getString(39),
            "a cause the network's table doesn't list is not a reason to refuse the card");
    }

    @Test
    void anApprovalThatAbortsDoesNotLeave() throws Exception {
        Outcome o = withdraw("000000002000", "000331", "journal-full");
        assertFalse(o.committed, "the journal failed after the approval was built");
        assertEquals("96", o.answer.getString(39), "the ATM must not dispense for a withdrawal the server rolled back");
    }

    @Test
    void aPartialApprovalThatAbortsDoesNotLeave() throws Exception {
        Outcome o = withdraw("000000030000", "000341", "journal-full");
        assertFalse(o.committed);
        assertEquals("96", o.answer.getString(39), "a partial approval dispenses cash too");
    }

    @Test
    void aRefusalAlreadyBuiltIsKept() throws Exception {
        assertEquals("65", withdraw("000000002000", "000350", "report:EXCEEDS_WITHDRAWAL_FREQUENCY_LIMIT")
            .answer.getString(39), "a failure with no answer built gets one");
        Outcome o = withdraw("000000060000", "000351", "");
        assertFalse(o.committed);
        assertEquals("61", o.answer.getString(39), "the limit check's own answer is the right one");
    }

    // ---- requests ----

    static final class Outcome {
        final ISOMsg answer;
        final boolean committed;
        Outcome(ISOMsg answer, boolean committed) { this.answer = answer; this.committed = committed; }
    }

    /** A cash withdrawal at ATM00042. {@code script} tells the test participants what goes wrong. */
    private static Outcome withdraw(String amount, String stan, String script) throws Exception {
        ISOMsg m = new ISOMsg("0200");
        m.set(2, "5555555555554444");
        m.set(3, "010000");
        m.set(4, amount);
        m.set(7, "1106183005");
        m.set(11, stan);
        m.set(41, "ATM00042");
        m.set(49, "978");
        BlockingQueue<ISOMsg> sent = new LinkedBlockingQueue<>();
        BlockingQueue<Boolean> ended = new LinkedBlockingQueue<>();
        Context ctx = new Context();
        ctx.put("ENDED", ended);
        ctx.put("SCRIPT", script);
        ctx.put(ContextConstants.REQUEST.toString(), m);
        ctx.put(ContextConstants.SOURCE.toString(), new ISOSource() {
            @Override public void send(ISOMsg r) { sent.add(r); }
            @Override public boolean isConnected() { return true; }
        });
        SpaceFactory.getSpace().out("practice.errors", ctx);
        Boolean committed = ended.poll(20, TimeUnit.SECONDS);
        assertNotNull(committed, "the transaction did not end");
        ISOMsg r = sent.poll(5, TimeUnit.SECONDS);
        assertNotNull(r, "the ATM got no answer");
        return new Outcome(r, committed);
    }

    // ---- test support: Q2 in this JVM, and participants that fail on cue ----

    private static Q2 q2;

    @BeforeAll
    static void startQ2() throws Exception {
        Path deploy = Files.createTempDirectory("deploy");
        Files.writeString(deploy.resolve("30_txnmgr.xml"),
            "<txnmgr name='practice-errors' class='org.jpos.transaction.TransactionManager'>\n"
          + "  <property name='queue' value='practice.errors'/>\n"
          + "  <property name='sessions' value='1'/>\n"
          + "  <property name='debug' value='false'/>\n"
          + "  <participant class='com.example.practice.AnswerFailuresTest$Script'/>\n"
          + "  <participant class='com.example.practice.AnswerFailuresTest$Authorize'/>\n"
          + "  <participant class='com.example.practice.AnswerFailuresTest$Journal'/>\n"
          + "  <participant class='com.example.practice.AnswerFailures'/>\n"
          + "  <participant class='org.jpos.transaction.participant.SendResponse'/>\n"
          + "  <participant class='com.example.practice.AnswerFailuresTest$Finished'/>\n"
          + "</txnmgr>\n");
        q2 = new Q2(deploy.toString());
        q2.start();
        assertNotNull(NameRegistrar.get("practice-errors", 15000), "the TransactionManager did not start");
    }

    @AfterAll
    static void stopQ2() {
        if (q2 != null) q2.shutdown(true);
    }

    private static String script(Serializable context) {
        String s = ((Context) context).getString("SCRIPT");
        return s != null ? s : "";
    }

    /** Before the decision: reports a cause, or throws, as the test asks. */
    public static class Script implements TransactionParticipant {
        @Override
        public int prepare(long id, Serializable context) {
            String s = script(context);
            if (s.equals("throw"))
                throw new IllegalStateException("account table unavailable");
            if (s.startsWith("report:"))
                return ((Context) context).getResult().fail(CMF.valueOf(s.substring(7)), "Script", "as the test asked").FAIL();
            return PREPARED | NO_JOIN | READONLY;
        }
    }

    /** Approves up to 200.00, approves 200.00 of anything up to 500.00 (10), refuses more with 61. */
    public static class Authorize implements TransactionParticipant {
        @Override
        public int prepare(long id, Serializable context) {
            Context ctx = (Context) context;
            try {
                ISOMsg request = ctx.get(ContextConstants.REQUEST.toString());
                long amount = Long.parseLong(request.getString(4));
                ISOMsg response = (ISOMsg) request.clone(0, 2, 3, 4, 7, 11, 41, 49);
                response.setResponseMTI();
                if (amount > 50000) {
                    response.set(39, "61");
                    ctx.put(ContextConstants.RESPONSE.toString(), response);
                    return ABORTED | NO_JOIN;
                }
                if (amount > 20000) {
                    response.set(4, "000000020000");
                    response.set(39, "10");
                } else {
                    response.set(39, "00");
                }
                ctx.put(ContextConstants.RESPONSE.toString(), response);
                return PREPARED | NO_JOIN;
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }

    /** Writes the approval to the journal, whose disk may be full. */
    public static class Journal implements TransactionParticipant {
        @Override
        public int prepare(long id, Serializable context) {
            if (script(context).equals("journal-full"))
                throw new IllegalStateException("No space left on device");
            return PREPARED | NO_JOIN | READONLY;
        }
    }

    /** Last on both paths: tells the test how the transaction ended. */
    public static class Finished implements AbortParticipant {
        @Override public int prepare(long id, Serializable context) { return PREPARED | READONLY; }
        @Override public int prepareForAbort(long id, Serializable context) { return PREPARED | READONLY; }
        @Override public void commit(long id, Serializable context) { ended(context).add(Boolean.TRUE); }
        @Override public void abort(long id, Serializable context) { ended(context).add(Boolean.FALSE); }

        @SuppressWarnings("unchecked")
        private static BlockingQueue<Boolean> ended(Serializable context) {
            return (BlockingQueue<Boolean>) ((Context) context).get("ENDED");
        }
    }
}

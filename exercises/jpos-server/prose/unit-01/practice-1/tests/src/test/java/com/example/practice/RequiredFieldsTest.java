package com.example.practice;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOSource;
import org.jpos.q2.Q2;
import org.jpos.space.Space;
import org.jpos.space.SpaceFactory;
import org.jpos.transaction.AbortParticipant;
import org.jpos.transaction.Context;
import org.jpos.transaction.ContextConstants;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Runs RequiredFields in a real TransactionManager, deployed by a real Q2, with the real SendResponse. */
class RequiredFieldsTest {
    private static final String REQUEST = ContextConstants.REQUEST.toString();
    private static final String SOURCE = ContextConstants.SOURCE.toString();

    @Test
    void aCompletePurchaseReachesTheAuthorizer() throws Exception {
        Run run = Run.of(message("0200", "000418", "29110047", "000000001995"));
        assertEquals(1, run.sent.size(), "the terminal gets one answer");
        assertEquals("0210", run.sent.get(0).getMTI());
        assertEquals("00", run.sent.get(0).getString(39), "the authorizer approved it");
    }

    @Test
    void aCompleteAuthorizationReachesTheAuthorizer() throws Exception {
        Run run = Run.of(message("0100", "000419", "29110047", "000000012000"));
        assertEquals(1, run.sent.size());
        assertEquals("0110", run.sent.get(0).getMTI());
        assertEquals("00", run.sent.get(0).getString(39));
    }

    @Test
    void aPurchaseWithoutAnAmountIsAnsweredAtOnce() throws Exception {
        Run run = Run.of(message("0200", "000420", "29110047", null));
        assertEquals(1, run.sent.size(), "the terminal is answered, not left to time out");
        assertEquals("0210", run.sent.get(0).getMTI());
        assertEquals("30", run.sent.get(0).getString(39));
        assertEquals(0, run.authorized, "the authorizer never saw it");
    }

    @Test
    void theRefusalIsRecognisedByTheTerminal() throws Exception {
        Run run = Run.of(message("0100", "000421", "29110052", null));
        assertEquals(1, run.sent.size());
        ISOMsg answer = run.sent.get(0);
        assertEquals("0110", answer.getMTI());
        assertEquals("000421", answer.getString(11), "the terminal matches its answer by STAN");
        assertEquals("29110052", answer.getString(41), "and by terminal ID");
    }

    @Test
    void anEchoIsNotAPurchase() throws Exception {
        ISOMsg echo = message("0800", "000422", "29110047", null);
        echo.set(70, "301");
        Run run = Run.of(echo);
        assertEquals(1, run.authorized, "an echo carries no amount and still reaches the authorizer");
        assertEquals("00", run.sent.get(0).getString(39));
    }

    @Test
    void theRequestStaysAsItArrived() throws Exception {
        Run run = Run.of(message("0200", "000423", "29110047", null));
        assertEquals(1, run.sent.size(), "the terminal is answered");
        assertEquals("30", run.sent.get(0).getString(39));
        ISOMsg request = run.ctx.get(REQUEST);
        assertEquals("0200", request.getMTI(), "later participants and the log still see the request");
        assertTrue(!request.hasField(39), "the request carries no response code");
    }

    // ---- the request as a terminal sends it ----

    private static ISOMsg message(String mti, String stan, String terminal, String amount) throws Exception {
        ISOMsg m = new ISOMsg(mti);
        if (!mti.startsWith("08")) m.set(3, "000000");
        if (amount != null) m.set(4, amount);
        m.set(7, "1106101500");
        m.set(11, stan);
        m.set(41, terminal);
        return m;
    }

    // ---- test support: Q2 in this JVM, deploying a TransactionManager around RequiredFields ----

    private static Q2 q2;
    private static Path deploy;

    @BeforeAll
    static void startQ2() throws Exception {
        deploy = Files.createTempDirectory("deploy");
        Files.writeString(deploy.resolve("30_txnmgr.xml"),
            "<txnmgr name='practice-txnmgr' class='org.jpos.transaction.TransactionManager'>\n"
          + "  <property name='queue' value='practice.txnmgr'/>\n"
          + "  <property name='debug' value='false'/>\n"
          + "  <participant class='com.example.practice.RequiredFields'/>\n"
          + "  <participant class='com.example.practice.RequiredFieldsTest$Authorizer'/>\n"
          + "  <participant class='org.jpos.transaction.participant.SendResponse'/>\n"
          + "  <participant class='com.example.practice.RequiredFieldsTest$Finished'/>\n"
          + "</txnmgr>\n");
        q2 = new Q2(deploy.toString());
        q2.start();
        assertNotNull(NameRegistrar.get("practice-txnmgr", 30000), "the TransactionManager did not start");
    }

    @AfterAll
    static void stopQ2() {
        q2.shutdown(true);
    }

    /** One transaction, run to its end. */
    static final class Run {
        final List<ISOMsg> sent = new CopyOnWriteArrayList<>();
        Context ctx;
        int authorized;

        static Run of(ISOMsg request) throws Exception {
            Run run = new Run();
            String id = "run." + RUNS.incrementAndGet();
            Context ctx = new Context();
            ctx.put(REQUEST, request);
            ctx.put(SOURCE, new ISOSource() {
                @Override public void send(ISOMsg m) { run.sent.add(m); }
                @Override public boolean isConnected() { return true; }
            });
            ctx.put("RUN", id);
            Space<String, Object> sp = SpaceFactory.getSpace();
            sp.out("practice.txnmgr", ctx);
            run.ctx = (Context) sp.in(id, 20000);
            assertNotNull(run.ctx, "the transaction did not finish");
            Integer n = run.ctx.get("AUTHORIZED");
            run.authorized = n == null ? 0 : n;
            return run;
        }
    }

    private static final AtomicInteger RUNS = new AtomicInteger();

    /** Stands in for the real authorizer: approves whatever reaches it. */
    public static class Authorizer implements org.jpos.transaction.TransactionParticipant {
        @Override
        public int prepare(long id, Serializable context) {
            Context ctx = (Context) context;
            try {
                ISOMsg response = (ISOMsg) ((ISOMsg) ctx.get(REQUEST)).clone(0, 3, 4, 7, 11, 41, 70);
                response.setResponseMTI();
                response.set(39, "00");
                ctx.put(ContextConstants.RESPONSE.toString(), response);
                ctx.put("AUTHORIZED", 1);
                return PREPARED | NO_JOIN;
            } catch (Exception e) {
                return ABORTED;
            }
        }
    }

    /** Last in the chain, on both paths: tells the test that the transaction is over. */
    public static class Finished implements AbortParticipant {
        @Override
        public int prepare(long id, Serializable context) {
            return PREPARED | READONLY;
        }

        @Override
        public void commit(long id, Serializable context) {
            done((Context) context);
        }

        @Override
        public void abort(long id, Serializable context) {
            done((Context) context);
        }

        private static void done(Context ctx) {
            SpaceFactory.getSpace().out(ctx.getString("RUN"), ctx);
        }
    }
}

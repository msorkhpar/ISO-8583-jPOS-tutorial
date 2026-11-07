package com.example.practice;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOSource;
import org.jpos.q2.Q2;
import org.jpos.rc.CMF;
import org.jpos.rc.Result;
import org.jpos.space.LocalSpace;
import org.jpos.space.Space;
import org.jpos.space.SpaceFactory;
import org.jpos.space.SpaceListener;
import org.jpos.space.SpaceUtil;
import org.jpos.transaction.AbortParticipant;
import org.jpos.transaction.Context;
import org.jpos.transaction.ContextConstants;
import org.jpos.util.NameRegistrar;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Runs AfterNetwork after a real QueryHost and QMUX, in a TransactionManager deployed by Q2 in this JVM. */
class AfterNetworkTest {
    private static final String SAF = "saf.mastercard";

    @BeforeEach
    void emptyTheSaf() {
        SpaceUtil.wipe(SpaceFactory.getSpace(), SAF);
    }

    @Test
    void anApprovalGoesBackAsItIs() throws Exception {
        host.then(Host.APPROVE);
        Outcome o = run("mc", "000611");
        assertTrue(o.committed, "an approved purchase commits");
        ISOMsg r = o.answer;
        assertEquals("0210", r.getMTI());
        assertEquals("00", r.getString(39));
        assertEquals("000611", r.getString(11));
        assertNull(SpaceFactory.getSpace().rdp(SAF), "an approval leaves nothing to put right");
    }

    @Test
    void aSilentNetworkIsNotADecline() throws Exception {
        host.then(Host.DROP);
        ISOMsg r = purchase("mc", "000612");
        assertEquals("91", r.getString(39), "the terminal learns the network was unreachable, not that the card was refused");
        assertEquals("000612", r.getString(11));
        host.then(Host.APPROVE);
        assertTrue(run("mc", "000622").committed, "the next purchase, answered, goes through as usual");
    }

    @Test
    void aSilentNetworkLeavesOneReversal() throws Exception {
        host.then(Host.DROP);
        purchase("mc", "000613");
        Space<String, Object> sp = SpaceFactory.getSpace();
        ISOMsg rev = (ISOMsg) sp.inp(SAF);
        assertNotNull(rev, "the network received the purchase and may have approved it");
        assertEquals("0420", rev.getMTI());
        assertTrue(rev.getString(90).startsWith("0200000613"), "the reversal points at the purchase");
        assertNull(sp.inp(SAF), "one reversal per purchase");
        host.then(Host.APPROVE);
        assertTrue(run("mc", "000623").committed, "the next purchase, answered, goes through as usual");
    }

    @Test
    void aNetworkDeclineIsPassedOnAndNotReversed() throws Exception {
        host.then(Host.DECLINE);
        Outcome o = run("mc", "000614");
        assertTrue(o.committed, "the network's decline is a complete, answered transaction");
        ISOMsg r = o.answer;
        assertEquals("51", r.getString(39), "the network's own answer reaches the terminal");
        assertNull(SpaceFactory.getSpace().rdp(SAF), "a declined purchase holds nothing to undo");
    }

    @Test
    void aDownLinkLeavesNothingToReverse() throws Exception {
        ISOMsg r = purchase("mc-down", "000615");
        assertEquals("91", r.getString(39));
        assertNull(SpaceFactory.getSpace().rdp(SAF), "nothing was sent, so the network holds nothing");
        host.then(Host.APPROVE);
        assertTrue(run("mc", "000625").committed, "a purchase on the working link goes through as usual");
        assertNull(SpaceFactory.getSpace().rdp(SAF));
    }

    // ---- requests ----

    /** What the terminal got, and whether the transaction committed. */
    static final class Outcome {
        final ISOMsg answer;
        final boolean committed;
        Outcome(ISOMsg answer, boolean committed) { this.answer = answer; this.committed = committed; }
    }

    private static ISOMsg purchase(String destination, String stan) throws Exception {
        return run(destination, stan).answer;
    }

    private static Outcome run(String destination, String stan) throws Exception {
        ISOMsg m = new ISOMsg("0200");
        m.set(2, "5555555555554444");
        m.set(3, "000000");
        m.set(4, "000000003390");
        m.set(7, "1106142233");
        m.set(11, stan);
        m.set(41, "29110077");
        m.set(49, "978");
        BlockingQueue<ISOMsg> sent = new LinkedBlockingQueue<>();
        BlockingQueue<Boolean> ended = new LinkedBlockingQueue<>();
        Context ctx = new Context();
        ctx.put("ENDED", ended);
        ctx.put(ContextConstants.REQUEST.toString(), m);
        ctx.put(ContextConstants.DESTINATION.toString(), destination);
        ctx.put(ContextConstants.SOURCE.toString(), new ISOSource() {
            @Override public void send(ISOMsg r) { sent.add(r); }
            @Override public boolean isConnected() { return true; }
        });
        SpaceFactory.getSpace().out("practice.network", ctx);
        Boolean committed = ended.poll(20, TimeUnit.SECONDS);
        assertNotNull(committed, "the transaction did not end");
        ISOMsg r = sent.poll(5, TimeUnit.SECONDS);
        assertNotNull(r, "the terminal got no answer");
        return new Outcome(r, committed);
    }

    // ---- test support: Q2 in this JVM, two QMUXes, and a network on the far side of one of them ----

    private static Q2 q2;
    private static Host host;

    @BeforeAll
    @SuppressWarnings("unchecked")
    static void startQ2() throws Exception {
        Path deploy = Files.createTempDirectory("deploy");
        Files.writeString(deploy.resolve("20_mux_mc.xml"),
            "<mux class='org.jpos.q2.iso.QMUX' name='mc'>\n"
          + "  <in>mc-receive</in>\n  <out>mc-send</out>\n  <unhandled>mc-unhandled</unhandled>\n"
          + "</mux>\n");
        Files.writeString(deploy.resolve("21_mux_mc_down.xml"),
            "<mux class='org.jpos.q2.iso.QMUX' name='mc-down'>\n"
          + "  <in>mc-down-receive</in>\n  <out>mc-down-send</out>\n  <ready>mc-down.ready</ready>\n"
          + "</mux>\n");
        Files.writeString(deploy.resolve("30_txnmgr.xml"),
            "<txnmgr name='practice-network' class='org.jpos.transaction.TransactionManager'>\n"
          + "  <property name='queue' value='practice.network'/>\n"
          + "  <property name='sessions' value='2'/>\n"
          + "  <property name='debug' value='false'/>\n"
          + "  <participant class='org.jpos.transaction.participant.Switch'>\n"
          + "    <property name='txnname' value='DESTINATION'/>\n"
          + "    <property name='mc' value='mastercard'/>\n"
          + "    <property name='mc-down' value='mastercard'/>\n"
          + "  </participant>\n"
          + "  <participant class='com.example.practice.AfterNetworkTest$AnswerFailures'/>\n"
          + "  <participant class='org.jpos.transaction.participant.SendResponse'/>\n"
          + "  <participant class='com.example.practice.AfterNetworkTest$Finished'/>\n"
          + "  <group name='mastercard'>\n"
          + "    <participant class='org.jpos.transaction.participant.QueryHost'>\n"
          + "      <property name='timeout' value='1000'/>\n"
          + "    </participant>\n"
          + "    <participant class='com.example.practice.AfterNetwork'>\n"
          + "      <property name='saf' value='" + SAF + "'/>\n"
          + "    </participant>\n"
          + "  </group>\n"
          + "</txnmgr>\n");
        q2 = new Q2(deploy.toString());
        q2.start();
        assertNotNull(NameRegistrar.get("practice-network", 15000), "the TransactionManager did not start");
        assertNotNull(NameRegistrar.get("mux.mc", 15000), "the QMUX did not start");
        host = new Host((LocalSpace<String, Object>) SpaceFactory.getSpace());
    }

    @AfterAll
    static void stopQ2() {
        if (q2 != null) q2.shutdown(true);
    }

    /** The network: approves (00), declines (51) or drops each request, in script order. */
    static final class Host implements SpaceListener<String, Object> {
        static final String APPROVE = "00", DECLINE = "51", DROP = "drop";
        private final LocalSpace<String, Object> sp;
        private final Deque<String> script = new ArrayDeque<>();

        Host(LocalSpace<String, Object> sp) {
            this.sp = sp;
            sp.addListener("mc-send", this);
        }

        synchronized void then(String... acts) {
            script.clear();
            script.addAll(java.util.List.of(acts));
        }

        @Override
        public void notify(String queue, Object value) {
            ISOMsg request = (ISOMsg) sp.inp(queue);
            if (request == null) return;
            String act;
            synchronized (this) {
                act = script.isEmpty() ? APPROVE : script.poll();
            }
            if (DROP.equals(act)) return;
            try {
                ISOMsg response = (ISOMsg) request.clone(0, 2, 3, 4, 7, 11, 41, 49);
                response.setResponseMTI();
                response.set(39, act);
                sp.out("mc-receive", response);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
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

    /** On the abort path, answers what the stock participants only reported: 91 for an unreachable host. */
    public static class AnswerFailures implements AbortParticipant {
        @Override
        public int prepare(long id, Serializable context) {
            return PREPARED | NO_JOIN | READONLY;
        }

        @Override
        public int prepareForAbort(long id, Serializable context) {
            Context ctx = (Context) context;
            if (ctx.get(ContextConstants.RESPONSE.toString()) != null)
                return ABORTED | NO_JOIN | READONLY;
            try {
                ISOMsg response = (ISOMsg) ((ISOMsg) ctx.get(ContextConstants.REQUEST.toString()))
                    .clone(0, 2, 3, 4, 7, 11, 41, 49);
                response.setResponseMTI();
                Result result = ctx.getResult();
                response.set(39, result.hasFailure(CMF.HOST_UNREACHABLE) ? "91" : "96");
                ctx.put(ContextConstants.RESPONSE.toString(), response);
            } catch (Exception e) {
                ctx.log(e);
            }
            return ABORTED | NO_JOIN;
        }
    }
}

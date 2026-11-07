package com.example.practice;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOSource;
import org.jpos.q2.Q2;
import org.jpos.rc.CMF;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Runs the switch's forwarding and reversal groups in a TransactionManager deployed by Q2 in this JVM. */
class ReversalRecordsTest {

    @BeforeEach
    @SuppressWarnings("unchecked")
    void emptyTheSwitch() {
        LocalSpace<String, Object> sp = (LocalSpace<String, Object>) ReversalRecords.SPACE;
        for (String key : sp.getKeySet())
            SpaceUtil.wipe(sp, key);
        for (Host h : hosts) h.reset();
    }

    @Test
    void anApprovedPurchaseAndItsReversal() throws Exception {
        ISOMsg p = purchase("29110091", "000412", "631104000412");
        assertEquals("00", send("fleet", "purchase", p).getString(39));
        ISOMsg r = send("fleet", "reversal", reversal(p, "000413", "0420"));
        assertEquals("0430", r.getMTI());
        assertEquals("00", r.getString(39));
        assertEquals("000413", r.getString(11));
        List<ISOMsg> queued = drain("reversal.fleet");
        assertEquals(1, queued.size(), "the network gets one reversal for the purchase");
        assertEquals("0420", queued.get(0).getMTI());
    }

    @Test
    void aReversalOfNothingIsAnsweredAndSendsNothing() throws Exception {
        ISOMsg p = purchase("29110092", "000077", "631104000077");
        ISOMsg r = send("visa", "reversal", reversal(p, "000078", "0420"));
        assertEquals("00", r.getString(39), "nothing to undo is still a reversal done");
        assertTrue(drain("reversal.visa").isEmpty());
        assertTrue(drain("reversal.fleet").isEmpty());
    }

    @Test
    void theNetworkCanFindWhatItIsAskedToUndo() throws Exception {
        ISOMsg p = purchase("29110093", "000502", "631104000502");
        send("visa", "purchase", p);
        ISOMsg seen = hosts.get(1).received().get(0);
        send("visa", "reversal", reversal(p, "000503", "0420"));
        List<ISOMsg> queued = drain("reversal.visa");
        assertEquals(1, queued.size());
        assertTrue(queued.get(0).getString(90).startsWith("0200" + seen.getString(11) + seen.getString(7)),
            "the reversal names the purchase the network received: 90=" + queued.get(0).getString(90));
    }

    @Test
    void repeatsAreSentUpstreamOnce() throws Exception {
        ISOMsg p = purchase("29110094", "000801", "631104000801");
        send("fleet", "purchase", p);
        send("fleet", "reversal", reversal(p, "000802", "0420"));
        ISOMsg repeat = send("fleet", "reversal", reversal(p, "000802", "0421"));
        assertEquals("00", repeat.getString(39));
        assertEquals(1, drain("reversal.fleet").size(), "the network undoes the purchase once, so it is asked once");
    }

    @Test
    @SuppressWarnings("unchecked")
    void whatTheSwitchKeepsHoldsNoCardSecrets() throws Exception {
        ISOMsg p = purchase("29110095", "000611", "631104000611");
        p.set(35, "5555555555554444=28122011234567890");
        p.set(52, "A1B2C3D4E5F60718");
        send("fleet", "purchase", p);
        assertTrue(hosts.get(0).received().get(0).hasField(35), "the network needs the track data to authorize");
        LocalSpace<String, Object> sp = (LocalSpace<String, Object>) ReversalRecords.SPACE;
        int records = 0;
        for (String key : sp.getKeySet()) {
            Object v = sp.rdp(key);
            if (v instanceof ReversalRecords.Sent) {
                records++;
                ISOMsg kept = ((ReversalRecords.Sent) v).message();
                assertTrue(kept.hasField(2));
                for (int f : new int[] { 35, 45, 52, 55 })
                    assertFalse(kept.hasField(f), "field " + f + " is kept by the switch");
            }
        }
        assertEquals(1, records, "the purchase is remembered");
    }

    @Test
    void twoTerminalsGiveTheSameRrn() throws Exception {
        ISOMsg a = purchase("29110096", "000035", "631104000035");
        ISOMsg b = purchase("29110097", "000035", "631104000035");
        send("fleet", "purchase", a);
        send("fleet", "purchase", b);
        List<ISOMsg> seen = hosts.get(0).received();
        send("fleet", "reversal", reversal(a, "000036", "0420"));
        send("fleet", "reversal", reversal(b, "000036", "0420"));
        List<ISOMsg> queued = drain("reversal.fleet");
        assertEquals(2, queued.size(), "each terminal's purchase is reversed");
        assertTrue(queued.get(0).getString(90).startsWith("0200" + seen.get(0).getString(11)),
            "the first terminal's reversal names its own purchase");
        assertTrue(queued.get(1).getString(90).startsWith("0200" + seen.get(1).getString(11)),
            "the second terminal's reversal names its own purchase");
    }

    // ---- requests ----

    private static ISOMsg purchase(String terminal, String stan, String rrn) throws Exception {
        ISOMsg m = new ISOMsg("0200");
        m.set(2, "5555555555554444");
        m.set(3, "000000");
        m.set(4, "000000006180");
        m.set(7, "1107081205");
        m.set(11, stan);
        m.set(37, rrn);
        m.set(41, terminal);
        m.set(49, "978");
        return m;
    }

    private static ISOMsg reversal(ISOMsg p, String stan, String mti) throws Exception {
        ISOMsg r = (ISOMsg) p.clone(2, 3, 4, 37, 41, 49);
        r.setMTI(mti);
        r.set(7, "1107081250");
        r.set(11, stan);
        return r;
    }

    private static List<ISOMsg> drain(String queue) {
        List<ISOMsg> l = new ArrayList<>();
        Object o;
        while ((o = ReversalRecords.SPACE.inp(queue)) != null) l.add((ISOMsg) o);
        return l;
    }

    private static ISOMsg send(String link, String kind, ISOMsg m) throws Exception {
        BlockingQueue<ISOMsg> sent = new LinkedBlockingQueue<>();
        BlockingQueue<Boolean> ended = new LinkedBlockingQueue<>();
        Context ctx = new Context();
        ctx.put("ENDED", ended);
        ctx.put("KIND", kind);
        ctx.put(ContextConstants.REQUEST.toString(), m);
        ctx.put(ContextConstants.DESTINATION.toString(), link);
        ctx.put(ContextConstants.SOURCE.toString(), new ISOSource() {
            @Override public void send(ISOMsg r) { sent.add(r); }
            @Override public boolean isConnected() { return true; }
        });
        SpaceFactory.getSpace().out("practice.switch", ctx);
        assertNotNull(ended.poll(20, TimeUnit.SECONDS), "the transaction did not end");
        ISOMsg r = sent.poll(5, TimeUnit.SECONDS);
        assertNotNull(r, "the terminal got no answer");
        return r;
    }

    // ---- test support: Q2 in this JVM, two QMUXes, and a network behind each ----

    private static Q2 q2;
    private static List<Host> hosts;

    @BeforeAll
    @SuppressWarnings("unchecked")
    static void startQ2() throws Exception {
        Path deploy = Files.createTempDirectory("deploy");
        for (String link : List.of("fleet", "visa"))
            Files.writeString(deploy.resolve("20_mux_" + link + ".xml"),
                "<mux class='org.jpos.q2.iso.QMUX' name='" + link + "'>\n"
              + "  <in>" + link + "-receive</in>\n  <out>" + link + "-send</out>\n"
              + "  <unhandled>" + link + "-unhandled</unhandled>\n</mux>\n");
        Files.writeString(deploy.resolve("30_txnmgr.xml"),
            "<txnmgr name='practice-switch' class='org.jpos.transaction.TransactionManager'>\n"
          + "  <property name='queue' value='practice.switch'/>\n"
          + "  <property name='sessions' value='2'/>\n"
          + "  <property name='debug' value='false'/>\n"
          + "  <participant class='org.jpos.transaction.participant.Switch'>\n"
          + "    <property name='txnname' value='KIND'/>\n"
          + "    <property name='purchase' value='forward'/>\n"
          + "    <property name='reversal' value='reverse'/>\n"
          + "  </participant>\n"
          + "  <participant class='com.example.practice.ReversalRecordsTest$AnswerFailures'/>\n"
          + "  <participant class='org.jpos.transaction.participant.SendResponse'/>\n"
          + "  <participant class='com.example.practice.ReversalRecordsTest$Finished'/>\n"
          + "  <group name='forward'>\n"
          + "    <participant class='com.example.practice.ReversalRecords$Outbound'/>\n"
          + "    <participant class='org.jpos.transaction.participant.QueryHost'>\n"
          + "      <property name='request' value='OUTBOUND'/>\n"
          + "      <property name='response' value='HOST_RESPONSE'/>\n"
          + "      <property name='timeout' value='1000'/>\n"
          + "    </participant>\n"
          + "    <participant class='com.example.practice.ReversalRecords$Reply'/>\n"
          + "  </group>\n"
          + "  <group name='reverse'>\n"
          + "    <participant class='com.example.practice.ReversalRecords$ReverseFromTerminal'/>\n"
          + "  </group>\n"
          + "</txnmgr>\n");
        LocalSpace<String, Object> sp = (LocalSpace<String, Object>) SpaceFactory.getSpace();
        hosts = List.of(new Host(sp, "fleet"), new Host(sp, "visa"));
        q2 = new Q2(deploy.toString());
        q2.start();
        assertNotNull(NameRegistrar.get("practice-switch", 15000), "the TransactionManager did not start");
        assertNotNull(NameRegistrar.get("mux.fleet", 15000), "the QMUX did not start");
        assertNotNull(NameRegistrar.get("mux.visa", 15000), "the QMUX did not start");
    }

    @AfterAll
    static void stopQ2() {
        if (q2 != null) q2.shutdown(true);
    }

    /** A network that approves every purchase. */
    static final class Host implements SpaceListener<String, Object> {
        private final LocalSpace<String, Object> sp;
        private final String in;
        private final List<ISOMsg> received = new ArrayList<>();

        Host(LocalSpace<String, Object> sp, String link) {
            this.sp = sp;
            this.in = link + "-receive";
            sp.addListener(link + "-send", this);
        }

        synchronized void reset() { received.clear(); }

        synchronized List<ISOMsg> received() { return new ArrayList<>(received); }

        @Override
        public void notify(String queue, Object value) {
            ISOMsg request = (ISOMsg) sp.inp(queue);
            if (request == null) return;
            synchronized (this) { received.add(request); }
            try {
                ISOMsg response = (ISOMsg) request.clone();
                response.setResponseMTI();
                response.set(38, "R4T8W1");
                response.set(39, "00");
                sp.out(in, response);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }

    /** Last on both paths: tells the test the transaction ended. */
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

    /** Answers a reported failure from the terminal's request: 91 when the network was unreachable, else 96. */
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
                    .clone(0, 2, 3, 4, 7, 11, 37, 41, 49);
                response.setResponseMTI();
                response.set(39, ctx.getResult().hasFailure(CMF.HOST_UNREACHABLE) ? "91" : "96");
                ctx.put(ContextConstants.RESPONSE.toString(), response);
            } catch (Exception e) {
                ctx.log(e);
            }
            return ABORTED | NO_JOIN;
        }
    }
}

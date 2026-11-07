package com.example.practice;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOSource;
import org.jpos.q2.Q2;
import org.jpos.rc.CMF;
import org.jpos.rc.IRC;
import org.jpos.rc.Result;
import org.jpos.space.LocalSpace;
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
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Runs ForwardLeg around a real QueryHost and real QMUXes, in a TransactionManager deployed by Q2 in this JVM. */
class ForwardLegTest {

    @BeforeEach
    void freshLinks() {
        for (String link : List.of("fleet", "visa")) {
            SpaceUtil.wipe(SpaceFactory.getSpace(), "stan." + link);
            hosts.get(link).reset();
        }
    }

    @Test
    void anApprovedPurchaseReachesTheTerminal() throws Exception {
        ISOMsg r = send("fleet", purchase("29110071", "004410"));
        assertEquals("0210", r.getMTI());
        assertEquals("00", r.getString(39));
        assertEquals("004410", r.getString(11));
        assertEquals("29110071", r.getString(41));
        assertEquals("Q7X2K9", r.getString(38), "the approval code reaches the terminal");
        ISOMsg seen = hosts.get("fleet").received().get(0);
        assertEquals("SW000001", seen.getString(41), "the network knows the switch by its own terminal ID");
        assertEquals("12345678901", seen.getString(32));
        assertFalse(seen.hasField(48), "the network refuses the terminals' private field");
    }

    @Test
    void aBalanceInquiryShowsTheBalance() throws Exception {
        ISOMsg inquiry = purchase("29110072", "000093");
        inquiry.set(3, "310000");
        inquiry.unset(4);
        ISOMsg r = send("fleet", inquiry);
        assertEquals("00", r.getString(39));
        assertEquals("000093", r.getString(11));
        assertEquals(Host.BALANCE, r.getString(54), "the balance the network returned reaches the terminal");
    }

    @Test
    void declinesAndSilenceAnswerTheTerminalsOwnRequest() throws Exception {
        hosts.get("visa").then("51");
        ISOMsg declined = send("visa", purchase("29110073", "120007"));
        assertEquals("51", declined.getString(39));
        assertEquals("120007", declined.getString(11), "the terminal matches the answer by its own STAN");
        assertEquals("29110073", declined.getString(41), "...and by its own terminal ID");
        hosts.get("fleet").then(Host.DROP);
        ISOMsg silent = send("fleet", purchase("29110074", "000318"));
        assertEquals("91", silent.getString(39), "the network did not answer");
        assertEquals("000318", silent.getString(11), "the terminal matches the answer by its own STAN");
        assertEquals("29110074", silent.getString(41), "...and by its own terminal ID");
        assertFalse(silent.hasField(32), "the switch's acquirer ID is not the terminal's business");
    }

    @Test
    void theReplyCarriesNothingPrivateOfTheNetwork() throws Exception {
        ISOMsg r = send("visa", purchase("29110075", "000562"));
        assertEquals("00", r.getString(39));
        assertEquals("29110075", r.getString(41), "the terminal matches the answer by its own terminal ID");
        assertEquals("000562", r.getString(11));
        assertFalse(r.hasField(63), "the network's private field stays at the switch");
        assertFalse(r.hasField(32), "the switch's acquirer ID stays at the switch");
    }

    @Test
    void eachLinkNumbersItsOwnMessages() throws Exception {
        send("fleet", purchase("29110076", "000700"));
        send("visa", purchase("29110076", "000701"));
        send("fleet", purchase("29110076", "000702"));
        assertEquals(List.of("000001", "000002"), stans(hosts.get("fleet").received()));
        assertEquals(List.of("000001"), stans(hosts.get("visa").received()),
            "a link's numbering doesn't depend on traffic on other links");
    }

    @Test
    void twoTerminalsWithTheSameStanAtTheSameMoment() throws Exception {
        Host fleet = hosts.get("fleet");
        fleet.then(Host.HOLD, Host.HOLD);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<ISOMsg> a = pool.submit(() -> send("fleet", purchase("29110081", "000017")));
            assertTrue(fleet.awaitArrivals(1), "the first purchase reached the network");
            Future<ISOMsg> b = pool.submit(() -> send("fleet", purchase("29110082", "000017")));
            assertTrue(fleet.awaitArrivals(1), "the second purchase reached the network while the first waited");
            fleet.releaseHeld();
            ISOMsg ra = a.get(20, TimeUnit.SECONDS), rb = b.get(20, TimeUnit.SECONDS);
            assertEquals("00", ra.getString(39));
            assertEquals("00", rb.getString(39));
            assertEquals("29110081", ra.getString(41));
            assertEquals("29110082", rb.getString(41));
        } finally {
            pool.shutdownNow();
        }
    }

    // ---- requests ----

    private static ISOMsg purchase(String terminal, String stan) throws Exception {
        ISOMsg m = new ISOMsg("0200");
        m.set(2, "5555555555554444");
        m.set(3, "000000");
        m.set(4, "000000006180");
        m.set(7, "1107081205");
        m.set(11, stan);
        m.set(41, terminal);
        m.set(48, "PUMP04");
        m.set(49, "978");
        return m;
    }

    private static List<String> stans(List<ISOMsg> ms) {
        List<String> s = new ArrayList<>();
        for (ISOMsg m : ms) s.add(m.getString(11));
        return s;
    }

    /** Hands the request to the switch's TransactionManager and returns what the terminal got. */
    private static ISOMsg send(String link, ISOMsg m) throws Exception {
        BlockingQueue<ISOMsg> sent = new LinkedBlockingQueue<>();
        BlockingQueue<Boolean> ended = new LinkedBlockingQueue<>();
        Context ctx = new Context();
        ctx.put("ENDED", ended);
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

    // ---- test support: Q2 in this JVM, two QMUXes, and a network on the far side of each ----

    private static Q2 q2;
    private static Map<String, Host> hosts;

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
          + "  <participant class='com.example.practice.ForwardLeg$Outbound'>\n"
          + "    <property name='terminal-id' value='SW000001'/>\n"
          + "    <property name='acquirer-id' value='12345678901'/>\n"
          + "  </participant>\n"
          + "  <participant class='org.jpos.transaction.participant.QueryHost'>\n"
          + "    <property name='request' value='OUTBOUND'/>\n"
          + "    <property name='response' value='HOST_RESPONSE'/>\n"
          + "    <property name='timeout' value='1000'/>\n"
          + "  </participant>\n"
          + "  <participant class='com.example.practice.ForwardLeg$Reply'/>\n"
          + "  <participant class='com.example.practice.ForwardLegTest$AnswerFailures'/>\n"
          + "  <participant class='org.jpos.transaction.participant.SendResponse'/>\n"
          + "  <participant class='com.example.practice.ForwardLegTest$Finished'/>\n"
          + "</txnmgr>\n");
        LocalSpace<String, Object> sp = (LocalSpace<String, Object>) SpaceFactory.getSpace();
        hosts = Map.of("fleet", new Host(sp, "fleet"), "visa", new Host(sp, "visa"));
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

    /** A network: approves (with an approval code, and a balance for an inquiry), declines, drops or holds. */
    static final class Host implements SpaceListener<String, Object> {
        static final String DROP = "drop", HOLD = "hold", BALANCE = "1001978C000000012500";
        private final LocalSpace<String, Object> sp;
        private final String in;
        private final Deque<String> script = new ArrayDeque<>();
        private final List<ISOMsg> received = new ArrayList<>();
        private final List<ISOMsg> held = new ArrayList<>();
        private final Semaphore arrivals = new Semaphore(0);

        Host(LocalSpace<String, Object> sp, String link) {
            this.sp = sp;
            this.in = link + "-receive";
            sp.addListener(link + "-send", this);
        }

        synchronized void reset() {
            script.clear();
            received.clear();
            held.clear();
            arrivals.drainPermits();
        }

        synchronized void then(String... acts) {
            script.addAll(List.of(acts));
        }

        synchronized List<ISOMsg> received() {
            return new ArrayList<>(received);
        }

        boolean awaitArrivals(int n) throws InterruptedException {
            return arrivals.tryAcquire(n, 10, TimeUnit.SECONDS);
        }

        void releaseHeld() throws Exception {
            List<ISOMsg> now;
            synchronized (this) { now = new ArrayList<>(held); held.clear(); }
            for (ISOMsg m : now) respond(m, "00");
        }

        @Override
        public void notify(String queue, Object value) {
            ISOMsg request = (ISOMsg) sp.inp(queue);
            if (request == null) return;
            String act;
            synchronized (this) {
                received.add(request);
                act = script.isEmpty() ? "00" : script.poll();
                if (HOLD.equals(act)) held.add(request);
            }
            try {
                if (!DROP.equals(act) && !HOLD.equals(act)) respond(request, act);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            } finally {
                arrivals.release();
            }
        }

        private void respond(ISOMsg request, String rc) throws Exception {
            ISOMsg response = (ISOMsg) request.clone();
            response.setResponseMTI();
            response.set(39, rc);
            if ("00".equals(rc)) {
                response.set(38, "Q7X2K9");
                if (request.getString(3).startsWith("31")) response.set(54, BALANCE);
            }
            response.set(63, "NETWORK-PRIVATE");
            sp.out(in, response);
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

    /** Unit s10's answering participant, cut down: answers a reported cause from the terminal's request. */
    public static class AnswerFailures implements AbortParticipant {
        private static final Map<IRC, String> CODES = Map.of(
            CMF.DO_NOT_HONOUR, "05", CMF.NOT_SUFFICIENT_FUNDS, "51", CMF.EXPIRED, "54", CMF.HOST_UNREACHABLE, "91");

        @Override
        public int prepare(long id, Serializable context) {
            return PREPARED | NO_JOIN | READONLY;
        }

        @Override
        public int prepareForAbort(long id, Serializable context) {
            Context ctx = (Context) context;
            try {
                ISOMsg response = (ISOMsg) ((ISOMsg) ctx.get(ContextConstants.REQUEST.toString()))
                    .clone(ForwardLeg.ECHOED);
                response.setResponseMTI();
                Result.Entry failure = ctx.getResult().failure();
                response.set(39, failure == null ? "96" : CODES.getOrDefault(failure.getIrc(), "96"));
                ctx.put(ContextConstants.RESPONSE.toString(), response);
            } catch (Exception e) {
                ctx.log(e);
            }
            return ABORTED | NO_JOIN;
        }
    }
}

package com.example.practice;

import org.jpos.iso.ISOMsg;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LateAnswersTest {

    private static ISOMsg purchase(String mti, String stan) throws Exception {
        ISOMsg m = new ISOMsg(mti);
        m.set(2, "4111111111111111");
        m.set(3, "000000");
        m.set(4, "000000008840");
        m.set(11, stan);
        m.set(41, "29110018");
        return m;
    }

    private static ISOMsg echo(String stan) throws Exception {
        ISOMsg m = new ISOMsg("0800");
        m.set(11, stan);
        m.set(41, "29110018");
        m.set(70, "301");
        return m;
    }

    /** Sends m, lets the request give up before the host answers, then has the host answer it. */
    private static void answerLate(Rig rig, ISOMsg m, String act) throws Exception {
        rig.host.then(Host.HOLD);
        assertNull(rig.mux.request(m, 100), "the request gives up first");
        rig.host.awaitArrivals(1);
        if (Host.ANSWER.equals(act)) rig.host.releaseHeld();
        else rig.host.releaseHeldWith(act);
    }

    private static ISOMsg unhandled(Rig rig) {
        return (ISOMsg) rig.sp.rdp("host-unhandled");
    }

    private static final LateAnswers.Ledger BROKEN = r -> {
        throw new IllegalStateException("ledger table is locked");
    };

    @Test
    void aLateApprovalReachesTheLedger() throws Exception {
        Rig rig = new Rig(true);
        List<ISOMsg> ledger = Collections.synchronizedList(new ArrayList<>());
        rig.mux.addISORequestListener(new LateAnswers(ledger::add));
        answerLate(rig, purchase("0200", "000771"), Host.ANSWER);
        assertEquals(1, ledger.size());
        assertEquals("000771", ledger.get(0).getString(11));
        assertNull(unhandled(rig), "the listener took it");
    }

    @Test
    void aMessageTheHostStartsGoesOn() throws Exception {
        Rig rig = new Rig(true);
        List<ISOMsg> ledger = Collections.synchronizedList(new ArrayList<>());
        rig.mux.addISORequestListener(new LateAnswers(ledger::add));
        rig.sp.out("host-receive", echo("100772"));     // the host asks if we are alive
        assertTrue(ledger.isEmpty());
        ISOMsg u = unhandled(rig);
        assertNotNull(u, "it belongs to whoever answers the host's echoes");
        assertEquals("0800", u.getMTI());
    }

    @Test
    void aLateDeclineIsNotAnApproval() throws Exception {
        Rig rig = new Rig(true);
        List<ISOMsg> ledger = Collections.synchronizedList(new ArrayList<>());
        rig.mux.addISORequestListener(new LateAnswers(ledger::add));
        answerLate(rig, purchase("0100", "000773"), "51");
        assertTrue(ledger.isEmpty(), "no money moved");
        assertNotNull(unhandled(rig));
    }

    @Test
    void aLateEchoAnswerMovedNoMoney() throws Exception {
        Rig rig = new Rig(true);
        List<ISOMsg> ledger = Collections.synchronizedList(new ArrayList<>());
        rig.mux.addISORequestListener(new LateAnswers(ledger::add));
        answerLate(rig, echo("000774"), Host.ANSWER);
        assertTrue(ledger.isEmpty(), "an 0810 is not a purchase");
        assertNotNull(unhandled(rig));
    }

    @Test
    void aBrokenLedgerCostsNoOtherAnswer() throws Exception {
        Rig rig = new Rig(true);
        rig.mux.addISORequestListener(new LateAnswers(BROKEN));
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            rig.host.then(Host.HOLD, Host.HOLD);
            assertNull(rig.mux.request(purchase("0200", "000775"), 100), "this one gives up first");
            Future<ISOMsg> waiting = pool.submit(() -> rig.mux.request(purchase("0200", "000776"), 30000));
            rig.host.awaitArrivals(2);
            try {
                rig.host.releaseHeld();                  // the late 000775 first, then 000776
            } catch (RuntimeException e) {
                throw new AssertionError("the failure reached the thread that delivers answers", e);
            }
            ISOMsg r = waiting.get();
            assertNotNull(r, "000776 was answered");
            assertEquals("000776", r.getString(11));
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void aLateApprovalTheLedgerRefusedIsKept() throws Exception {
        Rig rig = new Rig(true);
        rig.mux.addISORequestListener(new LateAnswers(BROKEN));
        answerLate(rig, purchase("0200", "000777"), Host.ANSWER);
        ISOMsg u = unhandled(rig);
        assertNotNull(u, "someone must still find this approval");
        assertEquals("000777", u.getString(11));
    }
    // ---- test support: a real QMUX in this JVM, and a host on the far side of its queues ----

    private static final java.util.concurrent.atomic.AtomicInteger SPACES = new java.util.concurrent.atomic.AtomicInteger();

    static final class Rig {
        final org.jpos.space.LocalSpace<String, Object> sp;
        final org.jpos.q2.iso.QMUX mux;
        final Host host;

        Rig(boolean linkUp) throws Exception {
            String space = "tspace:practice-" + SPACES.incrementAndGet();
            sp = (org.jpos.space.LocalSpace<String, Object>) org.jpos.space.SpaceFactory.getSpace(space);
            org.jdom2.Element e = new org.jdom2.Element("mux");
            e.addContent(new org.jdom2.Element("space").setText(space));
            e.addContent(new org.jdom2.Element("in").setText("host-receive"));
            e.addContent(new org.jdom2.Element("out").setText("host-send"));
            e.addContent(new org.jdom2.Element("ready").setText("host.ready"));
            e.addContent(new org.jdom2.Element("unhandled").setText("host-unhandled"));
            mux = new org.jpos.q2.iso.QMUX();
            mux.setName("host-mux-" + space);
            mux.setPersist(e);
            mux.setConfiguration(new org.jpos.core.SimpleConfiguration());
            mux.init();
            mux.start();
            if (linkUp) sp.out("host.ready", Boolean.TRUE);
            host = new Host(sp);
        }
    }

    /** Answers (39=00), declines (39=05), drops or holds each request, in script order. */
    static final class Host implements org.jpos.space.SpaceListener<String, Object> {
        static final String ANSWER = "answer", DECLINE = "decline", DROP = "drop", HOLD = "hold";
        private final org.jpos.space.LocalSpace<String, Object> sp;
        private final java.util.Deque<String> script = new java.util.ArrayDeque<>();
        private final java.util.List<ISOMsg> received = new java.util.ArrayList<>();
        private final java.util.List<ISOMsg> held = new java.util.ArrayList<>();
        private final java.util.concurrent.Semaphore arrivals = new java.util.concurrent.Semaphore(0);

        Host(org.jpos.space.LocalSpace<String, Object> sp) {
            this.sp = sp;
            sp.addListener("host-send", this);
        }

        synchronized Host then(String... acts) {
            script.addAll(java.util.List.of(acts));
            return this;
        }

        synchronized java.util.List<ISOMsg> received() {
            return new java.util.ArrayList<>(received);
        }

        /** Waits (up to a generous limit) until n requests have arrived in total since the last call. */
        void awaitArrivals(int n) throws InterruptedException {
            if (!arrivals.tryAcquire(n, 30, java.util.concurrent.TimeUnit.SECONDS))
                throw new AssertionError("the host did not receive " + n + " request(s)");
        }

        /** Answers every held request with the given field 39. */
        void releaseHeldWith(String rc) throws Exception {
            java.util.List<ISOMsg> now;
            synchronized (this) { now = new java.util.ArrayList<>(held); held.clear(); }
            for (ISOMsg m : now) respond(m, rc);
        }

        /** Answers every held request, approving it. */
        void releaseHeld() throws Exception {
            java.util.List<ISOMsg> now;
            synchronized (this) { now = new java.util.ArrayList<>(held); held.clear(); }
            for (ISOMsg m : now) respond(m, "00");
        }

        @Override
        public void notify(String queue, Object value) {
            ISOMsg request = (ISOMsg) sp.inp(queue);
            if (request == null) return;
            String act;
            synchronized (this) {
                received.add(request);
                act = script.isEmpty() ? ANSWER : script.poll();
                if (HOLD.equals(act)) held.add(request);
            }
            try {
                if (ANSWER.equals(act)) respond(request, "00");
                else if (DECLINE.equals(act)) respond(request, "05");
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            } finally {
                arrivals.release();
            }
        }

        private void respond(ISOMsg request, String rc) throws Exception {
            ISOMsg response = (ISOMsg) request.clone();
            response.setResponseMTI();
            response.set(39, rc);
            sp.out("host-receive", response);
        }
    }
}

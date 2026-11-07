package com.example.practice;

import org.jpos.iso.ISOMsg;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UpstreamTest {

    private static ISOMsg purchase(String terminal, String stan) throws Exception {
        ISOMsg m = new ISOMsg("0200");
        m.set(3, "000000");
        m.set(4, "000000002500");
        m.set(11, stan);
        m.set(41, terminal);
        return m;
    }

    /** Sends two purchases that are both in flight at the same moment, then lets the host answer. */
    private static ISOMsg[] bothInFlight(Upstream gw, Rig rig, ISOMsg a, ISOMsg b) throws Exception {
        rig.host.then(Host.HOLD, Host.HOLD);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<ISOMsg> first = pool.submit(() -> gw.forward(a));
            rig.host.awaitArrivals(1);
            Future<ISOMsg> second = pool.submit(() -> gw.forward(b));
            rig.host.awaitArrivals(1);
            rig.host.releaseHeld();
            return new ISOMsg[] { first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS) };
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void aTerminalGetsTheHostsAnswer() throws Exception {
        Rig rig = new Rig(true);
        Upstream gw = new Upstream(rig.mux, "GW000001");
        ISOMsg answer = gw.forward(purchase("29110001", "000731"));
        assertNotNull(answer, "the host answered at once");
        assertEquals("0210", answer.getMTI());
        assertEquals("00", answer.getString(39));
        assertEquals("000000002500", answer.getString(4));
        assertEquals("GW000001", rig.host.received().get(0).getString(41), "the host knows the gateway by its own ID");
    }

    @Test
    void theTerminalGetsItsOwnStanBack() throws Exception {
        Rig rig = new Rig(true);
        Upstream gw = new Upstream(rig.mux, "GW000001");
        assertEquals("000731", gw.forward(purchase("29110001", "000731")).getString(11));
        assertEquals("000732", gw.forward(purchase("29110001", "000732")).getString(11));
    }

    @Test
    void theTerminalGetsItsOwnIdBack() throws Exception {
        Rig rig = new Rig(true);
        Upstream gw = new Upstream(rig.mux, "GW000001");
        assertEquals("29110001", gw.forward(purchase("29110001", "000731")).getString(41));
        assertEquals("29110077", gw.forward(purchase("29110077", "000731")).getString(41));
    }

    @Test
    void twoTerminalsWithTheSameStanAtTheSameMoment() throws Exception {
        Rig rig = new Rig(true);
        Upstream gw = new Upstream(rig.mux, "GW000001");
        ISOMsg[] answers = bothInFlight(gw, rig, purchase("29110001", "000042"), purchase("29110002", "000042"));
        assertEquals("29110001", answers[0].getString(41));
        assertEquals("29110002", answers[1].getString(41));
        assertEquals("000042", answers[0].getString(11));
        assertEquals("000042", answers[1].getString(11));
    }

    @Test
    void twoTerminalsStartingTheirDayAtTheSameMoment() throws Exception {
        Rig rig = new Rig(true);
        Upstream gw = new Upstream(rig.mux, "GW000001");
        ISOMsg[] answers = bothInFlight(gw, rig, purchase("29110001", "000017"), purchase("29110002", "000503"));
        assertEquals("000017", answers[0].getString(11));
        assertEquals("000503", answers[1].getString(11));
        java.util.List<ISOMsg> seen = rig.host.received();
        assertNotEquals(seen.get(0).getString(11), seen.get(1).getString(11),
                "two messages in flight on one link never share a STAN");
    }

    @Test
    void aLongRunningLinkNeverSendsStanZero() throws Exception {
        Rig rig = new Rig(true);
        Upstream gw = new Upstream(rig.mux, "GW000001");
        String last = null;
        for (int i = 1; i <= 1_000_000; i++) {
            last = gw.nextStan();
            if (last.equals("000000"))
                throw new AssertionError("message " + i + " got STAN 000000");
            assertEquals(6, last.length());
        }
        assertEquals("000001", last, "after 999999 the numbering starts again at 000001");
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

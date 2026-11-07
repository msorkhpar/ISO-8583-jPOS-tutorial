package com.example.practice;

import org.jpos.iso.ISOMsg;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdviceSafTest {

    private static ISOMsg advice(String stan) throws Exception {
        ISOMsg m = new ISOMsg("0220");
        m.set(2, "4111111111111111");
        m.set(4, "000000004200");
        m.set(11, stan);
        m.set(41, "29110001");
        return m;
    }

    private static String seen(Rig rig) {
        StringBuilder sb = new StringBuilder();
        for (ISOMsg m : rig.host.received())
            sb.append(sb.length() == 0 ? "" : " ").append(sneak(m));
        return sb.toString();
    }

    private static String sneak(ISOMsg m) {
        try {
            return m.getMTI() + ":" + m.getString(11);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** A MUX whose process dies in the middle of a request. */
    static final class PowerCut implements org.jpos.iso.MUX {
        @Override public ISOMsg request(ISOMsg m, long timeout) { throw new IllegalStateException("power cut"); }
        @Override public void request(ISOMsg m, long t, org.jpos.iso.ISOResponseListener l, Object h) { throw new IllegalStateException("power cut"); }
        @Override public void send(ISOMsg m) { throw new IllegalStateException("power cut"); }
        @Override public boolean isConnected() { return true; }
    }

    @Test
    void aStuckAdviceDoesNotBlockTheNextOne() throws Exception {
        Rig rig = new Rig(true);
        rig.host.then(Host.DROP, Host.DROP, Host.DROP);
        AdviceSaf saf = new AdviceSaf(new AdviceSaf.Store(), new AdviceSaf.Store(), 3);
        saf.add(advice("000301"));
        saf.add(advice("000302"));
        for (int i = 0; i < 12 && !saf.isEmpty(); i++)
            saf.sendOldest(rig.mux, 1000);
        assertTrue(saf.isEmpty(), "the line moved on");
        java.util.List<ISOMsg> sent = rig.host.received();
        assertEquals("000302", sent.get(sent.size() - 1).getString(11),
                "the advice behind the stuck one was delivered: " + seen(rig));
    }

    @Test
    void anAdviceIsParkedAfterItsLastAttempt() throws Exception {
        Rig rig = new Rig(true);
        rig.host.then(Host.DROP, Host.DROP);
        AdviceSaf.Store parked = new AdviceSaf.Store();
        AdviceSaf saf = new AdviceSaf(new AdviceSaf.Store(), parked, 2);
        saf.add(advice("000301"));
        for (int i = 0; i < 4 && !saf.isEmpty(); i++)
            saf.sendOldest(rig.mux, 1000);
        assertEquals("0220:000301 0221:000301", seen(rig), "two attempts, then no more");
        assertEquals(1, parked.all().size());
    }

    @Test
    void aParkedAdviceIsKept() throws Exception {
        Rig rig = new Rig(true);
        rig.host.then(Host.DROP, Host.DROP);
        AdviceSaf.Store parked = new AdviceSaf.Store();
        AdviceSaf saf = new AdviceSaf(new AdviceSaf.Store(), parked, 2);
        saf.add(advice("000301"));
        saf.sendOldest(rig.mux, 1000);
        saf.sendOldest(rig.mux, 1000);
        assertEquals(1, parked.all().size());
        assertEquals("000301", parked.all().get(0).getString(11));
        assertEquals("0221", parked.all().get(0).getMTI());
    }

    @Test
    void attemptsAreNotForgottenByARestart() throws Exception {
        Rig rig = new Rig(true);
        rig.host.then(Host.DROP, Host.DROP, Host.DROP);
        AdviceSaf.Store table = new AdviceSaf.Store();
        AdviceSaf.Store parked = new AdviceSaf.Store();
        new AdviceSaf(table, parked, 3).add(advice("000301"));
        for (int i = 0; i < 3; i++)
            new AdviceSaf(table, parked, 3).sendOldest(rig.mux, 1000);   // a restart between attempts
        assertEquals(1, parked.all().size(), "three attempts are three attempts, restarts or not");
        assertTrue(new AdviceSaf(table, parked, 3).isEmpty());
    }

    @Test
    void timeWithTheLinkDownIsNotAnAttempt() throws Exception {
        Rig rig = new Rig(false);
        AdviceSaf.Store parked = new AdviceSaf.Store();
        AdviceSaf saf = new AdviceSaf(new AdviceSaf.Store(), parked, 2);
        saf.add(advice("000301"));
        for (int i = 0; i < 5; i++)
            assertFalse(saf.sendOldest(rig.mux, 1000));
        rig.sp.out("host.ready", Boolean.TRUE);
        rig.host.then(Host.DROP);
        assertFalse(saf.sendOldest(rig.mux, 1000));
        assertTrue(saf.sendOldest(rig.mux, 1000), "one real attempt so far, so a second one is made");
        assertEquals(0, parked.all().size());
    }

    @Test
    void theLastAllowedAttemptIsMade() throws Exception {
        Rig rig = new Rig(true);
        rig.host.then(Host.DROP, Host.DROP);
        AdviceSaf.Store parked = new AdviceSaf.Store();
        AdviceSaf saf = new AdviceSaf(new AdviceSaf.Store(), parked, 3);
        saf.add(advice("000301"));
        saf.sendOldest(rig.mux, 1000);
        saf.sendOldest(rig.mux, 1000);
        assertTrue(saf.sendOldest(rig.mux, 1000), "the third of three attempts goes out");
        assertEquals(0, parked.all().size());
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

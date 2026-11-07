package com.example.practice;

import org.jpos.iso.ISOMsg;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentEndpointTest {

    private static ISOMsg purchase(String stan, String amount) throws Exception {
        ISOMsg m = new ISOMsg("0200");
        m.set(2, "5555555555554444");
        m.set(3, "000000");
        m.set(4, amount);
        m.set(11, stan);
        m.set(41, "29110077");
        return m;
    }

    @Test
    void anApprovedPurchaseIs200() throws Exception {
        Rig rig = new Rig(true);
        PaymentEndpoint api = new PaymentEndpoint(rig.mux, 1000);
        assertEquals(200, api.handle(purchase("004410", "000000012999")));
        assertEquals(1, rig.host.received().size());
        assertEquals("000000012999", rig.host.received().get(0).getString(4));
        assertTrue(api.unknown.isEmpty());
    }

    @Test
    void aRefusalIsNotAnApproval() throws Exception {
        Rig rig = new Rig(true);
        rig.host.then(Host.DECLINE);
        PaymentEndpoint api = new PaymentEndpoint(rig.mux, 1000);
        assertEquals(402, api.handle(purchase("004411", "000000250000")),
                "the host answered with 39=51 (insufficient funds)");
        assertTrue(api.unknown.isEmpty(), "a declined purchase moved no money");
    }

    @Test
    void silenceIsAnUnknownOutcome() throws Exception {
        Rig rig = new Rig(true);
        rig.host.then(Host.DROP);
        PaymentEndpoint api = new PaymentEndpoint(rig.mux, 300);
        ISOMsg p = purchase("004412", "000000003175");
        assertEquals(504, api.handle(p));
        assertEquals(1, api.unknown.size(), "the purchase waits for the reversal job");
        assertSame(p, api.unknown.get(0));
    }

    @Test
    void silenceIsNotAReasonToAskAgain() throws Exception {
        Rig rig = new Rig(true);
        rig.host.then(Host.DROP, Host.ANSWER);
        PaymentEndpoint api = new PaymentEndpoint(rig.mux, 300);
        int status = api.handle(purchase("004413", "000000003175"));
        assertEquals(1, rig.host.received().size(), "the host may already have approved the first one");
        assertEquals(504, status);
    }

    @Test
    void aDownLinkIs503AndNothingGoesOut() throws Exception {
        Rig rig = new Rig(false);
        PaymentEndpoint api = new PaymentEndpoint(rig.mux, 300);
        assertEquals(503, api.handle(purchase("004414", "000000000899")));
        assertEquals(0, rig.host.received().size(), "a 503 promises that nothing reached the host");
        assertTrue(api.unknown.isEmpty(), "what never went out has nothing to undo");
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

    /** Answers (39=00), declines (39=51), drops or holds each request, in script order. */
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
                else if (DECLINE.equals(act)) respond(request, "51");
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

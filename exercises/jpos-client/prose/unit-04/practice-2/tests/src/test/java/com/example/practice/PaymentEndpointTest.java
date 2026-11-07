package com.example.practice;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentEndpointTest {

    private static ISOMsg purchase(String stan, String amount) throws Exception {
        ISOMsg m = new ISOMsg("0200");
        m.set(2, "5555555555554444");
        m.set(3, "000000");
        m.set(4, amount);
        m.set(11, stan);
        m.set(41, "29110063");
        return m;
    }

    private static int await(CompletableFuture<Integer> f) throws Exception {
        return f.get(20, TimeUnit.SECONDS);
    }

    @Test
    void anApprovedPurchaseCompletesWith200() throws Exception {
        Rig rig = new Rig(true);
        PaymentEndpoint api = new PaymentEndpoint(rig.mux, 2000);
        assertEquals(200, await(api.handleAsync(purchase("007120", "000000004450"))));
        assertEquals(1, rig.host.received().size());
        assertTrue(api.unknown.isEmpty());
    }

    @Test
    void silenceStillCompletesTheFuture() throws Exception {
        Rig rig = new Rig(true);
        rig.host.then(Host.DROP);
        PaymentEndpoint api = new PaymentEndpoint(rig.mux, 300);
        ISOMsg p = purchase("007121", "000000001995");
        assertEquals(504, await(api.handleAsync(p)), "the caller must not wait forever");
        assertEquals(1, api.unknown.size());
    }

    @Test
    void aRefusedPurchaseFailsTheFuture() throws Exception {
        Rig rig = new Rig(true);
        rig.host.then(Host.HOLD);
        PaymentEndpoint api = new PaymentEndpoint(rig.mux, 2000);
        CompletableFuture<Integer> first = api.handleAsync(purchase("007122", "000000001995"));
        CompletableFuture<Integer> twin = api.handleAsync(purchase("007122", "000000001995"));
        ExecutionException e = assertThrows(ExecutionException.class, () -> twin.get(20, TimeUnit.SECONDS),
                "the MUX refused the second one: it never left");
        assertInstanceOf(ISOException.class, e.getCause());
        assertTrue(api.unknown.isEmpty(), "a refused purchase has nothing to undo");
        rig.host.awaitArrivals(1);
        rig.host.releaseHeld();
        assertEquals(200, await(first));
    }

    @Test
    void aDownLinkCompletesWith503AndNothingGoesOut() throws Exception {
        Rig rig = new Rig(false);
        PaymentEndpoint api = new PaymentEndpoint(rig.mux, 300);
        assertEquals(503, await(api.handleAsync(purchase("007123", "000000000250"))));
        assertEquals(0, rig.host.received().size());
        assertTrue(api.unknown.isEmpty());
    }

    @Test
    void theCallerGetsItsFutureBeforeTheAnswer() throws Exception {
        Rig rig = new Rig(true);
        rig.host.then(Host.HOLD);
        PaymentEndpoint api = new PaymentEndpoint(rig.mux, 2000);
        CompletableFuture<Integer> f = api.handleAsync(purchase("007124", "000000007000"));
        assertFalse(f.isDone(), "the host has not answered yet, so nothing can be known");
        rig.host.awaitArrivals(1);
        rig.host.releaseHeld();
        assertEquals(200, await(f));
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

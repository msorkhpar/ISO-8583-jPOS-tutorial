package com.example.practice;

import org.jpos.iso.ISOMsg;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LinkStopTest {
    private static final String LINK = "acq-mux";

    /** Signs the link on in the space, as the sign-on manager would have. */
    private static Date signedOn(Rig rig) {
        Date ready = new Date();
        rig.sp.put(Rig.READY, ready);
        rig.sp.put(LINK + ".logon", ready);
        rig.sp.put(LINK + ".enabled", ready);
        rig.sp.put(LINK + ".echo", Boolean.TRUE);
        return ready;
    }

    private static ISOMsg purchase(String stan) throws Exception {
        ISOMsg m = new ISOMsg("0200");
        m.set(2, "5555555555554444");
        m.set(3, "000000");
        m.set(4, "000000012345");
        m.set(11, stan);
        m.set(41, "29110007");
        return m;
    }

    /** Sends a purchase the host holds, and returns once the QMUX counts it as waiting. */
    private static Future<ISOMsg> inFlight(Rig rig, ExecutorService pool, String stan) throws Exception {
        rig.host.then(Host.HOLD);
        Future<ISOMsg> f = pool.submit(() -> rig.mux.request(purchase(stan), 30000));
        rig.host.awaitArrivals(1);
        rig.awaitPending(1);
        return f;
    }

    private static LinkStop stop(Rig rig, Pauses pauses, int maxPauses) {
        return new LinkStop(rig.sp, rig.mux, LINK, Rig.READY, pauses, maxPauses);
    }

    @Test
    void anIdleSignedOnLinkIsSignedOffAndForgotten() throws Exception {
        Rig rig = new Rig();
        Date ready = signedOn(rig);
        assertEquals(0, stop(rig, new Pauses(rig, 0, 20), 20).stop());
        assertEquals(1, rig.host.signOffs(), "one 0800 / 002 reached the host");
        assertNull(rig.sp.rdp(LINK + ".enabled"));
        assertNull(rig.sp.rdp(LINK + ".logon"));
        assertNull(rig.sp.rdp(LINK + ".echo"));
        assertTrue(rig.sp.rdp(Rig.READY) == ready, "the ready entry belongs to the adaptor");
    }

    @Test
    void aConnectionThatNeverSignedOnIsForgottenWithoutASignOff() throws Exception {
        Rig rig = new Rig();
        signedOn(rig);
        rig.sp.put(Rig.READY, new Date());              // the adaptor reconnected since
        stop(rig, new Pauses(rig, 0, 20), 20).stop();
        assertEquals(0, rig.host.signOffs(), "no session to end on this connection");
        assertNull(rig.sp.rdp(LINK + ".enabled"));
        assertNull(rig.sp.rdp(LINK + ".logon"));
    }

    @Test
    void theSignOffWaitsForTheRequestInFlight() throws Exception {
        Rig rig = new Rig();
        signedOn(rig);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        Future<ISOMsg> purchase = inFlight(rig, pool, "004711");
        Pauses pauses = new Pauses(rig, 2, 20);
        assertEquals(0, stop(rig, pauses, 20).stop());
        assertEquals(1, rig.host.signOffs());
        assertEquals(0, rig.host.heldAtSignOff(), "the sign-off overtook a purchase still waiting for its answer");
        ISOMsg r = purchase.get(30, TimeUnit.SECONDS);
        assertNotNull(r, "the purchase got its answer");
        assertEquals("00", r.getString(39));
        pool.shutdownNow();
    }

    @Test
    void nothingNewIsSentWhileTheLinkDrains() throws Exception {
        Rig rig = new Rig();
        signedOn(rig);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        inFlight(rig, pool, "004712");
        Pauses pauses = new Pauses(rig, 3, 20);
        stop(rig, pauses, 20).stop();
        assertTrue(pauses.taken > 0);
        assertFalse(pauses.enabledSeen, "the link was still in the pool while it drained");
        pool.shutdownNow();
    }

    @Test
    void aRequestThatNeverReturnsDoesNotHoldUpTheStop() throws Exception {
        Rig rig = new Rig();
        signedOn(rig);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        inFlight(rig, pool, "004713");
        Pauses pauses = new Pauses(rig, 0, 6);          // never released
        assertEquals(1, stop(rig, pauses, 6).stop(), "one request was still waiting");
        assertEquals(1, rig.host.signOffs(), "the link is signed off anyway");
        assertNull(rig.sp.rdp(LINK + ".logon"));
        rig.host.releaseHeld();
        pool.shutdownNow();
    }

    @Test
    void anIdleLinkSignsOffWithoutPausing() throws Exception {
        Rig rig = new Rig();
        signedOn(rig);
        Pauses pauses = new Pauses(rig, 0, 20);
        stop(rig, pauses, 20).stop();
        assertEquals(0, pauses.taken, "nothing was in flight");
        assertEquals(1, rig.host.signOffs());
    }

    @Test
    void theDrainEndsAsSoonAsTheLastAnswerArrives() throws Exception {
        Rig rig = new Rig();
        signedOn(rig);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        inFlight(rig, pool, "004714");
        Pauses pauses = new Pauses(rig, 1, 50);
        stop(rig, pauses, 50).stop();
        assertEquals(1, pauses.taken, "the answer came during the first pause");
        assertEquals(1, rig.host.signOffs());
        pool.shutdownNow();
    }

    /** A pause that takes no time: it notes what it sees and can answer the held requests. */
    static final class Pauses implements LinkStop.Pause {
        private final Rig rig;
        private final int releaseAt;
        private final int limit;
        int taken;
        boolean enabledSeen;

        /** releaseAt: on which pause the host answers what it holds (0: never). */
        Pauses(Rig rig, int releaseAt, int maxPauses) {
            this.rig = rig;
            this.releaseAt = releaseAt;
            this.limit = maxPauses;
        }

        @Override
        public void pause() throws InterruptedException {
            taken++;
            if (taken > limit + 3)
                throw new AssertionError("still pausing after " + taken + " pauses; the limit was " + limit);
            if (rig.sp.rdp(LINK + ".enabled") != null)
                enabledSeen = true;
            if (taken == releaseAt) {
                try {
                    rig.host.releaseHeld();
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
                rig.awaitPending(0);
            }
        }
    }

    // ---- test support: a real QMUX in this JVM, and a host on the far side of its queues ----

    private static final java.util.concurrent.atomic.AtomicInteger SPACES = new java.util.concurrent.atomic.AtomicInteger();

    static final class Rig {
        static final String READY = "acq-channel.ready";
        final org.jpos.space.LocalSpace<String, Object> sp;
        final org.jpos.q2.iso.QMUX mux;
        final Host host;

        @SuppressWarnings("unchecked")
        Rig() throws Exception {
            String space = "tspace:practice-" + SPACES.incrementAndGet();
            sp = (org.jpos.space.LocalSpace<String, Object>) org.jpos.space.SpaceFactory.getSpace(space);
            org.jdom2.Element e = new org.jdom2.Element("mux");
            e.addContent(new org.jdom2.Element("space").setText(space));
            e.addContent(new org.jdom2.Element("in").setText("acq-receive"));
            e.addContent(new org.jdom2.Element("out").setText("acq-send"));
            e.addContent(new org.jdom2.Element("ready").setText(READY));
            mux = new org.jpos.q2.iso.QMUX();
            mux.setName("acq-mux-" + space);
            mux.setPersist(e);
            mux.setConfiguration(new org.jpos.core.SimpleConfiguration());
            mux.init();
            mux.start();
            host = new Host(sp);
        }

        /** Waits until the QMUX counts exactly n requests as waiting (it counts just after sending). */
        void awaitPending(int n) {
            for (long i = 0; mux.getRXPending() != n; i++) {
                if (i > 2_000_000_000L)
                    throw new AssertionError("the QMUX never counted " + n + " pending request(s)");
                Thread.onSpinWait();
            }
        }
    }

    /** Answers (39=00) or holds each request, in script order; notes what it holds at a sign-off. */
    static final class Host implements org.jpos.space.SpaceListener<String, Object> {
        static final String ANSWER = "answer", HOLD = "hold";
        private final org.jpos.space.LocalSpace<String, Object> sp;
        private final java.util.Deque<String> script = new java.util.ArrayDeque<>();
        private final java.util.List<ISOMsg> held = new java.util.ArrayList<>();
        private final java.util.concurrent.Semaphore arrivals = new java.util.concurrent.Semaphore(0);
        private int signOffs;
        private int heldAtSignOff = -1;

        Host(org.jpos.space.LocalSpace<String, Object> sp) {
            this.sp = sp;
            sp.addListener("acq-send", this);
        }

        synchronized Host then(String... acts) {
            script.addAll(java.util.List.of(acts));
            return this;
        }

        synchronized int signOffs() {
            return signOffs;
        }

        synchronized int heldAtSignOff() {
            return heldAtSignOff;
        }

        void awaitArrivals(int n) throws InterruptedException {
            if (!arrivals.tryAcquire(n, 30, java.util.concurrent.TimeUnit.SECONDS))
                throw new AssertionError("the host did not receive " + n + " request(s)");
        }

        void releaseHeld() throws Exception {
            java.util.List<ISOMsg> now;
            synchronized (this) { now = new java.util.ArrayList<>(held); held.clear(); }
            for (ISOMsg m : now) respond(m);
        }

        @Override
        public void notify(String queue, Object value) {
            ISOMsg request = (ISOMsg) sp.inp(queue);
            if (request == null) return;
            String act;
            try {
                synchronized (this) {
                    if ("0800".equals(request.getMTI()) && "002".equals(request.getString(70))) {
                        signOffs++;
                        heldAtSignOff = held.size();
                        act = ANSWER;
                    } else {
                        act = script.isEmpty() ? ANSWER : script.poll();
                        if (HOLD.equals(act)) held.add(request);
                    }
                }
                if (ANSWER.equals(act)) respond(request);
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            } finally {
                arrivals.release();
            }
        }

        private void respond(ISOMsg request) throws Exception {
            ISOMsg response = (ISOMsg) request.clone();
            response.setResponseMTI();
            response.set(39, "00");
            sp.out("acq-receive", response);
        }
    }
}

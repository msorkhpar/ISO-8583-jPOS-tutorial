package com.example.practice;

import org.jpos.iso.IF_CHAR;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.packager.GenericPackager;
import org.jpos.iso.packager.ISO87APackager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubmitterTest {
    private static final String NAME_40 = "EXAMPLE BOOKSHOP         SPRINGFIELD  US";

    private static ISOMsg purchase(String terminal, String stan, String nameAndLocation) throws Exception {
        ISOMsg m = new ISOMsg("0200");
        m.set(2, "5555555555554444");
        m.set(3, "000000");
        m.set(4, "000000004875");
        m.set(7, "0214081522");
        m.set(11, stan);
        m.set(41, terminal);
        m.set(42, "000000007654321");
        m.set(43, nameAndLocation);
        m.set(49, "978");
        return m;
    }

    private static GenericPackager iso87ascii() throws Exception {
        return new GenericPackager("jar:packager/iso87ascii.xml");
    }

    @Test
    void aRequestThatFitsIsAnswered() throws Exception {
        Rig rig = new Rig(true);
        Submitter s = new Submitter(rig.mux, iso87ascii(), 2000);
        Submitter.Result r = s.submit(purchase("29110057", "004711", NAME_40));
        assertEquals(Submitter.Outcome.ANSWERED, r.outcome());
        assertNotNull(r.response(), "an answered request carries the host's response");
        assertEquals("0210", r.response().getMTI());
        assertEquals("00", r.response().getString(39));
        assertEquals(1, rig.host.received().size());
    }

    @Test
    void aRequestThatDoesNotFitNeverReachesTheHost() throws Exception {
        Rig rig = new Rig(true);
        Submitter s = new Submitter(rig.mux, iso87ascii(), 2000);
        Submitter.Result r = s.submit(purchase("29110057", "004712", NAME_40 + "A"));
        assertEquals(Submitter.Outcome.NOT_SENT, r.outcome());
        assertNotNull(r.reason(), "the caller is told what was wrong");
        assertTrue(rig.host.received().isEmpty(), "a request that cannot be packed must never be queued");
    }

    @Test
    void silenceIsNotTheSameAsNotSent() throws Exception {
        Rig rig = new Rig(true);
        rig.host.then(Host.DROP);
        Submitter s = new Submitter(rig.mux, iso87ascii(), 300);
        Submitter.Result r = s.submit(purchase("29110057", "004713", NAME_40));
        assertEquals(Submitter.Outcome.NO_ANSWER, r.outcome(),
                "the host received this one; only its answer is missing");
        assertEquals(1, rig.host.received().size());
    }

    @Test
    void anyFieldThatDoesNotFitIsCaught() throws Exception {
        Rig rig = new Rig(true);
        Submitter s = new Submitter(rig.mux, iso87ascii(), 2000);
        Submitter.Result r = s.submit(purchase("291100570", "004714", NAME_40));   // 9 characters in field 41
        assertEquals(Submitter.Outcome.NOT_SENT, r.outcome());
        assertTrue(rig.host.received().isEmpty());
    }

    @Test
    void theLinksOwnPackagerDecides() throws Exception {
        Rig rig = new Rig(true);
        ISO87APackager dialect = new ISO87APackager();                 // this network allows 25 characters in field 43
        dialect.setFieldPackager(43, new IF_CHAR(25, "CARD ACCEPTOR NAME/LOCATION"));
        Submitter s = new Submitter(rig.mux, dialect, 2000);
        Submitter.Result r = s.submit(purchase("29110058", "000318", "EXAMPLE BOOKSHOP SPRINGFIELD"));
        assertEquals(Submitter.Outcome.NOT_SENT, r.outcome(), "28 characters do not fit this network's field 43");
        assertTrue(rig.host.received().isEmpty());
        assertEquals(Submitter.Outcome.ANSWERED,
                s.submit(purchase("29110058", "000319", "EXAMPLE BOOKSHOP")).outcome());
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

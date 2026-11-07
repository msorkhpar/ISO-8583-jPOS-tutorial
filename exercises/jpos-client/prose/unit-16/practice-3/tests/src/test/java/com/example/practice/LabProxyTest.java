package com.example.practice;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOSource;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LabProxyTest {

    /** The tester's client connection: records what the proxy sends back. */
    static final class Client implements ISOSource {
        final List<ISOMsg> sent = new ArrayList<>();
        @Override public void send(ISOMsg m) { sent.add(m); }
        @Override public boolean isConnected() { return true; }
    }

    /** The lab's simulator: approves with its own approval code, and counts what it was asked. */
    static final class LabSimulator implements UnaryOperator<ISOMsg> {
        final AtomicInteger asked = new AtomicInteger();
        @Override public ISOMsg apply(ISOMsg request) {
            asked.incrementAndGet();
            try {
                ISOMsg r = (ISOMsg) request.clone();
                r.setResponseMTI();
                r.set(38, "LAB001");
                r.set(39, "00");
                return r;
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private static ISOMsg purchase(String stan) throws Exception {
        ISOMsg m = new ISOMsg("0200");
        m.set(2, "5555555555554444");
        m.set(3, "000000");
        m.set(4, "000000006325");
        m.set(11, stan);
        m.set(41, "29110004");
        return m;
    }

    @Test
    void anAnsweredRequestIsRelayed() throws Exception {
        Rig rig = new Rig(true);
        LabSimulator sim = new LabSimulator();
        Client client = new Client();
        assertTrue(new LabProxy(rig.mux, sim, 1000).process(client, purchase("000611")));
        assertEquals(1, rig.host.received().size(), "forwarded once");
        assertEquals(1, client.sent.size());
        assertEquals("0210", client.sent.get(0).getMTI());
        assertEquals("00", client.sent.get(0).getString(39));
        assertEquals("H40917", client.sent.get(0).getString(38), "the certification host's answer, not the lab's");
    }

    @Test
    void noAnswerFromTheCertificationHostMeansNoAnswer() throws Exception {
        Rig rig = new Rig(true);
        rig.host.then(Host.DROP);
        LabSimulator sim = new LabSimulator();
        Client client = new Client();
        assertTrue(new LabProxy(rig.mux, sim, 500).process(client, purchase("000621")));
        assertEquals(1, rig.host.received().size(), "the purchase reached the certification host");
        assertEquals(List.of(), client.sent, "nobody knows whether it was approved");
        assertEquals(0, sim.asked.get(), "the lab's simulator has no say in it");
    }

    @Test
    void outsideASlotTheSimulatorAnswersAndNothingIsForwarded() throws Exception {
        Rig rig = new Rig(false);
        LabSimulator sim = new LabSimulator();
        Client client = new Client();
        assertTrue(new LabProxy(rig.mux, sim, 500).process(client, purchase("000631")));
        assertEquals(0, rig.host.received().size(), "nothing went to the certification host");
        assertEquals(1, client.sent.size());
        assertEquals("LAB001", client.sent.get(0).getString(38));
    }

    @Test
    void theCertificationHostsAnswerReachesTheClientAsSent() throws Exception {
        Rig rig = new Rig(true);
        rig.host.then(Host.DECLINE);
        Client client = new Client();
        new LabProxy(rig.mux, new LabSimulator(), 1000).process(client, purchase("000641"));
        assertEquals(1, client.sent.size());
        ISOMsg r = client.sent.get(0);
        assertEquals("05", r.getString(39));
        assertEquals("5555555555554444", r.getString(2), "the client gets the host's answer as the host sent it");
        assertNull(r.getString(38));
    }

    @Test
    void aLinkThatDropsWhileWaitingStillMeansNoAnswer() throws Exception {
        Rig rig = new Rig(true);
        rig.host.then(Host.HOLD);
        LabSimulator sim = new LabSimulator();
        Client client = new Client();
        LabProxy proxy = new LabProxy(rig.mux, sim, 1000);
        Thread session = new Thread(() -> proxy.process(client, purchaseOrFail("000651")));
        session.start();
        rig.host.awaitArrivals(1);
        rig.sp.inp("host.ready");                       // the slot ends while the purchase waits
        session.join(15000);
        assertFalse(session.isAlive());
        assertEquals(List.of(), client.sent, "the purchase reached the certification host");
        assertEquals(0, sim.asked.get());
    }

    @Test
    void aRepeatWhileTheFirstWaitsIsNotAnsweredByTheLab() throws Exception {
        Rig rig = new Rig(true);
        rig.host.then(Host.HOLD);
        LabSimulator sim = new LabSimulator();
        Client client = new Client();
        LabProxy proxy = new LabProxy(rig.mux, sim, 5000);
        Thread session = new Thread(() -> proxy.process(client, purchaseOrFail("000661")));
        session.start();
        rig.host.awaitArrivals(1);
        proxy.process(client, purchase("000661"));      // the client repeats it on a second connection
        rig.host.releaseHeld();
        session.join(15000);
        assertFalse(session.isAlive());
        assertEquals(0, sim.asked.get(), "the certification host has this purchase");
        assertEquals(1, client.sent.size());
        assertEquals("H40917", client.sent.get(0).getString(38));
    }

    private static ISOMsg purchaseOrFail(String stan) {
        try {
            return purchase(stan);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
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
            if ("00".equals(rc))
                response.set(38, "H40917");             // the certification host's approval code
            response.set(39, rc);
            sp.out("host-receive", response);
        }
    }
}

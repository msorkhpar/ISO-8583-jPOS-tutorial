package com.example.practice;

import com.example.practice.PaymentClient.Outcome;
import org.jpos.iso.ISOMsg;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class PaymentClientTest {

    private static ISOMsg message(String mti, String stan) throws Exception {
        ISOMsg m = new ISOMsg(mti);
        if (!mti.startsWith("08")) {
            m.set(2, "4111111111111111");
            m.set(3, "000000");
            m.set(4, "000000005000");
            m.set(32, "123456");
        } else {
            m.set(70, "301");
        }
        m.set(7, "1105143000");
        m.set(11, stan);
        m.set(41, "29110001");
        return m;
    }

    private static ISOMsg queued(Rig rig) {
        return (ISOMsg) rig.sp.rdp(PaymentClient.SAF);
    }

    @Test
    void aQuickApprovalIsApproved() throws Exception {
        Rig rig = new Rig(true);
        PaymentClient client = new PaymentClient(rig.mux, rig.sp, 1000);
        assertEquals(Outcome.APPROVED, client.send(message("0200", "000125")));
        assertNull(queued(rig), "nothing is left to send");
    }

    @Test
    void aDeclineIsADecline() throws Exception {
        Rig rig = new Rig(true);
        rig.host.then(Host.DECLINE);
        PaymentClient client = new PaymentClient(rig.mux, rig.sp, 1000);
        assertEquals(Outcome.DECLINED, client.send(message("0200", "000125")));
        assertNull(queued(rig), "a declined purchase moved no money: nothing to undo");
    }

    @Test
    void aPurchaseWithNoAnswerIsReversedNotSentAgain() throws Exception {
        Rig rig = new Rig(true);
        rig.host.then(Host.DROP);
        PaymentClient client = new PaymentClient(rig.mux, rig.sp, 1000);
        assertEquals(Outcome.REVERSED, client.send(message("0200", "000125")));
        assertEquals(1, rig.host.received().size(), "the purchase went to the host exactly once");
        ISOMsg reversal = queued(rig);
        assertNotNull(reversal, "the reversal waits in the SAF queue");
        assertEquals("0420", reversal.getMTI());
    }

    @Test
    void theReversalPointsAtThePurchase() throws Exception {
        Rig rig = new Rig(true);
        rig.host.then(Host.DROP);
        PaymentClient client = new PaymentClient(rig.mux, rig.sp, 1000);
        client.send(message("0100", "000777"));
        ISOMsg reversal = queued(rig);
        assertNotNull(reversal);
        assertEquals("0100" + "000777" + "1105143000" + "00000123456" + "00000000000", reversal.getString(90));
        assertNotEquals("000777", reversal.getString(11), "the reversal is a new message with its own STAN");
    }

    @Test
    void anAdviceWithNoAnswerIsQueuedAsItIs() throws Exception {
        Rig rig = new Rig(true);
        rig.host.then(Host.DROP);
        PaymentClient client = new PaymentClient(rig.mux, rig.sp, 1000);
        assertEquals(Outcome.QUEUED, client.send(message("0220", "000126")));
        ISOMsg advice = queued(rig);
        assertNotNull(advice, "the advice still has to reach the host");
        assertEquals("0220", advice.getMTI(), "an advice is repeated, not reversed");
        assertEquals("000126", advice.getString(11));
        assertEquals(1, rig.host.received().size());
    }

    @Test
    void anEchoWithNoAnswerNeedsNothing() throws Exception {
        Rig rig = new Rig(true);
        rig.host.then(Host.DROP);
        PaymentClient client = new PaymentClient(rig.mux, rig.sp, 1000);
        assertEquals(Outcome.NO_ANSWER, client.send(message("0800", "000127")));
        assertNull(queued(rig), "an echo moved no money");
        assertEquals(1, rig.host.received().size());
    }

    @Test
    void aPurchaseOnADownLinkIsNeitherSentNorReversed() throws Exception {
        Rig rig = new Rig(false);
        PaymentClient client = new PaymentClient(rig.mux, rig.sp, 1000);
        assertEquals(Outcome.NOT_SENT, client.send(message("0200", "000128")));
        assertEquals(0, rig.host.received().size(), "nothing went out");
        assertNull(queued(rig), "what never went out needs no reversal");
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

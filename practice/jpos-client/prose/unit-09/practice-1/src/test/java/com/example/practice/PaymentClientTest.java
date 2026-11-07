package com.example.practice;

import com.example.practice.PaymentClient.Outcome;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.packager.GenericPackager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class PaymentClientTest {

    private static ISOMsg purchase(String stan) throws Exception {
        ISOMsg m = new ISOMsg("0200");
        m.set(2, "5555555555554444");
        m.set(3, "000000");
        m.set(4, "000000003175");
        m.set(11, stan);
        m.set(37, "631205000431");
        m.set(41, "29110077");
        m.set(43, "NORTHSIDE HARDWARE AND GARDEN SUPPLY CO.");   // exactly 40
        return m;
    }

    private static PaymentClient client(Rig rig) throws Exception {
        return new PaymentClient(rig.mux, rig.sp, new GenericPackager("jar:packager/iso87ascii.xml"), 300);
    }

    private static ISOMsg queued(Rig rig) {
        return (ISOMsg) rig.sp.rdp(PaymentClient.SAF);
    }

    @Test
    void aGoodPurchaseIsApproved() throws Exception {
        Rig rig = new Rig(true);
        assertEquals(Outcome.APPROVED, client(rig).pay(purchase("000431")));
        assertEquals(1, rig.host.received().size());
        assertNull(queued(rig));
    }

    @Test
    void aPurchaseTheHostNeverAnswersIsLeftForReversal() throws Exception {
        Rig rig = new Rig(true);
        rig.host.then(Host.DROP);
        assertEquals(Outcome.UNKNOWN, client(rig).pay(purchase("000432")));
        assertNotNull(queued(rig));
    }

    @Test
    void aFieldTooLongForTheLinkNeverReachesIt() throws Exception {
        Rig rig = new Rig(true);
        ISOMsg m = purchase("000433");
        m.set(37, "6312050004330");                      // 13 characters; the link carries 12
        assertEquals(Outcome.REFUSED, client(rig).pay(m));
        assertEquals(0, rig.host.received().size(), "nothing may reach the link");
    }

    @Test
    void anAmountThatIsNotANumberNeverReachesIt() throws Exception {
        Rig rig = new Rig(true);
        ISOMsg m = purchase("000434");
        m.set(4, "31.75");
        assertEquals(Outcome.REFUSED, client(rig).pay(m));
        assertEquals(0, rig.host.received().size(), "the host would read this as another amount");
    }

    @Test
    void aRefusedMessageIsNotLeftForReversal() throws Exception {
        Rig rig = new Rig(true);
        ISOMsg m = purchase("000435");
        m.set(41, "290110077");                          // 9 characters; the link carries 8
        assertEquals(Outcome.REFUSED, client(rig).pay(m));
        assertNull(queued(rig), "the host never saw it: there is nothing to reverse");
    }

    @Test
    void aMessageWithoutAnAmountIsCheckedToo() throws Exception {
        Rig rig = new Rig(true);
        ISOMsg m = purchase("000436");
        m.setMTI("0100");
        m.set(3, "310000");                              // a balance inquiry carries no amount
        m.unset(4);
        assertEquals(Outcome.APPROVED, client(rig).pay(m));
        assertEquals(1, rig.host.received().size());
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

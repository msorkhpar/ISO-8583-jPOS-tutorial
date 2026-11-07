package com.example.practice;

import com.example.practice.PaymentClient.Category;
import com.example.practice.PaymentClient.Outcome;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.packager.GenericPackager;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class PaymentClientTest {

    /** This network's list. */
    private static final Map<String, Category> CODES = Map.ofEntries(
            Map.entry("00", Category.APPROVAL), Map.entry("11", Category.APPROVAL),
            Map.entry("05", Category.DECLINE), Map.entry("57", Category.DECLINE),
            Map.entry("61", Category.DECLINE),
            Map.entry("91", Category.UNAVAILABLE), Map.entry("96", Category.UNAVAILABLE),
            Map.entry("12", Category.FORMAT_ERROR), Map.entry("13", Category.FORMAT_ERROR),
            Map.entry("30", Category.FORMAT_ERROR));

    private static ISOMsg purchase(String stan, String amount) throws Exception {
        ISOMsg m = new ISOMsg("0200");
        m.set(2, "5555555555554444");
        m.set(3, "000000");
        m.set(4, amount);
        m.set(11, stan);
        m.set(41, "29110063");
        return m;
    }

    private static PaymentClient client(Rig rig) throws Exception {
        return new PaymentClient(rig.mux, rig.sp, new GenericPackager("jar:packager/iso87ascii.xml"), 1000, CODES);
    }

    private static ISOMsg queued(Rig rig, String queue) {
        return (ISOMsg) rig.sp.rdp(queue);
    }

    @Test
    void anApprovalAndADeclineAreReportedAsSuch() throws Exception {
        Rig rig = new Rig(true);
        rig.host.then("rc:00", "rc:61");
        assertEquals(Outcome.APPROVED, client(rig).pay(purchase("000551", "000000012000")));
        assertEquals(Outcome.DECLINED, client(rig).pay(purchase("000552", "000000250000")));
        assertNull(queued(rig, PaymentClient.SAF), "the host decided both: nothing to reverse");
    }

    @Test
    void anApprovalIsNotOnlyZeroZero() throws Exception {
        Rig rig = new Rig(true);
        rig.host.then("rc:11");
        assertEquals(Outcome.APPROVED, client(rig).pay(purchase("000553", "000000012000")));
    }

    @Test
    void anUnavailableIssuerIsNotADecline() throws Exception {
        Rig rig = new Rig(true);
        rig.host.then("rc:96");
        assertEquals(Outcome.TRY_LATER, client(rig).pay(purchase("000554", "000000012000")));
    }

    @Test
    void tryLaterDoesNotSendTheSameMessageAgain() throws Exception {
        Rig rig = new Rig(true);
        rig.host.then("rc:91", "rc:00");
        assertEquals(Outcome.TRY_LATER, client(rig).pay(purchase("000555", "000000012000")));
        assertEquals(1, rig.host.received().size(), "a new attempt is the cardholder's, as a new transaction");
    }

    @Test
    void aFormatErrorReachesTheDevelopers() throws Exception {
        Rig rig = new Rig(true);
        rig.host.then("rc:13");
        assertEquals(Outcome.REJECTED, client(rig).pay(purchase("000556", "000000012000")));
        ISOMsg alert = queued(rig, PaymentClient.ALERTS);
        assertNotNull(alert, "somebody must hear that the requests are wrong");
        assertEquals("000556", alert.getString(11));
    }

    @Test
    void anAnswerWithoutACodeIsLeftForReversal() throws Exception {
        Rig rig = new Rig(true);
        rig.host.then("rc:");
        assertEquals(Outcome.UNKNOWN, client(rig).pay(purchase("000557", "000000012000")));
        ISOMsg left = queued(rig, PaymentClient.SAF);
        assertNotNull(left, "nobody knows what the host did");
        assertEquals("000557", left.getString(11));
    }

    @Test
    void aCodeNotOnTheListIsLeftForReversal() throws Exception {
        Rig rig = new Rig(true);
        rig.host.then("rc:Q4");
        assertEquals(Outcome.UNKNOWN, client(rig).pay(purchase("000558", "000000012000")));
        assertNotNull(queued(rig, PaymentClient.SAF), "nobody knows what the host did");
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

    /** Answers (39=00), declines (39=05), answers "rc:NN" with 39=NN ("rc:" with none), drops or holds each request, in script order. */
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
                else if (act.startsWith("rc:")) respond(request, act.substring(3));
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            } finally {
                arrivals.release();
            }
        }

        private void respond(ISOMsg request, String rc) throws Exception {
            ISOMsg response = (ISOMsg) request.clone();
            response.setResponseMTI();
            if (rc.isEmpty()) response.unset(39); else response.set(39, rc);
            sp.out("host-receive", response);
        }
    }
}

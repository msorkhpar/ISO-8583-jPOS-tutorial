package com.example.practice;

import org.jpos.iso.ISOMsg;
import org.jpos.q2.iso.MUXPool;
import org.jpos.space.LocalSpace;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class CheckoutTest {

    private static ISOMsg purchase(String terminal, String stan, String amount) throws Exception {
        ISOMsg m = new ISOMsg("0200");
        m.set(3, "000000");
        m.set(4, amount);
        m.set(7, "0314093015");
        m.set(11, stan);
        m.set(41, terminal);
        return m;
    }

    @Test
    void answeredPurchasesAreApprovedOrDeclined() throws Exception {
        Rig rig = new Rig();
        rig.up(0);
        rig.up(1);
        rig.hosts[1].then(Host.ANSWER);
        rig.hosts[0].then(Host.DECLINE);
        Checkout checkout = new Checkout(rig.pool, rig.sp, 2000);
        assertEquals(Checkout.Outcome.APPROVED, checkout.pay(purchase("31400007", "004410", "000000001999")));
        assertEquals(Checkout.Outcome.DECLINED, checkout.pay(purchase("31400007", "004411", "000000012000")));
        assertNull(rig.sp.rdp(Checkout.TO_REVERSE), "an answered purchase leaves nothing to reverse");
    }

    @Test
    void aPurchaseLeftUnansweredGoesToNoOtherLink() throws Exception {
        Rig rig = new Rig();
        rig.up(0);
        rig.up(1);
        rig.hosts[1].then(Host.DROP);
        Checkout checkout = new Checkout(rig.pool, rig.sp, 300);
        assertEquals(Checkout.Outcome.UNKNOWN, checkout.pay(purchase("31400007", "004420", "000000004575")));
        assertEquals(1, rig.received(), "the purchase went out once, on one link");
    }

    @Test
    void aPurchaseLeftUnansweredIsQueuedForReversal() throws Exception {
        Rig rig = new Rig();
        rig.up(0);
        rig.up(1);
        rig.hosts[1].then(Host.DROP);
        Checkout checkout = new Checkout(rig.pool, rig.sp, 300);
        checkout.pay(purchase("31400007", "004430", "000000004575"));
        ISOMsg queued = (ISOMsg) rig.sp.inp(Checkout.TO_REVERSE);
        assertNotNull(queued, "the host may have approved it");
        assertEquals("004430", queued.getString(11));
        assertNull(rig.sp.rdp(Checkout.TO_REVERSE));
    }

    @Test
    void withEveryLinkDownNothingIsSentOrReversed() throws Exception {
        Rig rig = new Rig();
        Checkout checkout = new Checkout(rig.pool, rig.sp, 300);
        assertEquals(Checkout.Outcome.UNAVAILABLE, checkout.pay(purchase("31400007", "004440", "000000000850")));
        assertEquals(0, rig.received());
        assertNull(rig.sp.rdp(Checkout.TO_REVERSE), "nothing went out, so there is nothing to undo");
    }

    // ---- test support: two real QMUXes, a host behind each, and a real round-robin MUXPool ----

    private static final java.util.concurrent.atomic.AtomicInteger SPACES = new java.util.concurrent.atomic.AtomicInteger();

    static final class Rig {
        final LocalSpace<String, Object> sp;
        final Host[] hosts = new Host[2];
        final MUXPool pool;

        @SuppressWarnings("unchecked")
        Rig() throws Exception {
            int n = SPACES.incrementAndGet();
            String space = "tspace:checkout-" + n;
            sp = (LocalSpace<String, Object>) org.jpos.space.SpaceFactory.getSpace(space);
            StringBuilder names = new StringBuilder();
            for (int i = 0; i < 2; i++) {
                String name = "store-mux-" + n + "-" + (i + 1);
                org.jdom2.Element e = new org.jdom2.Element("mux");
                e.addContent(new org.jdom2.Element("space").setText(space));
                e.addContent(new org.jdom2.Element("in").setText("link-" + (i + 1) + "-receive"));
                e.addContent(new org.jdom2.Element("out").setText("link-" + (i + 1) + "-send"));
                e.addContent(new org.jdom2.Element("ready").setText("link-" + (i + 1) + ".ready"));
                org.jpos.q2.iso.QMUX q = new org.jpos.q2.iso.QMUX();
                q.setName(name);
                q.setPersist(e);
                q.setConfiguration(new org.jpos.core.SimpleConfiguration());
                q.init();
                q.start();
                hosts[i] = new Host(sp, "link-" + (i + 1) + "-send", "link-" + (i + 1) + "-receive");
                names.append(name).append(' ');
            }
            org.jdom2.Element p = new org.jdom2.Element("mux");
            p.addContent(new org.jdom2.Element("muxes").setText(names.toString().trim()));
            p.addContent(new org.jdom2.Element("strategy").setText("round-robin"));
            p.addContent(new org.jdom2.Element("space").setText(space));
            pool = new MUXPool();
            pool.setName("store-pool-" + n);
            pool.setPersist(p);
            pool.setConfiguration(new org.jpos.core.SimpleConfiguration());
            pool.init();
            pool.start();
        }

        void up(int i) { sp.put("link-" + (i + 1) + ".ready", new java.util.Date()); }

        int received() { return hosts[0].received().size() + hosts[1].received().size(); }
    }

    /** Answers (39=00), declines (39=05) or drops each request, in script order; then answers. */
    static final class Host implements org.jpos.space.SpaceListener<String, Object> {
        static final String ANSWER = "answer", DECLINE = "decline", DROP = "drop";
        private final LocalSpace<String, Object> sp;
        private final String in;
        private final java.util.Deque<String> script = new java.util.ArrayDeque<>();
        private final java.util.List<ISOMsg> received = new java.util.ArrayList<>();

        Host(LocalSpace<String, Object> sp, String out, String in) {
            this.sp = sp;
            this.in = in;
            sp.addListener(out, this);
        }

        synchronized Host then(String... acts) {
            script.addAll(java.util.List.of(acts));
            return this;
        }

        synchronized java.util.List<ISOMsg> received() {
            return new java.util.ArrayList<>(received);
        }

        @Override
        public void notify(String queue, Object value) {
            ISOMsg request = (ISOMsg) sp.inp(queue);
            if (request == null) return;
            String act;
            synchronized (this) {
                received.add(request);
                act = script.isEmpty() ? ANSWER : script.poll();
            }
            if (DROP.equals(act)) return;
            try {
                ISOMsg response = (ISOMsg) request.clone();
                response.setResponseMTI();
                response.set(39, DECLINE.equals(act) ? "05" : "00");
                sp.out(in, response);
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        }
    }
}

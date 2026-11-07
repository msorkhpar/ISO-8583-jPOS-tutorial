package com.example.practice;

import org.jpos.iso.ISOMsg;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaymentServiceTest {

    private static ISOMsg purchase(String stan) throws Exception {
        ISOMsg m = new ISOMsg("0200");
        m.set(2, "4111111111111111");
        m.set(3, "000000");
        m.set(4, "000000006120");
        m.set(11, stan);
        m.set(41, "29110018");
        return m;
    }

    @Test
    void aDeployedMuxCarriesThePurchase() throws Exception {
        Link link = new Link();
        String name = Link.uniqueName("acq");
        link.deploy(name);
        PaymentService service = new PaymentService(name, 1000);
        assertEquals(PaymentService.Result.APPROVED, service.pay(purchase("002201")));
        assertEquals(1, link.host.received().size());
    }

    @Test
    void theServiceStartsBeforeTheMuxIsDeployed() throws Exception {
        Link link = new Link();
        String name = Link.uniqueName("acq");
        PaymentService service = new PaymentService(name, 1000);
        assertEquals(PaymentService.Result.UNAVAILABLE, service.pay(purchase("002202")),
                "nothing is deployed yet");
        link.deploy(name);
        assertEquals(PaymentService.Result.APPROVED, service.pay(purchase("002203")));
    }

    @Test
    void anOperatorRedeploysTheMux() throws Exception {
        Link link = new Link();
        String name = Link.uniqueName("acq");
        link.deploy(name);
        PaymentService service = new PaymentService(name, 1000);
        assertEquals(PaymentService.Result.APPROVED, service.pay(purchase("002204")));
        link.redeploy(name);
        assertEquals(PaymentService.Result.APPROVED, service.pay(purchase("002205")),
                "the descriptor was edited and Q2 replaced the QMUX");
        assertEquals(2, link.host.received().size());
    }

    @Test
    void theNameNowBelongsToAPool() throws Exception {
        Link link = new Link();
        String member = Link.uniqueName("acq-a");
        String pool = Link.uniqueName("acq-pool");
        link.deploy(member);
        link.deployPool(pool, member);
        PaymentService service = new PaymentService(pool, 1000);
        assertEquals(PaymentService.Result.APPROVED, service.pay(purchase("002206")));
    }

    // ---- test support: QMUXes and a MUXPool registered by name, and a host behind their queues ----

    private static final java.util.concurrent.atomic.AtomicInteger NAMES = new java.util.concurrent.atomic.AtomicInteger();

    /** One link: a space with the adaptor's queues, a host on the far side, and the link up. */
    static final class Link {
        final String space = uniqueName("tspace:practice");
        final org.jpos.space.LocalSpace<String, Object> sp;
        final Host host;
        private final java.util.Map<String, org.jpos.q2.iso.QMUX> deployed = new java.util.HashMap<>();

        Link() {
            sp = (org.jpos.space.LocalSpace<String, Object>) org.jpos.space.SpaceFactory.getSpace(space);
            sp.out("host.ready", Boolean.TRUE);
            host = new Host(sp);
        }

        static String uniqueName(String prefix) {
            return prefix + "-" + NAMES.incrementAndGet();
        }

        /** What Q2 does with a QMUX descriptor: create, configure, register under the name, start. */
        void deploy(String name) throws Exception {
            org.jdom2.Element e = new org.jdom2.Element("mux");
            e.addContent(new org.jdom2.Element("space").setText(space));
            e.addContent(new org.jdom2.Element("in").setText("host-receive"));
            e.addContent(new org.jdom2.Element("out").setText("host-send"));
            e.addContent(new org.jdom2.Element("ready").setText("host.ready"));
            e.addContent(new org.jdom2.Element("unhandled").setText("host-unhandled"));
            org.jpos.q2.iso.QMUX mux = new org.jpos.q2.iso.QMUX();
            mux.setName(name);
            mux.setPersist(e);
            mux.setConfiguration(new org.jpos.core.SimpleConfiguration());
            mux.init();
            mux.start();
            deployed.put(name, mux);
        }

        /** What Q2 does when the descriptor changes: the old QMUX is stopped and destroyed, a new one deployed. */
        void redeploy(String name) throws Exception {
            org.jpos.q2.iso.QMUX old = deployed.remove(name);
            old.stop();
            old.destroy();
            deploy(name);
        }

        void deployPool(String name, String... members) throws Exception {
            org.jdom2.Element e = new org.jdom2.Element("mux");
            e.addContent(new org.jdom2.Element("muxes").setText(String.join(" ", members)));
            e.addContent(new org.jdom2.Element("strategy").setText("primary-secondary"));
            org.jpos.q2.iso.MUXPool pool = new org.jpos.q2.iso.MUXPool();
            pool.setName(name);
            pool.setPersist(e);
            pool.setConfiguration(new org.jpos.core.SimpleConfiguration());
            pool.init();
            pool.start();
        }
    }

    /** Approves every request it takes from host-send. */
    static final class Host implements org.jpos.space.SpaceListener<String, Object> {
        private final org.jpos.space.LocalSpace<String, Object> sp;
        private final java.util.List<ISOMsg> received = new java.util.ArrayList<>();

        Host(org.jpos.space.LocalSpace<String, Object> sp) {
            this.sp = sp;
            sp.addListener("host-send", this);
        }

        synchronized java.util.List<ISOMsg> received() {
            return new java.util.ArrayList<>(received);
        }

        @Override
        public void notify(String queue, Object value) {
            ISOMsg request = (ISOMsg) sp.inp(queue);
            if (request == null) return;
            synchronized (this) { received.add(request); }
            try {
                ISOMsg response = (ISOMsg) request.clone();
                response.setResponseMTI();
                response.set(39, "00");
                sp.out("host-receive", response);
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        }
    }
}

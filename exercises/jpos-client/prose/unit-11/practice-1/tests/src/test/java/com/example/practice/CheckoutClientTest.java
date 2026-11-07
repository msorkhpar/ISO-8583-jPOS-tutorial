package com.example.practice;

import org.jpos.iso.ISOMsg;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static com.example.practice.CheckoutClient.Outcome.APPROVED;
import static com.example.practice.CheckoutClient.Outcome.HOST_UNAVAILABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CheckoutClientTest {

    private static ISOMsg purchase(String stan) throws Exception {
        ISOMsg m = new ISOMsg("0200");
        m.set(3, "000000");
        m.set(4, "000000001875");
        m.set(11, stan);
        m.set(41, "29110003");
        return m;
    }

    private static CheckoutClient client(Rig rig) {
        return new CheckoutClient(rig.mux, rig.sp, 30000);
    }

    @Test
    void aSignedOnLinkCarriesThePurchase() throws Exception {
        Rig rig = new Rig();
        Date connection = new Date(1_762_400_000_000L);
        rig.sp.out(Rig.READY, connection);
        rig.sp.put(rig.enabledKey(), connection);
        assertEquals(APPROVED, client(rig).authorize(purchase("004211")));
        assertEquals(1, rig.host.received().size());
    }

    @Test
    void aFreshConnectionIsNotSignedOn() throws Exception {
        Rig rig = new Rig();
        rig.sp.out(Rig.READY, new Date(1_762_400_000_000L));
        assertEquals(HOST_UNAVAILABLE, client(rig).authorize(purchase("004212")));
        assertEquals(0, rig.host.received().size(), "the host never sees the purchase");
    }

    @Test
    void aMarkLeftByThePreviousConnectionDoesNotCount() throws Exception {
        Rig rig = new Rig();
        rig.sp.put(rig.enabledKey(), new Date(1_762_400_000_000L));   // signed on last night
        rig.sp.out(Rig.READY, new Date(1_762_410_500_000L));          // reconnected since
        assertEquals(HOST_UNAVAILABLE, client(rig).authorize(purchase("004213")));
        assertEquals(0, rig.host.received().size());
    }

    @Test
    void aReconnectInTheSameMillisecondIsStillANewConnection() throws Exception {
        Rig rig = new Rig();
        long t = 1_762_410_500_123L;
        rig.sp.put(rig.enabledKey(), new Date(t));
        rig.sp.out(Rig.READY, new Date(t));
        assertEquals(HOST_UNAVAILABLE, client(rig).authorize(purchase("004214")));
        assertEquals(0, rig.host.received().size());
    }

    @Test
    void aLinkThatIsDownIsUnavailableAtOnce() throws Exception {
        Rig rig = new Rig();
        assertEquals(HOST_UNAVAILABLE, client(rig).authorize(purchase("004215")));
        assertEquals(0, rig.host.received().size());
    }

    // ---- test support: a real QMUX in this JVM, and a host on the far side of its queues ----

    private static final java.util.concurrent.atomic.AtomicInteger SPACES = new java.util.concurrent.atomic.AtomicInteger();

    static final class Rig {
        static final String READY = "host-channel.ready";
        final org.jpos.space.LocalSpace<String, Object> sp;
        final org.jpos.q2.iso.QMUX mux;
        final Host host;

        @SuppressWarnings("unchecked")
        Rig() throws Exception {
            String space = "tspace:practice-" + SPACES.incrementAndGet();
            sp = (org.jpos.space.LocalSpace<String, Object>) org.jpos.space.SpaceFactory.getSpace(space);
            org.jdom2.Element e = new org.jdom2.Element("mux");
            e.addContent(new org.jdom2.Element("space").setText(space));
            e.addContent(new org.jdom2.Element("in").setText("host-receive"));
            e.addContent(new org.jdom2.Element("out").setText("host-send"));
            e.addContent(new org.jdom2.Element("ready").setText(READY));
            e.addContent(new org.jdom2.Element("unhandled").setText("host-unhandled"));
            mux = new org.jpos.q2.iso.QMUX();
            mux.setName("host-mux-" + SPACES.get());
            mux.setPersist(e);
            mux.setConfiguration(new org.jpos.core.SimpleConfiguration());
            mux.init();
            mux.start();
            host = new Host(sp);
        }

        String enabledKey() {
            return mux.getName() + ".enabled";
        }
    }

    /** Approves every request it receives. */
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

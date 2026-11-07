package com.example.practice;

import org.jdom2.Element;
import org.jpos.core.SimpleConfiguration;
import org.jpos.iso.ISOMsg;
import org.jpos.q2.iso.QMUX;
import org.jpos.space.LocalSpace;
import org.jpos.space.SpaceFactory;
import org.jpos.space.SpaceListener;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LinkDescriptorsTest {

    private static Element visa() {
        return LinkDescriptors.channelAdaptor("visa-primary", "to-visa", "from-visa", "127.0.0.1", 9100);
    }

    private static Element mastercard() {
        return LinkDescriptors.channelAdaptor("mc-primary", "to-mc", "from-mc", "127.0.0.1", 9200);
    }

    private static ISOMsg echo(String stan) throws Exception {
        ISOMsg m = new ISOMsg("0800");
        m.set(11, stan);
        m.set(70, "301");
        return m;
    }

    @Test
    void aRequestThroughTheNewMuxIsAnswered() throws Exception {
        Rig rig = new Rig();
        Element adaptor = visa();
        Element mux = LinkDescriptors.muxFor(adaptor, "visa-mux");
        assertEquals("org.jpos.q2.iso.QMUX", mux.getAttributeValue("class"), "Q2 needs the class to create");
        assertEquals("visa-mux", mux.getAttributeValue("name"));
        QMUX q = rig.deploy(mux);
        rig.adaptor(adaptor).up();
        ISOMsg r = q.request(echo("004417"), 5000);
        assertNotNull(r, "the host answered at once, but the answer never reached the waiting request");
        assertEquals("0810", r.getMTI());
    }

    @Test
    void theMuxSaysNotConnectedWhileItsLinkIsDown() throws Exception {
        Rig rig = new Rig();
        Element adaptor = visa();
        QMUX q = rig.deploy(LinkDescriptors.muxFor(adaptor, "visa-mux"));
        rig.adaptor(adaptor);                                     // never connects
        assertFalse(q.isConnected(), "the link to the host is down");
    }

    @Test
    void theMuxSaysConnectedOnlyWhileItsOwnLinkIsUp() throws Exception {
        Rig rig = new Rig();
        Element adaptor = mastercard();
        QMUX q = rig.deploy(LinkDescriptors.muxFor(adaptor, "mc-mux"));
        Adaptor mc = rig.adaptor(adaptor).up();
        assertTrue(q.isConnected(), "the link to the host is up");
        Adaptor other = rig.adaptor(visa()).up();
        mc.down();
        assertFalse(q.isConnected(), "only another link is up");
        other.down();
    }

    @Test
    void aMessageNobodyWaitsForStaysWithItsLink() throws Exception {
        Rig rig = new Rig();
        Element visa = visa(), mc = mastercard();
        QMUX visaMux = rig.deploy(LinkDescriptors.muxFor(visa, "visa-mux"));
        QMUX mcMux = rig.deploy(LinkDescriptors.muxFor(mc, "mc-mux"));
        rig.adaptor(visa).up();
        rig.adaptor(mc).up();
        String queue = visaMux.getUnhandledQueue();
        assertNotNull(queue, "a message nobody waits for must be kept somewhere an operator can read it");
        assertNotEquals(mcMux.getUnhandledQueue(), queue, "each link keeps its own");
        ISOMsg fromHost = new ISOMsg("0800");                     // the host asks something on its own
        fromHost.set(11, "880021");
        fromHost.set(70, "001");
        rig.sp.out(visa.getChildText("out"), fromHost);
        Object kept = rig.sp.rd(queue, 5000);
        assertNotNull(kept, "the host's message was not kept in " + queue);
        assertEquals("880021", ((ISOMsg) kept).getString(11));
    }

    // ---- test support: real QMUXes in this JVM, and a stand-in for each ChannelAdaptor ----

    private static final AtomicInteger SPACES = new AtomicInteger();

    static final class Rig {
        final String space = "tspace:link-" + SPACES.incrementAndGet();
        @SuppressWarnings("unchecked")
        final LocalSpace<String, Object> sp = (LocalSpace<String, Object>) SpaceFactory.getSpace(space);

        /** Deploys a QMUX from the descriptor, as Q2 would, in this test's own space. */
        QMUX deploy(Element descriptor) throws Exception {
            Element e = descriptor.clone();
            e.removeChildren("space");
            e.addContent(new Element("space").setText(space));
            QMUX q = new QMUX();
            q.setName(e.getAttributeValue("name") + "-" + space);
            q.setPersist(e);
            q.setConfiguration(new SimpleConfiguration());
            q.init();
            q.start();
            return q;
        }

        /** What the ChannelAdaptor would do with its queues; the host answers every request with 39=00. */
        Adaptor adaptor(Element descriptor) {
            return new Adaptor(sp, descriptor.getAttributeValue("name"),
                    descriptor.getChildText("in"), descriptor.getChildText("out"));
        }
    }

    static final class Adaptor implements SpaceListener<String, Object> {
        private final LocalSpace<String, Object> sp;
        private final String ready, in, out;

        Adaptor(LocalSpace<String, Object> sp, String name, String in, String out) {
            this.sp = sp;
            this.ready = name + ".ready";
            this.in = in;
            this.out = out;
            sp.addListener(in, this);
        }

        Adaptor up() {
            sp.out(ready, new java.util.Date());
            Object pending;
            while ((pending = sp.inp(in)) != null)
                answer((ISOMsg) pending);
            return this;
        }

        void down() {
            while (sp.inp(ready) != null) { }
        }

        @Override
        public void notify(String queue, Object value) {
            if (sp.rdp(ready) == null)
                return;                                           // not connected: the message waits
            ISOMsg request = (ISOMsg) sp.inp(queue);
            if (request != null)
                answer(request);
        }

        private void answer(ISOMsg request) {
            try {
                ISOMsg response = (ISOMsg) request.clone();
                response.setResponseMTI();
                response.set(39, "00");
                sp.out(out, response);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }
}

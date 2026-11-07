package com.example.practice;

import com.example.practice.LinkStatus.Status;
import org.jdom2.Element;
import org.jpos.core.SimpleConfiguration;
import org.jpos.iso.ISOMsg;
import org.jpos.q2.iso.QMUX;
import org.jpos.space.LocalSpace;
import org.jpos.space.SpaceFactory;
import org.jpos.space.SpaceListener;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LinkStatusTest {

    @Test
    void anAnsweredEchoIsUp() throws Exception {
        Link link = new Link("acquirer-b");
        link.deploy();
        link.up();
        assertEquals(Status.UP, new LinkStatus(link.muxName, 5000).check());
        List<ISOMsg> seen = link.received();
        assertEquals(1, seen.size(), "one check sends one echo");
        assertEquals("0800", seen.get(0).getMTI());
        assertEquals("301", seen.get(0).getString(70));
    }

    @Test
    void aLinkThatWasNeverDeployedIsNotDeployed() throws Exception {
        Link link = new Link("acquirer-c");
        assertEquals(Status.NOT_DEPLOYED, new LinkStatus(link.muxName, 5000).check());
    }

    @Test
    void aSilentHostIsNoAnswer() throws Exception {
        Link link = new Link("acquirer-d");
        link.deploy();
        link.up();
        link.silent = true;
        assertEquals(Status.NO_ANSWER, new LinkStatus(link.muxName, 300).check());
        assertEquals(1, link.received().size(), "the echo reached the host, which kept quiet");
    }

    @Test
    void aDeadLinkIsDownAndNothingWaitsToGoOut() throws Exception {
        Link link = new Link("acquirer-e");
        link.deploy();                                            // the adaptor never connects
        assertEquals(Status.DOWN, new LinkStatus(link.muxName, 300).check());
        assertNull(link.sp.rdp(link.send), "an echo was left waiting for the link to come back");
        link.up();
        assertTrue(link.received().isEmpty(), "the host received an echo after the check had ended");
    }

    @Test
    void aLinkDeployedAfterTheProbeStartedIsFound() throws Exception {
        Link link = new Link("acquirer-f");
        LinkStatus probe = new LinkStatus(link.muxName, 5000);
        assertEquals(Status.NOT_DEPLOYED, probe.check());
        link.deploy();
        link.up();
        assertEquals(Status.UP, probe.check());
    }

    @Test
    void aRedeployedLinkIsStillUp() throws Exception {
        Link link = new Link("acquirer-g");
        link.deploy();
        link.up();
        LinkStatus probe = new LinkStatus(link.muxName, 5000);
        assertEquals(Status.UP, probe.check());
        link.redeploy();                                          // someone edited 20_ in deploy/
        assertEquals(Status.UP, probe.check());
        assertEquals(Status.UP, probe.check());
        assertEquals(3, link.received().size());
    }

    // ---- test support: a real QMUX registered by name, as Q2 does, and a stand-in adaptor ----

    private static final AtomicInteger SPACES = new AtomicInteger();

    static final class Link implements SpaceListener<String, Object> {
        final String muxName;
        final String space = "tspace:status-" + SPACES.incrementAndGet();
        @SuppressWarnings("unchecked")
        final LocalSpace<String, Object> sp = (LocalSpace<String, Object>) SpaceFactory.getSpace(space);
        final String send, receive, ready;
        private final List<ISOMsg> received = new ArrayList<>();
        volatile boolean silent;
        private QMUX mux;

        Link(String name) {
            muxName = name + "-mux-" + SPACES.get();
            send = name + "-send";
            receive = name + "-receive";
            ready = name + "-channel.ready";
            sp.addListener(send, this);
        }

        /** What Q2 does with 20_..._mux.xml: create, configure, register under the name, start. */
        void deploy() throws Exception {
            Element e = new Element("mux");
            e.addContent(new Element("space").setText(space));
            e.addContent(new Element("in").setText(receive));
            e.addContent(new Element("out").setText(send));
            e.addContent(new Element("ready").setText(ready));
            QMUX q = new QMUX();
            q.setName(muxName);
            q.setPersist(e);
            q.setConfiguration(new SimpleConfiguration());
            q.init();
            q.start();
            mux = q;
        }

        /** What Q2 does when the file changes: stop and destroy the old one, deploy a new one. */
        void redeploy() throws Exception {
            mux.stop();
            mux.destroy();
            deploy();
        }

        void up() {
            sp.out(ready, new java.util.Date());
            Object pending;
            while ((pending = sp.inp(send)) != null)
                take((ISOMsg) pending);
        }

        synchronized List<ISOMsg> received() {
            return new ArrayList<>(received);
        }

        @Override
        public void notify(String queue, Object value) {
            if (sp.rdp(ready) == null)
                return;                                           // not connected: the message waits
            ISOMsg request = (ISOMsg) sp.inp(queue);
            if (request != null)
                take(request);
        }

        private void take(ISOMsg request) {
            synchronized (this) {
                received.add(request);
            }
            if (silent)
                return;
            try {
                ISOMsg response = (ISOMsg) request.clone();
                response.setResponseMTI();
                response.set(39, "00");
                sp.out(receive, response);
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        }
    }
}

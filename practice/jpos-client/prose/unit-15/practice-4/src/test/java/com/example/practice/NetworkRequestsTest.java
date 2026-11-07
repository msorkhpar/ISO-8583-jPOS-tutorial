package com.example.practice;

import org.jpos.iso.ISOMsg;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetworkRequestsTest {
    private static final String LINK = "acq-mux";

    private static ISOMsg fromHost(String mti, String code, String stan) throws Exception {
        ISOMsg m = new ISOMsg(mti);
        m.set(7, "1107021744");
        m.set(11, stan);
        if (code != null)
            m.set(70, code);
        return m;
    }

    private static ISOMsg keyChange(String stan) throws Exception {
        ISOMsg m = fromHost("0800", "161", stan);
        m.set(48, "6D0F2A9B3C4E5F61728394A5B6C7D8E9" + "4BE3CA");
        return m;
    }

    @Test
    void anEchoIsAnsweredOnTheLink() throws Exception {
        Rig rig = new Rig(m -> true);
        rig.hostSends(fromHost("0800", "301", "000317"));
        ISOMsg a = rig.onlyAnswer();
        assertEquals("0810", a.getMTI());
        assertEquals("00", a.getString(39));
        assertEquals("000317", a.getString(11));
        assertEquals("1107021744", a.getString(7));
        assertEquals("301", a.getString(70));
        assertNull(rig.unhandled(), "the echo was answered, not left on the unhandled queue");
    }

    @Test
    void aHostSignOnIsAcceptedAndTheMarksStay() throws Exception {
        Rig rig = new Rig(m -> true);
        rig.hostSends(fromHost("0800", "001", "000318"));
        assertEquals("00", rig.onlyAnswer().getString(39));
        assertTrue(rig.sp.rdp(LINK + ".enabled") == rig.ready);
        assertTrue(rig.sp.rdp(LINK + ".logon") == rig.ready);
    }

    @Test
    void whatIsNotNetworkManagementIsLeftToTheNextListener() throws Exception {
        Rig rig = new Rig(m -> true);
        ISOMsg authorization = new ISOMsg("0100");
        authorization.set(11, "000319");
        authorization.set(41, "29110007");
        ISOMsg lateAnswer = new ISOMsg("0210");
        lateAnswer.set(11, "000320");
        lateAnswer.set(41, "29110007");
        lateAnswer.set(39, "00");
        ISOMsg answerToUs = fromHost("0810", "301", "000321");
        answerToUs.set(39, "00");
        rig.hostSends(fromHost("0800", "301", "000327"));
        assertEquals("0810", rig.onlyAnswer().getMTI(), "the listener answers the host's echo");
        for (ISOMsg m : List.of(authorization, lateAnswer, answerToUs)) {
            rig.hostSends(m);
            assertEquals(0, rig.answers().size(), m.getMTI() + " was answered");
            ISOMsg next = rig.unhandled();
            assertNotNull(next, m.getMTI() + " never reached the unhandled queue");
            assertEquals(m.getMTI(), next.getMTI());
        }
    }

    @Test
    void aHostSignOffTakesTheLinkOutAndLetsTheManagerSignOnAgain() throws Exception {
        Rig rig = new Rig(m -> true);
        rig.hostSends(fromHost("0800", "002", "000322"));
        assertEquals("00", rig.onlyAnswer().getString(39));
        assertNull(rig.sp.rdp(LINK + ".enabled"), "the pool still uses the link");
        assertNull(rig.sp.rdp(LINK + ".logon"), "the manager still thinks this connection is signed on");
        assertTrue(rig.sp.rdp(Rig.READY) == rig.ready, "the ready entry belongs to the adaptor");
    }

    @Test
    void theKeyNeverTravelsBack() throws Exception {
        Rig rig = new Rig(m -> true);
        rig.hostSends(keyChange("000323"));
        ISOMsg a = rig.onlyAnswer();
        assertEquals("00", a.getString(39));
        assertFalse(a.hasField(48), "the answer carries the key back to the host");
        for (int f = 1; f <= 128; f++)
            if (a.hasField(f))
                assertTrue(f == 7 || f == 11 || f == 39 || f == 70, "field " + f + " is not in an 0810");
    }

    @Test
    void aKeyThatWasNotInstalledIsRefused() throws Exception {
        List<String> offered = new ArrayList<>();
        Rig rig = new Rig(m -> { offered.add(m.getString(11)); return false; });
        rig.hostSends(keyChange("000324"));
        assertEquals(List.of("000324"), offered, "the key change goes to the installer");
        assertEquals("06", rig.onlyAnswer().getString(39));
    }

    @Test
    void advicesAreAcknowledged() throws Exception {
        Rig rig = new Rig(m -> true);
        rig.hostSends(fromHost("0820", "201", "000325"));
        ISOMsg a = rig.onlyAnswer();
        assertEquals("0830", a.getMTI());
        assertEquals("00", a.getString(39));
        rig.hostSends(fromHost("0821", "201", "000325"));
        ISOMsg b = rig.onlyAnswer();
        assertEquals("0830", b.getMTI());
        assertEquals("00", b.getString(39));
    }

    @Test
    void anUnknownFunctionIsRefused() throws Exception {
        Rig rig = new Rig(m -> true);
        rig.hostSends(fromHost("0800", "370", "000326"));
        assertEquals("06", rig.onlyAnswer().getString(39));
    }

    // ---- test support: a real QMUX in this JVM, with the listener, and the host's side of its queues ----

    private static final java.util.concurrent.atomic.AtomicInteger SPACES = new java.util.concurrent.atomic.AtomicInteger();

    static final class Rig implements org.jpos.space.SpaceListener<String, Object> {
        static final String READY = "acq-channel.ready";
        final org.jpos.space.LocalSpace<String, Object> sp;
        final org.jpos.q2.iso.QMUX mux;
        final Date ready = new Date();
        private final List<ISOMsg> answers = new ArrayList<>();

        @SuppressWarnings("unchecked")
        Rig(NetworkRequests.KeyInstaller keys) throws Exception {
            String space = "tspace:practice-" + SPACES.incrementAndGet();
            sp = (org.jpos.space.LocalSpace<String, Object>) org.jpos.space.SpaceFactory.getSpace(space);
            org.jdom2.Element e = new org.jdom2.Element("mux");
            e.addContent(new org.jdom2.Element("space").setText(space));
            e.addContent(new org.jdom2.Element("in").setText("acq-receive"));
            e.addContent(new org.jdom2.Element("out").setText("acq-send"));
            e.addContent(new org.jdom2.Element("ready").setText(READY));
            e.addContent(new org.jdom2.Element("unhandled").setText("acq-unhandled"));
            mux = new org.jpos.q2.iso.QMUX();
            mux.setName("acq-mux-" + space);
            mux.setPersist(e);
            mux.setConfiguration(new org.jpos.core.SimpleConfiguration());
            mux.init();
            mux.start();
            sp.put(READY, ready);
            sp.put(LINK + ".logon", ready);
            sp.put(LINK + ".enabled", ready);
            mux.addISORequestListener(new NetworkRequests(sp, LINK, keys));
            sp.addListener("acq-send", this);
        }

        /** The host puts a message on the link; QMUX and its listener handle it in this thread. */
        void hostSends(ISOMsg m) {
            sp.out("acq-receive", m);
        }

        @Override
        public synchronized void notify(String queue, Object value) {
            Object m = sp.inp(queue);
            if (m != null)
                answers.add((ISOMsg) m);
        }

        synchronized List<ISOMsg> answers() {
            return new ArrayList<>(answers);
        }

        /** The one answer the host received since the last call. */
        synchronized ISOMsg onlyAnswer() {
            assertEquals(1, answers.size(), "the host received " + answers.size() + " answers");
            return answers.remove(0);
        }

        ISOMsg unhandled() {
            return (ISOMsg) sp.inp("acq-unhandled");
        }
    }
}

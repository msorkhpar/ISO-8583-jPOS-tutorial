package com.example.practice;

import org.jpos.iso.ISOMsg;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResponderTest {

    private static ISOMsg cardRequest(String mti, String terminal, String stan) throws Exception {
        ISOMsg m = new ISOMsg(mti);
        m.set(2, "5555555555554444");
        m.set(3, "000000");
        m.set(4, "000000018740");
        m.set(7, "0312091544");
        m.set(11, stan);
        m.set(12, "091544");
        m.set(13, "0312");
        m.set(41, terminal);
        m.set(42, "000000007654321");
        m.set(49, "978");
        return m;
    }

    @Test
    void anApprovedPurchaseReachesTheTerminal() throws Exception {
        Rig rig = new Rig();
        ISOMsg answer = rig.request(cardRequest("0200", "POS00417", "000318"), "00", "Z7Q2K9");
        assertNotNull(answer, "the terminal's client got no answer it recognised");
        assertEquals("00", answer.getString(39));
        assertEquals("Z7Q2K9", answer.getString(38));
    }

    @Test
    void aDeclinedPurchaseReachesTheTerminal() throws Exception {
        Rig rig = new Rig();
        ISOMsg answer = rig.request(cardRequest("0200", "POS00417", "000319"), "05", "Z7Q2K9");
        assertNotNull(answer, "the terminal's client got no answer it recognised");
        assertEquals("05", answer.getString(39));
    }

    @Test
    void aCompletionAdviceIsAcknowledged() throws Exception {
        Rig rig = new Rig();
        ISOMsg answer = rig.request(cardRequest("0220", "POS00417", "000320"), "00", "Z7Q2K9");
        assertNotNull(answer, "the completion was never acknowledged: the terminal will send it again");
    }

    @Test
    void aRepeatedReversalIsAcknowledged() throws Exception {
        Rig rig = new Rig();
        ISOMsg reversal = cardRequest("0421", "ATM00932", "004417");
        reversal.set(90, "020000431503120915440000000123400000000000");
        ISOMsg answer = rig.request(reversal, "00", "Z7Q2K9");
        assertNotNull(answer, "the repeat was never acknowledged: the terminal will keep repeating it");
    }

    @Test
    void anAuthorizationWithTheAcquirersReferenceIsAnswered() throws Exception {
        Rig rig = new Rig();
        ISOMsg auth = cardRequest("0100", "ATM00932", "004418");
        auth.set(37, "631000004418");
        ISOMsg answer = rig.request(auth, "00", "P4M8X1");
        assertNotNull(answer, "the terminal's client got no answer it recognised");
        assertEquals("00", answer.getString(39));
    }

    @Test
    void cardDataStaysWithTheServer() throws Exception {
        Rig rig = new Rig();
        ISOMsg swiped = cardRequest("0200", "POS00417", "000321");
        swiped.set(14, "2803");
        swiped.set(35, "5555555555554444=28031011000012300000");
        swiped.set(45, "B5555555555554444^EXAMPLE/CARD^28031011000012300000");
        swiped.set(52, new byte[] { 0x1A, 0x2B, 0x3C, 0x4D, 0x5E, 0x6F, 0x70, (byte) 0x81 });
        swiped.set(55, "9F2608A1B2C3D4E5F60718");
        ISOMsg answer = rig.request(swiped, "00", "Z7Q2K9");
        assertNotNull(answer, "the terminal's client got no answer it recognised");
        for (int f : new int[] { 14, 35, 45, 52, 55 })
            assertFalse(answer.hasField(f), "field " + f + " went back to the terminal");
    }

    @Test
    void theRequestIsLeftAsItArrived() throws Exception {
        Rig rig = new Rig();
        ISOMsg swiped = cardRequest("0200", "POS00417", "000322");
        swiped.set(35, "5555555555554444=28031011000012300000");
        assertNotNull(rig.request(swiped, "00", "Z7Q2K9"), "the terminal's client got no answer it recognised");
        ISOMsg request = rig.received.get(0);
        assertEquals("0200", request.getMTI(), "the request in the Context is no longer a request");
        assertFalse(request.hasField(39), "the request in the Context has a response code");
        assertTrue(request.hasField(35), "the request in the Context lost its track 2");
    }

    @Test
    void aDeclineCarriesNoApprovalCode() throws Exception {
        Rig rig = new Rig();
        ISOMsg answer = rig.request(cardRequest("0100", "ATM00932", "004419"), "51", "P4M8X1");
        assertNotNull(answer, "the terminal's client got no answer it recognised");
        assertEquals("51", answer.getString(39));
        assertFalse(answer.hasField(38), "a declined authorization carries an approval code");
    }

    // ---- test support: the terminals' client (a real QMUX keyed on 41, 11 and 37), joined to
    //      the Responder by the wire format, as the server's channel would be ----

    private static final java.util.concurrent.atomic.AtomicInteger RIGS = new java.util.concurrent.atomic.AtomicInteger();

    static final class Rig implements org.jpos.space.SpaceListener<String, Object> {
        final org.jpos.space.LocalSpace<String, Object> sp;
        final org.jpos.q2.iso.QMUX mux;
        final org.jpos.iso.ISOPackager packager;
        final java.util.List<ISOMsg> received = new java.util.concurrent.CopyOnWriteArrayList<>();
        private String responseCode, approvalCode;

        @SuppressWarnings("unchecked")
        Rig() throws Exception {
            String name = "responder-" + RIGS.incrementAndGet();
            sp = (org.jpos.space.LocalSpace<String, Object>) org.jpos.space.SpaceFactory.getSpace("tspace:" + name);
            packager = new org.jpos.iso.packager.GenericPackager("jar:packager/iso87ascii.xml");
            org.jdom2.Element e = new org.jdom2.Element("mux");
            e.addContent(new org.jdom2.Element("space").setText("tspace:" + name));
            e.addContent(new org.jdom2.Element("in").setText("terminal-receive"));
            e.addContent(new org.jdom2.Element("out").setText("terminal-send"));
            e.addContent(new org.jdom2.Element("key").setText("41, 11, 37"));
            mux = new org.jpos.q2.iso.QMUX();
            mux.setName(name);
            mux.setPersist(e);
            mux.setConfiguration(new org.jpos.core.SimpleConfiguration());
            mux.init();
            mux.start();
            sp.addListener("terminal-send", this);
        }

        ISOMsg request(ISOMsg m, String responseCode, String approvalCode) throws Exception {
            this.responseCode = responseCode;
            this.approvalCode = approvalCode;
            return mux.request(m, 1500);
        }

        @Override
        public void notify(String queue, Object value) {
            ISOMsg sent = (ISOMsg) sp.inp(queue);
            if (sent == null) return;
            try {
                ISOMsg request = travel(sent);
                received.add(request);
                ISOMsg response = new Responder().answer(request, responseCode, approvalCode);
                sp.out("terminal-receive", travel(response));
            } catch (Exception ex) {
                // no answer goes back: the client's request returns null
            }
        }

        private ISOMsg travel(ISOMsg m) throws org.jpos.iso.ISOException {
            ISOMsg out = (ISOMsg) m.clone();
            out.setPackager(packager);
            byte[] wire = out.pack();
            ISOMsg in = new ISOMsg();
            in.setPackager(packager);
            in.unpack(wire);
            return in;
        }
    }
}

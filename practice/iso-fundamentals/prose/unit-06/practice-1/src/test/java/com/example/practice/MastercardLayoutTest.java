package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;
import org.jpos.iso.packager.GenericPackager;
import org.junit.jupiter.api.Test;

class MastercardLayoutTest {

    private static String wireHex(ISOMsg m) throws Exception {
        m.setPackager(MastercardLayout.packager());
        return ISOUtil.hexString(m.pack());
    }

    @Test
    void packsAndReadsBackAnAuthorization() throws Exception {
        GenericPackager packager = MastercardLayout.packager();
        ISOMsg sent = new ISOMsg("0100");
        sent.set(2, "4111111111111111");
        sent.set(3, "003000");
        sent.set(4, "000000123456");
        sent.set(11, "004417");
        sent.set(41, "TERM0042");
        sent.set(42, "SHOP00000000017");
        sent.set(49, "978");
        sent.setPackager(packager);
        byte[] wire = sent.pack();
        assertTrue(ISOUtil.hexString(wire).startsWith("F0F1F0F0"), "the MTI as the samples write it");

        ISOMsg received = new ISOMsg();
        received.setPackager(packager);
        received.unpack(wire);
        assertEquals("0100", received.getMTI());
        for (int field : new int[] {2, 3, 4, 11, 41, 42, 49}) {
            assertEquals(sent.getString(field), received.getString(field), "field " + field);
        }
    }

    @Test
    void theBitmapIsEightRawBytes() throws Exception {
        ISOMsg m = new ISOMsg("0200");
        m.set(4, "000000000100");
        m.set(11, "000002");
        m.set(42, "STORE0000000001");
        // MTI, then fields 4, 11 and 42 announced in 8 bytes, then field 4 right after them.
        String hex = wireHex(m);
        assertEquals("F0F2F0F0" + "1020000000400000", hex.substring(0, 24));
        assertEquals("F0F0F0F0F0F0F0F0F0F1F0F0", hex.substring(24, 48));
    }

    @Test
    void thePanAndItsLengthAreOneByteADigit() throws Exception {
        ISOMsg m = new ISOMsg("0100");
        m.set(2, "4111111111111111");
        String hex = wireHex(m);
        assertEquals("F1F6" + "F4F1F1F1F1F1F1F1F1F1F1F1F1F1F1F1", hex.substring(24));
    }

    @Test
    void textFieldsAreWrittenLikeTheSamples() throws Exception {
        ISOMsg m = new ISOMsg("0100");
        m.set(41, "AB7");
        m.set(42, "Z9");
        String hex = wireHex(m);
        assertEquals("C1C2F74040404040" + "E9F9" + "40".repeat(13), hex.substring(24));
    }
}

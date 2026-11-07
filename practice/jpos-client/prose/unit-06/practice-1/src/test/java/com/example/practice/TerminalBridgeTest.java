package com.example.practice;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;
import org.jpos.iso.packager.GenericPackager;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Deque;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class TerminalBridgeTest {
    private static final String BLOCK_A = "3C91E07A5B2D4F18";
    private static final String BLOCK_B = "D2075A9E1C6B38F4";
    private static final String ICC = "9F2701809F3602007C950500800000009F1A020840";

    /** A PIN pad that hands out the given blocks in order, writing each into the caller's buffer. */
    private static TerminalBridge.PinPad pinPad(String... blocks) {
        Deque<byte[]> left = new ArrayDeque<>();
        for (String b : blocks) left.add(ISOUtil.hex2byte(b));
        return buffer -> System.arraycopy(left.poll(), 0, buffer, 0, 8);
    }

    /** What the far end sees: the message packed with {@code packagerXml} and unpacked again. */
    private static ISOMsg overTheWire(ISOMsg m, String packagerXml) throws Exception {
        GenericPackager p = new GenericPackager(packagerXml);
        ISOMsg out = (ISOMsg) m.clone();
        out.setPackager(p);
        byte[] wire = out.pack();
        ISOMsg in = new ISOMsg();
        in.setPackager(p);
        in.unpack(wire);
        return in;
    }

    private static ISOMsg hostResponse(String iccHex) throws Exception {
        ISOMsg r = new ISOMsg("0210");
        r.set(11, "004417");
        r.set(39, "00");
        if (iccHex != null)
            r.set(55, ISOUtil.hex2byte(iccHex));
        return r;
    }

    @Test
    void aPurchaseCarriesThePinBlockTheCardholderTyped() throws Exception {
        TerminalBridge bridge = new TerminalBridge(pinPad(BLOCK_A));
        ISOMsg m = bridge.purchase("4111111111111111", 1999, "004416", null);
        ISOMsg atHost = overTheWire(m, "jar:packager/iso87ascii.xml");
        assertArrayEquals(ISOUtil.hex2byte(BLOCK_A), atHost.getBytes(52));
        assertEquals("000000001999", atHost.getString(4));
    }

    @Test
    void theHostsChipDataReachesTheTerminalAsHex() throws Exception {
        TerminalBridge bridge = new TerminalBridge(pinPad());
        String answer = "910A1F2E3D4C5B6A79880012";
        ISOMsg received = overTheWire(hostResponse(answer), "jar:packager/iso93ascii.xml");
        assertEquals(answer, bridge.chipDataForTerminal(received).toUpperCase());
    }

    @Test
    void chipDataGoesOutAsTheBytesTheChipProduced() throws Exception {
        TerminalBridge bridge = new TerminalBridge(pinPad(BLOCK_A, BLOCK_A));
        byte[] chip = ISOUtil.hex2byte(ICC);
        ISOMsg binaryLink = overTheWire(bridge.purchase("5555555555554444", 4200, "004418", ICC),
                "jar:packager/iso93ascii.xml");
        assertArrayEquals(chip, binaryLink.getBytes(55), "a link that declares field 55 as binary");
        ISOMsg asciiLink = overTheWire(bridge.purchase("5555555555554444", 4200, "004419", ICC),
                "jar:packager/iso87ascii.xml");
        assertArrayEquals(chip, asciiLink.getBytes(55), "the course's ASCII link");
    }

    @Test
    void eachRequestKeepsItsOwnPinBlock() throws Exception {
        TerminalBridge bridge = new TerminalBridge(pinPad(BLOCK_A, BLOCK_B));
        ISOMsg first = bridge.purchase("4111111111111111", 650, "004420", null);
        ISOMsg second = bridge.purchase("5555555555554444", 1225, "004421", null);
        assertArrayEquals(ISOUtil.hex2byte(BLOCK_A), first.getBytes(52),
                "the first request still waits to be packed after the second cardholder typed a PIN");
        assertArrayEquals(ISOUtil.hex2byte(BLOCK_B), second.getBytes(52));
    }

    @Test
    void chipDataFromAnAsciiLinkReachesTheTerminalAsHex() throws Exception {
        TerminalBridge bridge = new TerminalBridge(pinPad());
        String answer = "8A023030910A5E4F3A2B1C0D8E7F0012";
        ISOMsg received = overTheWire(hostResponse(answer), "jar:packager/iso87ascii.xml");
        String hex = bridge.chipDataForTerminal(received);
        assertNotNull(hex);
        assertEquals(answer, hex.toUpperCase());
    }

    @Test
    void aResponseWithoutChipDataGivesTheTerminalNothing() throws Exception {
        TerminalBridge bridge = new TerminalBridge(pinPad());
        ISOMsg received = overTheWire(hostResponse(null), "jar:packager/iso93ascii.xml");
        assertNull(bridge.chipDataForTerminal(received));
    }
}

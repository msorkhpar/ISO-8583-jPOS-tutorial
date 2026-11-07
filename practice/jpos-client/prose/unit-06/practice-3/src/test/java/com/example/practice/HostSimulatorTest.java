package com.example.practice;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HostSimulatorTest {
    private static final byte[] PIN_BLOCK = ISOUtil.hex2byte("5E2A09C4718BD360");
    private static final byte[] ICC = ISOUtil.hex2byte("9F36020091950500000480009F2701");

    private static ISOMsg keyed(String mti, String stan) throws Exception {
        ISOMsg m = new ISOMsg(mti);
        m.set(2, "5555555555554444");
        m.set(3, "000000");
        m.set(4, "000000003175");
        m.set(7, "1107142208");
        m.set(11, stan);
        m.set(12, "092208");
        m.set(13, "1107");
        m.set(14, "3003");
        m.set(22, "012");
        m.set(32, "500001");
        m.set(37, "631114" + stan);
        m.set(41, "29110001");
        m.set(42, "000000001234567");
        m.set(49, "978");
        return m;
    }

    /** A chip purchase with everything a chip read carries. */
    private static ISOMsg chip(String mti, String stan) throws Exception {
        ISOMsg m = keyed(mti, stan);
        m.unset(14);
        m.set(22, "051");
        m.set(23, "001");
        m.set(35, "5555555555554444=30032011234500000");
        m.set(45, "B5555555555554444^TEST/CARD^30032011234500000");
        m.set(52, PIN_BLOCK.clone());
        m.set(55, ICC.clone());
        return m;
    }

    @Test
    void aPurchaseIsAnsweredWithItsEchoedFields() throws Exception {
        ISOMsg request = keyed("0200", "005521");
        ISOMsg response = new HostSimulator().respond(request, "00");
        assertEquals("0210", response.getMTI());
        for (int f : HostSimulator.ECHOED)
            assertEquals(request.getString(f), response.getString(f), "field " + f);
        assertEquals("00", response.getString(39));
        assertTrue(response.hasField(38));
        ISOMsg decline = new HostSimulator().respond(keyed("0100", "005522"), "51");
        assertEquals("0110", decline.getMTI());
        assertEquals("51", decline.getString(39));
    }

    @Test
    void aRepeatGetsTheAnswerOfAFirstAttempt() throws Exception {
        HostSimulator host = new HostSimulator();
        assertEquals("0230", host.respond(keyed("0221", "005523"), "00").getMTI());
        assertEquals("0410", host.respond(keyed("0401", "005524"), "00").getMTI());
        assertEquals("0130", host.respond(keyed("0120", "005525"), "00").getMTI());
    }

    @Test
    void nothingTheDocumentDoesNotListComesBack() throws Exception {
        ISOMsg response = new HostSimulator().respond(chip("0200", "005526"), "00");
        for (int f = 1; f <= 128; f++) {
            if (f == 38 || f == 39) continue;
            boolean listed = false;
            for (int e : HostSimulator.ECHOED) listed |= e == f;
            if (!listed)
                assertFalse(response.hasField(f), "field " + f + " came back");
        }
    }

    @Test
    void theRequestTheTestKeepsIsUnchanged() throws Exception {
        ISOMsg request = chip("0200", "005527");
        new HostSimulator().respond(request, "05");
        assertEquals("0200", request.getMTI());
        assertFalse(request.hasField(39));
        assertEquals("001", request.getString(23));
        assertEquals("051", request.getString(22));
        assertArrayEquals(PIN_BLOCK, request.getBytes(52));
        assertArrayEquals(ICC, request.getBytes(55));
        assertTrue(request.hasField(35) && request.hasField(45));
    }
}

package com.example.practice;

import org.jpos.iso.ISOMsg;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class EntryModeScreenTest {
    private static final String TRACK2 = "5555555555554444=29031010000047100000";
    private static final byte[] ICC = { (byte) 0x9F, 0x26, 0x08, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, (byte) 0x88 };

    /** A purchase from terminal 29110023; fields: 22, then any of 14, 35, 55 as given. */
    private static ISOMsg purchase(String entryMode, boolean expiry, boolean track2, boolean icc) throws Exception {
        ISOMsg m = new ISOMsg("0200");
        m.set(2, "5555555555554444");
        m.set(3, "000000");
        m.set(4, "000000008415");
        m.set(11, "004317");
        m.set(41, "29110023");
        m.set(42, "000000004455667");
        if (entryMode != null) m.set(22, entryMode);
        if (expiry) m.set(14, "2903");
        if (track2) m.set(35, TRACK2);
        if (icc) m.set(55, ICC);
        return m;
    }

    private static void assertRefused(ISOMsg request) throws Exception {
        ISOMsg answer = new EntryModeScreen().screen(request);
        assertNotNull(answer, "this request contradicts itself and must not go upstream");
        assertEquals("0210", answer.getMTI());
        assertEquals("30", answer.getString(39));
        assertFalse(answer.hasField(35) || answer.hasField(55), "card data is never echoed to the terminal");
    }

    @Test
    void aChipPurchaseWithItsChipDataGoesUpstream() throws Exception {
        assertNull(new EntryModeScreen().screen(purchase("051", false, true, true)));
    }

    @Test
    void aKeyedPurchaseWithItsExpiryGoesUpstream() throws Exception {
        assertNull(new EntryModeScreen().screen(purchase("011", true, false, false)));
    }

    @Test
    void aChipCodeWithOnlyATrackIsRefused() throws Exception {
        assertRefused(purchase("051", false, true, false));
    }

    @Test
    void aTerminalWithoutAPinPadIsNotRefused() throws Exception {
        assertNull(new EntryModeScreen().screen(purchase("902", false, true, false)));
        assertNull(new EntryModeScreen().screen(purchase("052", false, true, true)));
    }

    @Test
    void aContactlessCodeWithOnlyATrackIsRefused() throws Exception {
        assertRefused(purchase("071", false, true, false));
    }

    @Test
    void aKeyedCodeCarryingATrackIsRefused() throws Exception {
        assertRefused(purchase("011", true, true, false));
    }

    @Test
    void aCodeTheSpecificationDoesNotListIsRefused() throws Exception {
        assertRefused(purchase("021", false, true, false));
    }
}

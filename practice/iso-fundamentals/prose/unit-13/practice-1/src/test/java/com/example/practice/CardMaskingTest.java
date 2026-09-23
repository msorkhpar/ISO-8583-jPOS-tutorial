package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.jpos.iso.ISOMsg;
import org.junit.jupiter.api.Test;

class CardMaskingTest {

    private static ISOMsg carrying(String pan) throws Exception {
        ISOMsg m = new ISOMsg();
        m.setMTI("0100");
        if (pan != null) {
            m.set(2, pan);
        }
        m.set(4, "000000010000");
        m.set(11, "000123");
        return m;
    }

    @Test
    void showsOnlyTheFirstSixAndLastFourDigits() throws Exception {
        ISOMsg shown = CardMasking.forDisplay(carrying("4111111111111111"));
        assertEquals("411111******1111", shown.getString(2));
        assertEquals("0100", shown.getMTI());
        assertEquals("000000010000", shown.getString(4));
        assertEquals("000123", shown.getString(11));

        ISOMsg noPan = CardMasking.forDisplay(carrying(null));
        assertFalse(noPan.hasField(2));
        assertEquals("000123", noPan.getString(11));
    }

    @Test
    void masksAPanOfAnyLength() throws Exception {
        assertEquals("601100*********0004",
            CardMasking.forDisplay(carrying("6011000990139420004")).getString(2));
    }

    @Test
    void leavesTheMessageBeingProcessedUntouched() throws Exception {
        ISOMsg processed = carrying("4111111111111111");
        CardMasking.forDisplay(processed);
        assertEquals("4111111111111111", processed.getString(2));
    }
}

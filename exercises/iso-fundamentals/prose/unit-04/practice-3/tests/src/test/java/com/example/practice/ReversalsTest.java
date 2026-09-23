package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.jpos.iso.ISOMsg;
import org.junit.jupiter.api.Test;

class ReversalsTest {

    private static ISOMsg purchase(String mti) throws Exception {
        ISOMsg message = new ISOMsg();
        message.setMTI(mti);
        message.set(2, "4111111111111111");
        message.set(3, "000000");
        message.set(4, "000000012345");
        message.set(7, "0923101500");
        message.set(11, "123456");
        message.set(22, "051");
        message.set(37, "926610123456");
        message.set(38, "A1B2C3");
        message.set(39, "00");
        message.set(41, "12345678");
        message.set(42, "MERCHANT123456");
        message.set(49, "840");
        return message;
    }

    @Test
    void aReversalCarriesWhatIdentifiesTheOriginal() throws Exception {
        ISOMsg reversal = Reversals.reversalOf(purchase("0200"));
        assertEquals("0400", reversal.getMTI());
        assertEquals("4111111111111111", reversal.getString(2));
        assertEquals("000000", reversal.getString(3));
        assertEquals("000000012345", reversal.getString(4));
        assertEquals("123456", reversal.getString(11));
        assertEquals("926610123456", reversal.getString(37));
        assertEquals("12345678", reversal.getString(41));
        assertEquals("MERCHANT123456", reversal.getString(42));
        for (int notCarried : new int[] {7, 22, 38, 39, 49}) {
            assertFalse(reversal.hasField(notCarried), "field " + notCarried + " is not carried");
        }

        // An authorization without a retrieval reference number: nothing is invented for it.
        ISOMsg authorization = purchase("0100");
        authorization.unset(37);
        ISOMsg reversed = Reversals.reversalOf(authorization);
        assertEquals("0400", reversed.getMTI());
        assertEquals("123456", reversed.getString(11));
        assertFalse(reversed.hasField(37));
    }

    @Test
    void leavesTheOriginalAsItWas() throws Exception {
        ISOMsg original = purchase("0200");
        Reversals.reversalOf(original);
        assertEquals("0200", original.getMTI());
        assertEquals("00", original.getString(39));
        assertEquals("840", original.getString(49));
        assertEquals("0923101500", original.getString(7));
    }

    @Test
    void refusesWhatIsNotATransactionRequest() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> Reversals.reversalOf(purchase("0110")));
        ISOMsg echo = new ISOMsg();
        echo.setMTI("0800");
        echo.set(11, "000001");
        echo.set(70, "301");
        assertThrows(IllegalArgumentException.class, () -> Reversals.reversalOf(echo));
    }
}

package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jpos.iso.ISOBinaryField;
import org.jpos.iso.ISOMsg;
import org.junit.jupiter.api.Test;

class ResponseMakerTest {

    // A standard test card number: no bank issues it.
    private static final String PAN = "4111111111111111";
    private static final byte[] EMV = {(byte) 0x9F, 0x26, 0x02, (byte) 0xA1, (byte) 0xB2};
    private static final int[] ECHOED = {2, 3, 4, 7, 11, 41, 49};

    private static ISOMsg request(String mti) throws Exception {
        ISOMsg msg = new ISOMsg();
        msg.setMTI(mti);
        msg.set(2, PAN);
        msg.set(3, "000000");
        msg.set(4, "000000010000");
        msg.set(7, "0701104321");
        msg.set(11, "123456");
        msg.set(41, "12345678");
        msg.set(49, "840");
        msg.set(new ISOBinaryField(55, EMV));
        return msg;
    }

    @Test
    void aPurchaseIsAnsweredWithItsFieldsAndAResponseCode() throws Exception {
        ISOMsg request = request("0200");
        ISOMsg response = new ResponseMaker().respond(request, "00");
        assertNotNull(response);
        assertEquals("0210", response.getMTI());
        for (int field : ECHOED) {
            assertEquals(request.getString(field), response.getString(field), "field " + field);
        }
        assertEquals("00", response.getString(39));
        assertFalse(response.hasField(55));

        ISOMsg noChip = request("0200");
        noChip.unset(55);
        ISOMsg declined = new ResponseMaker().respond(noChip, "05");
        assertEquals("05", declined.getString(39));
        assertFalse(declined.hasField(55));
    }

    @Test
    void anAuthorizationIsAnsweredWith0110() throws Exception {
        ISOMsg response = new ResponseMaker().respond(request("0100"), "00");
        assertEquals("0110", response.getMTI());
        assertEquals("00", response.getString(39));
    }

    @Test
    void theRequestIsLeftUntouched() throws Exception {
        ISOMsg request = request("0200");
        new ResponseMaker().respond(request, "00");
        assertEquals("0200", request.getMTI());
        assertFalse(request.hasField(39));
        assertTrue(request.hasField(55));
        assertArrayEquals(EMV, request.getBytes(55));
        for (int field : ECHOED) {
            assertTrue(request.hasField(field), "field " + field);
        }
    }
}

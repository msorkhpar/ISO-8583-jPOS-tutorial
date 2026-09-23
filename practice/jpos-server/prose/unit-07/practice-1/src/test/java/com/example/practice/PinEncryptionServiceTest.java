package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;
import org.jpos.security.EncryptedPIN;
import org.jpos.security.SMAdapter;
import org.junit.jupiter.api.Test;

class PinEncryptionServiceTest {

    private static final String PAN = "4111111111111111";
    private static final String PIN = "1234";
    private static final byte[] BLOCK = {0x3A, 0x7F, 0x00, 0x12, (byte) 0xC4, 0x5E, (byte) 0x99, 0x01};

    @SuppressWarnings("rawtypes")
    private final SMAdapter module = mock(SMAdapter.class);

    private ISOMsg purchase() throws Exception {
        ISOMsg msg = new ISOMsg();
        msg.setMTI("0200");
        msg.set(2, PAN);
        msg.set(4, "000000001234");
        return msg;
    }

    private ISOMsg encrypted() throws Exception {
        when(module.encryptPIN(PIN, PAN)).thenReturn(new EncryptedPIN(BLOCK, SMAdapter.FORMAT01, PAN));
        ISOMsg msg = purchase();
        assertSame(msg, new PinEncryptionService(module).encryptPIN(msg, PIN));
        return msg;
    }

    @Test
    void theModuleEncryptsThePinWithTheCardNumber() throws Exception {
        ISOMsg msg = encrypted();
        verify(module).encryptPIN(PIN, PAN);
        assertEquals(ISOUtil.hexString(BLOCK), msg.getString(52).toUpperCase());
        for (int i = 0; i <= msg.getMaxField(); i++) {
            if (msg.hasField(i) && i != 52) {
                assertFalse(msg.getString(i).equals(PIN), "clear PIN in field " + i);
            }
        }
    }

    @Test
    void thePinBlockGoesInAsItsRawBytes() throws Exception {
        ISOMsg msg = encrypted();
        assertArrayEquals(BLOCK, msg.getBytes(52));
    }

    @Test
    void aMessageWithNoCardNumberIsRefusedBeforeTheModuleIsAsked() throws Exception {
        ISOMsg msg = purchase();
        msg.unset(2);
        assertThrows(IllegalArgumentException.class,
            () -> new PinEncryptionService(module).encryptPIN(msg, PIN));
        verifyNoInteractions(module);
        assertFalse(msg.hasField(52));
    }
}

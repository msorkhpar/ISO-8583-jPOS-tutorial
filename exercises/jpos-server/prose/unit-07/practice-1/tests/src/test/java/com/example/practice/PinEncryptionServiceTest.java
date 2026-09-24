package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
import org.jpos.security.SecureDESKey;
import org.junit.jupiter.api.Test;

class PinEncryptionServiceTest {

    private static final String PAN = "4111111111111111";
    private static final String PIN = "1234";
    /** What the module returns from encryptPIN: the PIN under its LMK. Must never reach the wire. */
    private static final byte[] LMK_BLOCK = {0x3A, 0x7F, 0x00, 0x12, (byte) 0xC4, 0x5E, (byte) 0x99, 0x01};
    /** What the module returns from exportPIN: the PIN under the ZPK. This is field 52. */
    private static final byte[] ZPK_BLOCK = {(byte) 0xB2, 0x04, 0x6D, (byte) 0xE1, 0x00, 0x57, 0x2C, (byte) 0x8F};
    private static final byte FORMAT = SMAdapter.FORMAT01;

    @SuppressWarnings("rawtypes")
    private final SMAdapter module = mock(SMAdapter.class);
    private final SecureDESKey zpk = new SecureDESKey(SMAdapter.LENGTH_DES3_2KEY, SMAdapter.TYPE_ZPK,
        "0123456789ABCDEFFEDCBA9876543210", "08D7B4");
    private final EncryptedPIN pinUnderLmk = new EncryptedPIN(LMK_BLOCK, SMAdapter.FORMAT01, PAN);

    private ISOMsg purchase() throws Exception {
        ISOMsg msg = new ISOMsg();
        msg.setMTI("0200");
        msg.set(2, PAN);
        msg.set(4, "000000001234");
        return msg;
    }

    @SuppressWarnings("unchecked")
    private PinEncryptionService service() {
        return new PinEncryptionService(module, zpk, FORMAT);
    }

    @SuppressWarnings("unchecked")
    private ISOMsg encrypted() throws Exception {
        when(module.encryptPIN(PIN, PAN)).thenReturn(pinUnderLmk);
        when(module.exportPIN(pinUnderLmk, zpk, FORMAT)).thenReturn(new EncryptedPIN(ZPK_BLOCK, FORMAT, PAN));
        ISOMsg msg = purchase();
        assertSame(msg, service().encryptPIN(msg, PIN));
        return msg;
    }

    @Test
    void theModuleEncryptsThePinWithTheCardNumber() throws Exception {
        ISOMsg msg = encrypted();
        verify(module).encryptPIN(PIN, PAN);
        verify(module).exportPIN(pinUnderLmk, zpk, FORMAT);
        String field52 = msg.getString(52).toUpperCase();
        assertNotEquals(ISOUtil.hexString(LMK_BLOCK), field52, "field 52 carries the LMK block; export it under the ZPK");
        assertEquals(ISOUtil.hexString(ZPK_BLOCK), field52);
        for (int i = 0; i <= msg.getMaxField(); i++) {
            if (msg.hasField(i) && i != 52) {
                assertFalse(msg.getString(i).equals(PIN), "clear PIN in field " + i);
            }
        }
    }

    @Test
    void thePinBlockGoesInAsItsRawBytes() throws Exception {
        ISOMsg msg = encrypted();
        assertArrayEquals(ZPK_BLOCK, msg.getBytes(52));
    }

    @Test
    void aMessageWithNoCardNumberIsRefusedBeforeTheModuleIsAsked() throws Exception {
        ISOMsg msg = purchase();
        msg.unset(2);
        assertThrows(IllegalArgumentException.class, () -> service().encryptPIN(msg, PIN));
        verifyNoInteractions(module);
        assertFalse(msg.hasField(52));
    }
}

package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jpos.iso.ISOBinaryField;
import org.jpos.iso.ISOComponent;
import org.jpos.iso.ISOMsg;
import org.junit.jupiter.api.Test;

class ComplexFieldsTest {

    private static final byte[] EMV = {(byte) 0x9F, 0x26, 0x08, (byte) 0xA1, (byte) 0xB2,
        (byte) 0xC3, (byte) 0xD4, (byte) 0xE5, (byte) 0xF6, (byte) 0xA7, (byte) 0xB8, 0x00};

    private static ISOMsg message() throws Exception {
        ISOMsg msg = new ISOMsg();
        msg.setMTI("0200");
        msg.set(3, "000000");
        return msg;
    }

    @Test
    void subfieldsLiveInANestedMessageNumberedAsTheirField() throws Exception {
        ISOMsg msg = message();
        ComplexFields.putSubfield(msg, 48, 1, "001");
        ComplexFields.putSubfield(msg, 62, 2, "12345");

        ISOComponent c48 = msg.getComponent(48);
        assertTrue(c48 instanceof ISOMsg, "field 48 is a nested ISOMsg");
        assertEquals(48, ((ISOMsg) c48).getKey());
        assertEquals("001", ((ISOMsg) c48).getString(1));
        assertTrue(msg.getComponent(62) instanceof ISOMsg, "field 62 is a nested ISOMsg");

        assertEquals("001", ComplexFields.getSubfield(msg, 48, 1));
        assertEquals("12345", ComplexFields.getSubfield(msg, 62, 2));
        assertEquals("000000", msg.getString(3));
    }

    @Test
    void binaryDataComesBackAsTheSameBytes() throws Exception {
        ISOMsg msg = message();
        ComplexFields.putBinary(msg, 55, EMV.clone());
        assertTrue(msg.getComponent(55) instanceof ISOBinaryField, "field 55 is binary");
        assertArrayEquals(EMV, (byte[]) msg.getValue(55));
        assertArrayEquals(EMV, ComplexFields.getBinary(msg, 55));
    }

    @Test
    void aSecondSubfieldJoinsTheFirst() throws Exception {
        ISOMsg msg = message();
        ComplexFields.putSubfield(msg, 48, 1, "001");
        ComplexFields.putSubfield(msg, 48, 2, "12345");
        assertEquals("001", ComplexFields.getSubfield(msg, 48, 1));
        assertEquals("12345", ComplexFields.getSubfield(msg, 48, 2));
    }

    @Test
    void aMissingSubfieldReadsAsNull() throws Exception {
        ISOMsg msg = message();
        assertNull(ComplexFields.getSubfield(msg, 48, 1));
        ComplexFields.putSubfield(msg, 48, 1, "001");
        assertNull(ComplexFields.getSubfield(msg, 48, 3));
    }
}

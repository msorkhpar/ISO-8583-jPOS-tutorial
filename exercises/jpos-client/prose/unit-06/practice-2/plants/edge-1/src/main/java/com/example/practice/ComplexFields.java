package com.example.practice;

import org.jpos.iso.ISOBinaryField;
import org.jpos.iso.ISOComponent;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;

/** Subfields and binary data, each stored as the component that fits it. */
public final class ComplexFields {

    private ComplexFields() {
    }

    public static void putSubfield(ISOMsg msg, int field, int subfield, String value)
            throws ISOException {
        ISOMsg nested = new ISOMsg(field);
        nested.set(subfield, value);
        msg.set(nested);
    }

    public static String getSubfield(ISOMsg msg, int field, int subfield) {
        ISOComponent component = msg.getComponent(field);
        if (!(component instanceof ISOMsg)) {
            return null;
        }
        return ((ISOMsg) component).getString(subfield);
    }

    public static void putBinary(ISOMsg msg, int field, byte[] data) throws ISOException {
        msg.set(new ISOBinaryField(field, data));
    }

    public static byte[] getBinary(ISOMsg msg, int field) throws ISOException {
        return msg.getBytes(field);
    }
}

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
        ISOComponent existing = msg.getComponent(field);
        ISOMsg nested;
        if (existing instanceof ISOMsg) {
            nested = (ISOMsg) existing;
        } else {
            nested = new ISOMsg(field);
            msg.set(nested);
        }
        nested.set(subfield, value);
    }

    public static String getSubfield(ISOMsg msg, int field, int subfield) {
        return ((ISOMsg) msg.getComponent(field)).getString(subfield);
    }

    public static void putBinary(ISOMsg msg, int field, byte[] data) throws ISOException {
        msg.set(new ISOBinaryField(field, data));
    }

    public static byte[] getBinary(ISOMsg msg, int field) throws ISOException {
        return msg.getBytes(field);
    }
}

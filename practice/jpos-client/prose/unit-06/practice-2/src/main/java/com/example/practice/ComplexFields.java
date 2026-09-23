package com.example.practice;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;

/** Subfields and binary data, each stored as the component that fits it. */
public final class ComplexFields {

    private ComplexFields() {
    }

    public static void putSubfield(ISOMsg msg, int field, int subfield, String value)
            throws ISOException {
        throw new UnsupportedOperationException("write putSubfield");
    }

    public static String getSubfield(ISOMsg msg, int field, int subfield) {
        throw new UnsupportedOperationException("write getSubfield");
    }

    public static void putBinary(ISOMsg msg, int field, byte[] data) throws ISOException {
        throw new UnsupportedOperationException("write putBinary");
    }

    public static byte[] getBinary(ISOMsg msg, int field) throws ISOException {
        throw new UnsupportedOperationException("write getBinary");
    }
}

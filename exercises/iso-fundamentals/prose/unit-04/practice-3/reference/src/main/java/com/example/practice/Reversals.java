package com.example.practice;

import org.jpos.iso.ISOMsg;

/** Builds the reversal of an earlier transaction. */
public final class Reversals {

    /** The fields that let the other side find the transaction being reversed. */
    private static final int[] IDENTIFYING = {2, 3, 4, 11, 37, 41, 42};

    private Reversals() {
    }

    /** A new 0400 carrying the fields that identify {@code original}. */
    public static ISOMsg reversalOf(ISOMsg original) throws Exception {
        String mti = original.getMTI();
        if (!"0100".equals(mti) && !"0200".equals(mti)) {
            throw new IllegalArgumentException("only an 0100 or 0200 request can be reversed, not " + mti);
        }
        ISOMsg reversal = new ISOMsg();
        reversal.setMTI("0400");
        for (int field : IDENTIFYING) {
            if (original.hasField(field)) {
                reversal.set(field, original.getString(field));
            }
        }
        return reversal;
    }
}

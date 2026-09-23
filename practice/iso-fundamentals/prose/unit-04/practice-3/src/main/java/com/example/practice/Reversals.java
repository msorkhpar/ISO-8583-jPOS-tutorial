package com.example.practice;

import org.jpos.iso.ISOMsg;

/** Builds the reversal of an earlier transaction. */
public final class Reversals {

    private Reversals() {
    }

    /** A new 0400 carrying the fields that identify {@code original}. */
    public static ISOMsg reversalOf(ISOMsg original) throws Exception {
        throw new UnsupportedOperationException("write reversalOf");
    }
}

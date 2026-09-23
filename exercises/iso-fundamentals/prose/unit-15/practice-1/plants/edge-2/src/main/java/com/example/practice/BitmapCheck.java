package com.example.practice;

import java.util.ArrayList;
import java.util.List;
import org.jpos.iso.ISOMsg;

/** Where a primary bitmap and the message it was captured with disagree. */
public final class BitmapCheck {

    private BitmapCheck() {
    }

    /** Every data element, 2 to 64, the bitmap and the message disagree on, ascending. */
    public static List<Integer> mismatches(String bitmapHex, ISOMsg message) {
        long bits = new java.math.BigInteger(bitmapHex, 16).longValue();
        List<Integer> found = new ArrayList<>();
        for (int field = 2; field <= 64; field++) {
            boolean claimed = (bits & (1L << (64 - field))) != 0;
            if (claimed != message.hasField(field)) {
                found.add(field);
            }
        }
        return found;
    }
}

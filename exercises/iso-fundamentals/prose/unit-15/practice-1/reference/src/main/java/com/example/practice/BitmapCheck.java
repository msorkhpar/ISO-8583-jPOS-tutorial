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
        if (bitmapHex == null || !bitmapHex.matches("[0-9A-Fa-f]{16}")) {
            throw new IllegalArgumentException("a primary bitmap is sixteen hexadecimal digits");
        }
        long bits = Long.parseUnsignedLong(bitmapHex, 16);
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

package com.example.practice;

import java.util.List;
import org.jpos.iso.ISOMsg;

/** Where a primary bitmap and the message it was captured with disagree. */
public final class BitmapCheck {

    private BitmapCheck() {
    }

    /** Every data element, 2 to 64, the bitmap and the message disagree on, ascending. */
    public static List<Integer> mismatches(String bitmapHex, ISOMsg message) {
        throw new UnsupportedOperationException("write mismatches");
    }
}

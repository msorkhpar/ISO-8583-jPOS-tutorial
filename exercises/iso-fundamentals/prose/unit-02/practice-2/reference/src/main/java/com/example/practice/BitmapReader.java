package com.example.practice;

import java.util.ArrayList;
import java.util.List;

/** Which data elements a message's bitmaps say are present. */
public final class BitmapReader {

    private BitmapReader() {
    }

    /** Every data element the primary and any secondary bitmap flag, ascending. */
    public static List<Integer> present(String bitmapHex) {
        if (bitmapHex == null || !bitmapHex.matches("[0-9A-Fa-f]{16}([0-9A-Fa-f]{16})?")) {
            throw new IllegalArgumentException("a bitmap is sixteen or thirty-two hexadecimal digits");
        }
        long primary = Long.parseUnsignedLong(bitmapHex.substring(0, 16), 16);
        boolean secondaryFollows = primary < 0;
        if (secondaryFollows != (bitmapHex.length() == 32)) {
            throw new IllegalArgumentException("bit 1 and the bitmap's length disagree");
        }
        List<Integer> found = new ArrayList<>();
        for (int bit = 2; bit <= 64; bit++) {
            if ((primary & (1L << (64 - bit))) != 0) {
                found.add(bit);
            }
        }
        if (secondaryFollows) {
            long secondary = Long.parseUnsignedLong(bitmapHex.substring(16), 16);
            for (int bit = 1; bit <= 64; bit++) {
                if ((secondary & (1L << (64 - bit))) != 0) {
                    found.add(64 + bit);
                }
            }
        }
        return found;
    }
}

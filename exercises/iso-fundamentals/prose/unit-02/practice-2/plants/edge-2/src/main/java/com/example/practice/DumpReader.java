package com.example.practice;

import java.util.ArrayList;
import java.util.List;

/**
 * The start of an ISO 8583 message captured on a hex-ASCII link, as support pastes it into a
 * ticket: the MTI, the bitmaps written as hexadecimal characters, then the fields.
 */
public final class DumpReader {

    private final String dump;

    public DumpReader(String dump) {
        if (dump == null || dump.length() < 20) {
            throw new IllegalArgumentException("too short to hold an MTI and a bitmap");
        }
        this.dump = dump;
    }

    /** The message type indicator. */
    public String mti() {
        return dump.substring(0, 4);
    }

    /** Every field the capture's bitmaps say is present, in ascending order. */
    public List<Integer> fields() {
        List<Integer> found = new ArrayList<>();
        for (int b = 2; b <= 64; b++) {
            if (bit(4, b)) {
                found.add(b);
            }
        }
        if (bit(4, 1)) {
            if (bit(20, 1)) {
                throw new IllegalArgumentException("a third bitmap follows, and this tool reads two");
            }
            for (int b = 2; b <= 64; b++) {
                if (bit(20, b)) {
                    found.add(64 + b);
                }
            }
        }
        return found;
    }

    /** The index in the capture where the characters of the first field begin. */
    public int dataStart() {
        return 20;                                     // after the MTI and the bitmap
    }

    /** Whether bit {@code n}, numbered as the standard numbers it, is set in the bitmap at {@code from}. */
    boolean bit(int from, int n) {
        int digit = Character.digit(dump.charAt(from + (n - 1) / 4), 16);
        return (digit & (8 >> ((n - 1) % 4))) != 0;
    }
}

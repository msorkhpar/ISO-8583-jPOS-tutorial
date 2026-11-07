package com.example.practice;

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
        // TODO: the list a support engineer checks against what the sender says it sent.
        //       Refuse a capture this tool can't read completely (see the statement).
        throw new IllegalStateException("TODO: fields");
    }

    /** The index in the capture where the characters of the first field begin. */
    public int dataStart() {
        // TODO: where reading the fields would start.
        throw new IllegalStateException("TODO: dataStart");
    }

    /** Whether bit {@code n}, numbered as the standard numbers it, is set in the bitmap at {@code from}. */
    boolean bit(int from, int n) {
        // TODO: from is the index of the bitmap's first hexadecimal character in the capture.
        throw new IllegalStateException("TODO: bit");
    }
}

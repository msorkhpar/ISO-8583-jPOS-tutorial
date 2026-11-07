package com.example.practice;

import java.io.ByteArrayOutputStream;

/**
 * Reads a hex dump pasted from a ticket, a log or a chat back into the bytes it shows, and refuses
 * a dump it cannot read with certainty.
 */
public final class HexDumpReader {
    private HexDumpReader() { }

    /** One line of a dump: the offset it states, and the bytes in its byte columns. */
    record Line(int offset, byte[] bytes) { }

    public static byte[] read(String dump) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (String text : dump.split("\r?\n")) {
            if (text.isBlank())
                continue;
            Line line = parse(text);
            check(line, out.size());
            out.writeBytes(line.bytes());
        }
        return out.toByteArray();
    }

    /** The offset and the bytes of one non-blank line. */
    static Line parse(String text) {
        int start = 0;
        while (start < text.length() && text.charAt(start) == ' ')
            start++;
        int end = start;
        while (end < text.length() && Character.digit(text.charAt(end), 16) >= 0)
            end++;
        if (end - start != 4 && end - start != 8)
            throw new IllegalArgumentException("no offset: " + text);
        int offset = Integer.parseInt(text.substring(start, end), 16);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        for (int i = 0; i < 16; i++) {
            int col = end + 2 + 3 * i + (i >= 8 ? 1 : 0);
            String pos = col + 2 <= text.length() ? text.substring(col, col + 2) : "  ";
            if (pos.isBlank())
                break;
            int hi = "0123456789ABCDEF".indexOf(pos.charAt(0)), lo = "0123456789ABCDEF".indexOf(pos.charAt(1));
            if (hi < 0 || lo < 0)
                throw new IllegalArgumentException("not a byte at column " + col + ": " + text);
            bytes.write(hi * 16 + lo);
        }
        return new Line(offset, bytes.toByteArray());
    }

    /** Refuses a line that does not continue the bytes read so far. */
    static void check(Line line, int readSoFar) {
        if (line.offset() != readSoFar)
            throw new IllegalArgumentException("line at offset " + Integer.toHexString(line.offset())
                + " after " + readSoFar + " bytes");
    }
}

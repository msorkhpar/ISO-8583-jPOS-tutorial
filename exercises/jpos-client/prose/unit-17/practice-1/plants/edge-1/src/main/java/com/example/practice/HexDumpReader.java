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
        for (String word : text.substring(end).trim().split("\\s+")) {      // every hex pair after the offset
            if (word.length() == 2 && Character.digit(word.charAt(0), 16) >= 0 && Character.digit(word.charAt(1), 16) >= 0)
                bytes.write(Integer.parseInt(word, 16));
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

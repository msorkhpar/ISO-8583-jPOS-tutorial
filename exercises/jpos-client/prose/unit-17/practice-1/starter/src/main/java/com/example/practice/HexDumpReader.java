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
        // TODO: The dumps come from jPOS's hexdump, from `hexdump -C` and from logs and chats.
        //       Return exactly the bytes this line shows, with the offset it states, or throw
        //       IllegalArgumentException when the line can't be read with certainty.
        throw new IllegalStateException("TODO: parse");
    }

    /** Refuses a line that does not continue the bytes read so far. */
    static void check(Line line, int readSoFar) {
        // TODO: Throw IllegalArgumentException when this line can't be the next one.
        throw new IllegalStateException("TODO: check");
    }
}

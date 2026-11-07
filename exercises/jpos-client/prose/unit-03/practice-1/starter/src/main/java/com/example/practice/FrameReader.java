package com.example.practice;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Reads the messages of a host whose interface spec says: "Every message is preceded by its
 * length in two bytes, binary, most significant byte first. The length does not include the two
 * length bytes. When the link is idle the host sends a message of length zero."
 */
public class FrameReader {
    private final InputStream in;

    public FrameReader(InputStream in) {
        this.in = in;
    }

    /** The bytes of the host's next message, without its length. Blocks until it is complete. */
    public byte[] next() throws IOException {
        // TODO: return the next message the host meant for us, and leave everything after it in
        //       the stream for the next call. (about 4 lines)
        throw new IllegalStateException("TODO: next");
    }

    /** The length announced in front of a message. */
    int readLength() throws IOException {
        // TODO: the two length bytes, as the number the host meant. (about 2 lines)
        throw new IllegalStateException("TODO: readLength");
    }

    /** The next n bytes of the stream. */
    byte[] readExactly(int n) throws IOException {
        // TODO: all n of them, or an EOFException if the stream ends first. (about 8 lines)
        throw new IllegalStateException("TODO: readExactly");
    }
}

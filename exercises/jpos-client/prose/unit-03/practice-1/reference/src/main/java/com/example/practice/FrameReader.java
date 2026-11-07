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
        int length = readLength();
        while (length == 0)
            length = readLength();
        return readExactly(length);
    }

    /** The length announced in front of a message. */
    int readLength() throws IOException {
        byte[] h = readExactly(2);
        return (h[0] & 0xFF) << 8 | (h[1] & 0xFF);
    }

    /** The next n bytes of the stream. */
    byte[] readExactly(int n) throws IOException {
        byte[] b = new byte[n];
        int got = 0;
        while (got < n) {
            int r = in.read(b, got, n - got);
            if (r < 0)
                throw new EOFException("stream ended after " + got + " of " + n + " bytes");
            got += r;
        }
        return b;
    }
}

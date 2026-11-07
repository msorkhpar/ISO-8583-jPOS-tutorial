package com.example.practice;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * Sits on a BASE I connection as a pass-through for the support desk: splits what the network
 * sends into frames, each one the header bytes and the message bytes, and copes with whatever
 * else arrives on the link the way jPOS's VAPChannel does.
 */
public final class Base1Frames {

    /** One frame: every header byte in front of the message, and the message from its MTI on. */
    public record Frame(byte[] header, byte[] message) {
        @Override
        public boolean equals(Object o) {
            return o instanceof Frame f && Arrays.equals(header, f.header) && Arrays.equals(message, f.message);
        }

        @Override
        public int hashCode() {
            return 31 * Arrays.hashCode(header) + Arrays.hashCode(message);
        }

        @Override
        public String toString() {
            return "Frame[header=" + hex(header) + ", message=" + hex(message) + "]";
        }
    }

    private final DataInputStream in;
    private final OutputStream out;

    /** {@code in} is what the network sends; {@code out} is the way back to the network. */
    public Base1Frames(InputStream in, OutputStream out) {
        this.in = new DataInputStream(in);
        this.out = out;
    }

    /** The next frame that carries a message, or null when the capture ends between frames. */
    public Frame next() throws IOException {
        while (true) {
            byte[] prefix = new byte[4];
            if (!readPrefix(prefix))
                return null;
            if (poll(prefix))
                continue;
            byte[] header = readHeaders();
            byte[] message = new byte[messageLength(prefix, header)];
            in.readFully(message);
            return new Frame(header, message);
        }
    }

    /** True when this prefix is not followed by a message; handles whatever such a prefix needs. */
    boolean poll(byte[] prefix) throws IOException {
        // TODO: decide whether this prefix starts a frame with no message behind it, and do for
        //       such a frame what the network expects of you. Return true when nothing follows.
        throw new IllegalStateException("TODO: poll");
    }

    /** Every header byte between the prefix and the message. */
    byte[] readHeaders() throws IOException {
        // TODO: read every header byte that comes before the message, whatever kind of frame this is.
        throw new IllegalStateException("TODO: readHeaders");
    }

    /** How many message bytes follow the headers. */
    int messageLength(byte[] prefix, byte[] header) {
        // TODO: how many message bytes follow the headers you read.
        throw new IllegalStateException("TODO: messageLength");
    }

    /** The length the prefix announces. */
    static int length(byte[] prefix) {
        return (prefix[0] & 0xFF) << 8 | (prefix[1] & 0xFF);
    }

    /** Reads a 4-byte prefix; false when the capture has ended before it. */
    private boolean readPrefix(byte[] prefix) throws IOException {
        int first = in.read();
        if (first < 0)
            return false;
        prefix[0] = (byte) first;
        in.readFully(prefix, 1, 3);
        return true;
    }

    static String hex(byte[] b) {
        StringBuilder s = new StringBuilder();
        for (byte x : b)
            s.append(String.format("%02X", x));
        return s.toString();
    }
}

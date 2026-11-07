package com.example.practice;

import java.util.ArrayList;
import java.util.List;
import org.jpos.iso.ISOUtil;

/**
 * Prints chip data (DE 55) from a hex dump for the support desk: splits the bytes into their
 * BER-TLV objects, and refuses data it can't split rather than guess.
 */
public final class TlvReader {

    /** One BER-TLV object: its tag and its value, both as upper-case hexadecimal. */
    public record Tlv(String tag, String value) { }

    /** A length read from the data, and where the value starts. */
    record Length(int value, int next) { }

    private TlvReader() {
    }

    /** Every TLV object in {@code data}, in the order they appear. */
    public static List<Tlv> read(byte[] data) {
        List<Tlv> found = new ArrayList<>();
        int at = 0;
        while (at < data.length) {
            int end = tagEnd(data, at);
            String tag = ISOUtil.hexString(data, at, end - at);
            Length length = lengthAt(data, end);
            byte[] value = value(data, length.next(), length.value());
            found.add(new Tlv(tag, ISOUtil.hexString(value)));
            at = length.next() + length.value();
        }
        return found;
    }

    /** The index just after the tag that starts at {@code start}. */
    static int tagEnd(byte[] data, int start) {
        // TODO: find where the tag that starts here ends. Use byteAt to read the data.
        throw new IllegalStateException("TODO: tagEnd");
    }

    /** The length that starts at {@code at}, and the index where the value begins. */
    static Length lengthAt(byte[] data, int at) {
        // TODO: read the value's length, for values of any size up to 65535 bytes, and say where
        //       the value starts. Refuse a length you can't read.
        throw new IllegalStateException("TODO: lengthAt");
    }

    /** The {@code length} bytes of a value that starts at {@code from}. */
    static byte[] value(byte[] data, int from, int length) {
        // TODO: the value's bytes, as the data holds them.
        throw new IllegalStateException("TODO: value");
    }

    /** One byte of the data; refuses to read past its end. */
    static byte byteAt(byte[] data, int index) {
        if (index >= data.length)
            throw new IllegalArgumentException("the data ends inside a tag or a length");
        return data[index];
    }
}

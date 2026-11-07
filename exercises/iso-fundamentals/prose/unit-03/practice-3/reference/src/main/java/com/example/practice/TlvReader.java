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
        int at = start;
        if ((byteAt(data, at++) & 0x1F) != 0x1F)
            return at;
        while ((byteAt(data, at++) & 0x80) != 0) {
            // a following byte with its top bit set says one more byte follows
        }
        return at;
    }

    /** The length that starts at {@code at}, and the index where the value begins. */
    static Length lengthAt(byte[] data, int at) {
        int first = byteAt(data, at) & 0xFF;
        if (first < 0x80)
            return new Length(first, at + 1);
        if (first == 0x81)
            return new Length(byteAt(data, at + 1) & 0xFF, at + 2);
        if (first == 0x82)
            return new Length((byteAt(data, at + 1) & 0xFF) << 8 | (byteAt(data, at + 2) & 0xFF), at + 3);
        throw new IllegalArgumentException("unsupported length byte " + Integer.toHexString(first));
    }

    /** The {@code length} bytes of a value that starts at {@code from}. */
    static byte[] value(byte[] data, int from, int length) {
        if (from + length > data.length)
            throw new IllegalArgumentException(length + " bytes announced, " + (data.length - from) + " left");
        byte[] v = new byte[length];
        System.arraycopy(data, from, v, 0, length);
        return v;
    }

    /** One byte of the data; refuses to read past its end. */
    static byte byteAt(byte[] data, int index) {
        if (index >= data.length)
            throw new IllegalArgumentException("the data ends inside a tag or a length");
        return data[index];
    }
}

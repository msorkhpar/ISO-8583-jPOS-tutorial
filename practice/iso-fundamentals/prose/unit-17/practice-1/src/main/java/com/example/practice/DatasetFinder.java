package com.example.practice;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;
import org.jpos.tlv.TLVList;

/** Finds one element of an ISO 8583:2023 message by its path, such as 34.0x01.0xDF10. */
public final class DatasetFinder {

    private DatasetFinder() {
    }

    /** The bytes the path names, as upper-case hex, or null when the message doesn't have them. */
    public static String find(ISOMsg message, String path) {
        String[] parts = path.split("\\.", -1);
        if (parts.length < 2 || parts.length > 3)
            throw new IllegalArgumentException("not a dataset path: " + path);
        int field = Integer.parseInt(parts[0]);
        int first = hex(parts[1]);
        Integer second = parts.length == 3 ? hex(parts[2]) : null;

        if (!hasEnvelope(field)) {
            if (second != null)
                throw new IllegalArgumentException("field " + field + " has one level below it: " + path);
            byte[] tlv = message.getBytes(field);
            return tlv == null ? null : tag(tlv, first);
        }
        if (second != null && !isTlvDataset(first))
            throw new IllegalArgumentException("dataset " + parts[1] + " needs its definition: " + path);
        byte[] bytes = message.getBytes(field);
        byte[] content = bytes == null ? null : content(bytes, first);
        if (content == null)
            return null;
        return second == null ? ISOUtil.hexString(content) : tag(content, second);
    }

    /** Whether the field's bytes are a row of datasets, as opposed to plain BER-TLV. */
    static boolean hasEnvelope(int field) {
        // TODO: Decide which fields find() should walk as datasets. Every field this tool is used
        //       on is a composite field of the 2023 edition.
        throw new IllegalStateException("TODO: hasEnvelope");
    }

    /** Whether a dataset with this identifier holds BER-TLV elements. */
    static boolean isTlvDataset(int id) {
        // TODO: find() may look up a tag only in a dataset whose content is BER-TLV.
        throw new IllegalStateException("TODO: isTlvDataset");
    }

    /** The content of the first dataset with this identifier, or null when the field has none. */
    static byte[] content(byte[] field, int id) {
        // TODO: Return the content of the dataset with this identifier, or null when the field has
        //       none. A field from another system can be damaged: refuse what can't be a correct
        //       field with an IllegalArgumentException rather than return a guess.
        throw new IllegalStateException("TODO: content");
    }

    private static int hex(String s) {
        if (!s.startsWith("0x") || s.length() < 3)
            throw new IllegalArgumentException("expected 0x and hex digits: " + s);
        return Integer.parseInt(s.substring(2), 16);
    }

    private static String tag(byte[] tlvBytes, int tag) {
        TLVList tlv = new TLVList();
        tlv.unpack(tlvBytes);
        return tlv.hasTag(tag) ? tlv.getString(tag) : null;
    }
}

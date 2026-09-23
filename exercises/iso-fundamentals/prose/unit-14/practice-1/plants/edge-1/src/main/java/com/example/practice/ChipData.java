package com.example.practice;

import java.util.LinkedHashMap;
import java.util.Map;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;

/** Splits the EMV data in field 55 into its tag-length-value items. */
public final class ChipData {

    private ChipData() {
    }

    /** Every tag in field 55 with its value, both upper-case hex, in the order they appear. */
    public static Map<String, String> tags(ISOMsg message) throws ISOException {
        Map<String, String> found = new LinkedHashMap<>();
        if (!message.hasField(55)) {
            return found;
        }
        byte[] data = message.getBytes(55);
        int at = 0;
        while (at < data.length) {
            int tagStart = at;
            int tagLength = 1;
            if (at + tagLength + 1 > data.length) {
                throw new ISOException("field 55 ends inside an item at byte " + at);
            }
            String tag = ISOUtil.hexString(data, tagStart, tagLength).toUpperCase();
            at += tagLength;
            int length = data[at++] & 0xFF;
            if (at + length > data.length) {
                throw new ISOException("tag " + tag + " claims " + length + " bytes past the end of field 55");
            }
            found.put(tag, ISOUtil.hexString(data, at, length).toUpperCase());
            at += length;
        }
        return found;
    }
}

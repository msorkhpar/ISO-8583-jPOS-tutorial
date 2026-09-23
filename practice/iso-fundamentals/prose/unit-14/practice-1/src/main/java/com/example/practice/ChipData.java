package com.example.practice;

import java.util.Map;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;

/** Splits the EMV data in field 55 into its tag-length-value items. */
public final class ChipData {

    private ChipData() {
    }

    /** Every tag in field 55 with its value, both upper-case hex, in the order they appear. */
    public static Map<String, String> tags(ISOMsg message) throws ISOException {
        throw new UnsupportedOperationException("write tags");
    }
}

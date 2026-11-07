package com.example.practice;

import java.util.ArrayList;
import java.util.List;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;
import org.jpos.tlv.TLVList;

/** Checks that the chip data in DE 55 agrees with the ISO fields of the same message. */
public final class ChipConsistency {

    /** The chip tags that mirror an ISO field, in the order they are reported. */
    static final int[] MIRRORED = { 0x9F02, 0x5F2A, 0x9C, 0x9A };

    private ChipConsistency() {
    }

    /** The tags whose value disagrees with the ISO field it mirrors, as hex strings ("9F02"), in MIRRORED order. */
    public static List<String> mismatches(ISOMsg message) {
        List<String> out = new ArrayList<>();
        byte[] de55 = message.getBytes(55);
        if (de55 == null)
            return out;
        for (int tag : MIRRORED) {
            String chip = chipDigits(de55, tag);
            if (chip == null)
                continue;
            Boolean agree = agrees(tag, chip, message);
            if (Boolean.FALSE.equals(agree))
                out.add(Integer.toHexString(tag).toUpperCase());
        }
        return out;
    }

    /** The digits of a numeric chip value in DE 55, or null when the tag is not there. */
    static String chipDigits(byte[] de55, int tag) {
        // TODO: find the tag in DE 55 and return the digits its value holds.
        throw new IllegalStateException("TODO: chipDigits");
    }

    /**
     * Whether the chip value's digits agree with the ISO field that tag mirrors, or null when the
     * message lacks that field.
     */
    static Boolean agrees(int tag, String chip, ISOMsg m) {
        // TODO: say whether the chip digits match the field this tag mirrors, or null when the
        //       message doesn't carry that field.
        throw new IllegalStateException("TODO: agrees");
    }
}

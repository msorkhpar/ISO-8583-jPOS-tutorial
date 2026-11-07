package com.example.practice;

import java.util.Map;
import java.util.TreeMap;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOPackager;

/** Checks a layout against one of the sample messages in a host's specification. */
public final class LayoutCheck {
    private LayoutCheck() {
    }

    /**
     * The row of the layout to fix first: the number of the field to look at, or 0 when the
     * packager reads the sample exactly as the specification documents it.
     *
     * @param packager   the layout under test
     * @param sample     the sample message's bytes, from the specification
     * @param documented every field of the sample with the value the specification gives for it
     */
    public static int firstWrongField(ISOPackager packager, byte[] sample, Map<Integer, String> documented) {
        ISOMsg read = new ISOMsg();
        read.setPackager(packager);
        try {
            read.unpack(sample);
            return 0;                                       // it read without an error
        } catch (ISOException e) {
            // keep what was read before the packager stopped
        }
        for (Map.Entry<Integer, String> field : new TreeMap<>(documented).entrySet()) {
            if (!field.getValue().equals(read.getString(field.getKey())))
                return field.getKey();
        }
        return 0;
    }
}

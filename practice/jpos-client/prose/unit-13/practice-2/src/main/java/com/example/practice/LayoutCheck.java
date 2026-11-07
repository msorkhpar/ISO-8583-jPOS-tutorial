package com.example.practice;

import java.util.Map;

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
        // TODO: Certification asks one thing of this method: which row of the layout to correct
        //       first. Use the sample's bytes, and every value the specification documents.
        throw new IllegalStateException("TODO: firstWrongField");
    }
}

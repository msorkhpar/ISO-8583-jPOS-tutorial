package com.example.practice;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;

/** Makes a message fit to show: its PAN masked as PCI-DSS requires. */
public final class CardMasking {

    private CardMasking() {
    }

    /** A copy of the message with its PAN masked for display. */
    public static ISOMsg forDisplay(ISOMsg message) throws ISOException {
        throw new UnsupportedOperationException("write forDisplay");
    }
}

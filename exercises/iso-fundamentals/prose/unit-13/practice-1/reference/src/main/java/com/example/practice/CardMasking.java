package com.example.practice;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;

/** Makes a message fit to show: its PAN masked as PCI-DSS requires. */
public final class CardMasking {

    private CardMasking() {
    }

    /** A copy of the message with its PAN masked for display. */
    public static ISOMsg forDisplay(ISOMsg message) throws ISOException {
        ISOMsg shown = (ISOMsg) message.clone();
        if (shown.hasField(2)) {
            String pan = shown.getString(2);
            shown.set(2, pan.substring(0, 6) + "*".repeat(pan.length() - 10) + pan.substring(pan.length() - 4));
        }
        return shown;
    }
}

package com.example.practice;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;

/**
 * The gateway's first look at a terminal's request. A request whose field 22 and card fields
 * contradict each other is answered here, with response code 30, and never goes upstream.
 */
public class EntryModeScreen {
    /** The PAN-entry codes in the acquirer's specification: the first two digits of field 22. */
    static final String KEYED = "01", STRIPE = "90", CHIP = "05", CONTACTLESS_CHIP = "07";

    /** The answer for the terminal when the request can't go upstream, or null when it can. */
    public ISOMsg screen(ISOMsg fromTerminal) throws ISOException {
        String problem = problem(fromTerminal);
        if (problem == null)
            return null;
        ISOMsg refusal = (ISOMsg) fromTerminal.clone(new int[] { 0, 3, 4, 11, 41, 42 });
        refusal.setResponseMTI();
        refusal.set(39, "30");
        return refusal;
    }

    /** Why field 22 and the card fields disagree, or null when they tell the same story. */
    String problem(ISOMsg m) {
        String entry = m.getString(22);
        if (entry == null || entry.length() != 3)
            return "no entry mode";
        switch (entry) {
            case KEYED + "1":
                return m.hasField(14) && !m.hasField(35) && !m.hasField(55) ? null : "keyed, but card fields disagree";
            case STRIPE + "1":
                return m.hasField(35) && !m.hasField(55) ? null : "stripe read, but card fields disagree";
            case CHIP + "1":
            case CONTACTLESS_CHIP + "1":
                return m.hasField(35) && m.hasField(55) ? null : "chip read, but card fields disagree";
            default:
                return "entry mode " + entry + " is not in the specification";
        }
    }
}

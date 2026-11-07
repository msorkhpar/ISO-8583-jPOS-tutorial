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
        // TODO: Return null only when the card fields the request carries (14, 35, 55) are the
        //       ones the read named in field 22 produces, and nothing else. Anything this
        //       acquirer's specification doesn't list is a problem too. The text you return is
        //       for the gateway's log; the tests only look at whether there is one.
        throw new IllegalStateException("TODO: problem");
    }
}

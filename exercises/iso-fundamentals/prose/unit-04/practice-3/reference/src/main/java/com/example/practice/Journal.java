package com.example.practice;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * The issuer's record of the requests it has processed, as they arrived from the acquirers'
 * links, and what happens when a reversal arrives.
 */
public class Journal {
    private final List<ISOMsg> processed = new ArrayList<>();
    private final List<ISOMsg> reversed = new ArrayList<>();

    /** Records a request the issuer has processed. */
    public synchronized void record(ISOMsg request) {
        processed.add(request);
    }

    /** Whether {@code request}, as recorded, has been reversed. */
    public synchronized boolean isReversed(ISOMsg request) {
        return reversed.contains(request);
    }

    /** Handles a reversal from an acquirer and returns the acknowledgement to send back. */
    public synchronized ISOMsg onReversal(ISOMsg reversal) throws ISOException {
        ISOMsg original = originalOf(reversal);
        if (original != null && !reversed.contains(original))
            reversed.add(original);                 // a real host gives the money back here
        ISOMsg ack = (ISOMsg) reversal.clone();
        ack.setResponseMTI();
        ack.set(39, "00");
        return ack;
    }

    /** The recorded request that {@code reversal} cancels, or null when the issuer never saw it. */
    ISOMsg originalOf(ISOMsg reversal) throws ISOException {
        String points = reversal.getString(90);
        for (ISOMsg o : processed) {
            String names = o.getMTI()
                    + ISOUtil.zeropad(o.getString(11), 6)
                    + ISOUtil.zeropad(o.getString(7), 10)
                    + ISOUtil.zeropad(o.hasField(32) ? o.getString(32) : "", 11)
                    + ISOUtil.zeropad(o.hasField(33) ? o.getString(33) : "", 11);
            if (names.equals(points))
                return o;
        }
        return null;
    }
}

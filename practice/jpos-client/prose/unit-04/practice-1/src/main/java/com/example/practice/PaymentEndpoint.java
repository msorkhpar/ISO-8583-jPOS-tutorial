package com.example.practice;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.MUX;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The purchase endpoint of a payment API. It forwards each purchase to the host through a MUX and
 * answers the API caller with an HTTP status code.
 */
public class PaymentEndpoint {
    public static final int APPROVED = 200, DECLINED = 402, HOST_UNAVAILABLE = 503, CHECK_LATER = 504;

    private final MUX mux;
    private final long timeout;
    /** Purchases whose outcome nobody knows. A reversal job (unit c12) works through this list. */
    final List<ISOMsg> unknown = new CopyOnWriteArrayList<>();

    public PaymentEndpoint(MUX mux, long timeout) {
        this.mux = mux;
        this.timeout = timeout;
    }

    public int handle(ISOMsg purchase) throws ISOException {
        // TODO: 503 is for a purchase that never left: the caller may retry it at once, and there
        //       is nothing to undo. Make sure handle() only says 503 when that is true. (two lines)
        ISOMsg response = mux.request(purchase, timeout);
        return statusOf(response, purchase);
    }

    /** The status for the API caller, once the wait for the host's answer is over. */
    int statusOf(ISOMsg response, ISOMsg purchase) {
        // TODO: 200 approved, 402 declined, 504 when nobody can say yet. A purchase whose outcome
        //       nobody knows also goes on `unknown`, exactly once. (about five lines)
        throw new IllegalStateException("TODO: statusOf");
    }
}

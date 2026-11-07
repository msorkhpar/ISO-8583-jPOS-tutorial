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
        if (!mux.isConnected())
            return HOST_UNAVAILABLE;
        ISOMsg response = mux.request(purchase, timeout);
        if (response == null)
            response = mux.request(purchase, timeout);   // one more try before giving up
        return statusOf(response, purchase);
    }

    /** The status for the API caller, once the wait for the host's answer is over. */
    int statusOf(ISOMsg response, ISOMsg purchase) {
        if (response == null) {
            unknown.add(purchase);
            return CHECK_LATER;
        }
        return "00".equals(response.getString(39)) ? APPROVED : DECLINED;
    }
}

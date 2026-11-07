package com.example.practice;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.MUX;
import org.jpos.space.Space;

/** Sends a store's purchases to the acquirer through a MUXPool over several links. */
public class Checkout {
    public enum Outcome { APPROVED, DECLINED, UNAVAILABLE, UNKNOWN }

    /** Purchases waiting to be reversed; the reversal sender of unit c12 takes them from here. */
    public static final String TO_REVERSE = "to-reverse";

    private final MUX pool;
    private final Space<String, Object> sp;
    private final long timeout;

    public Checkout(MUX pool, Space<String, Object> sp, long timeout) {
        this.pool = pool;
        this.sp = sp;
        this.timeout = timeout;
    }

    /** Sends one purchase and says what became of it. */
    public Outcome pay(ISOMsg purchase) throws ISOException {
        if (!canSend())
            return Outcome.UNAVAILABLE;
        ISOMsg response = pool.request(purchase, timeout);
        if (response == null)
            return onNoAnswer(purchase);
        return "00".equals(response.getString(39)) ? Outcome.APPROVED : Outcome.DECLINED;
    }

    /** Whether the purchase should be sent at all. */
    boolean canSend() {
        // TODO: Decide before anything goes out. A purchase that is never sent can be reported
        //       to the cardholder at once and needs nothing afterwards.
        throw new IllegalStateException("TODO: canSend");
    }

    /** The purchase went out and no answer came back in time. */
    Outcome onNoAnswer(ISOMsg purchase) throws ISOException {
        // TODO: The purchase left on one of the links and nothing came back. Return the outcome
        //       the store must show, and make sure the money ends up where it belongs.
        throw new IllegalStateException("TODO: onNoAnswer");
    }
}

package com.example.practice;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.q2.iso.QMUX;
import org.jpos.space.Space;

/**
 * Authorizes purchases over one link to a host that wants a sign-on on every connection. The
 * sign-on manager of the deployment keeps its marks in {@code sp}, as on the page.
 */
public class CheckoutClient {
    public enum Outcome { APPROVED, DECLINED, NO_ANSWER, HOST_UNAVAILABLE }

    private final QMUX mux;
    private final Space<String, Object> sp;
    private final long timeout;
    private final String readyKey;      // the adaptor's ready entry, e.g. "host-channel.ready"
    private final String enabledKey;    // the sign-on manager's mark for this QMUX

    public CheckoutClient(QMUX mux, Space<String, Object> sp, long timeout) {
        this.mux = mux;
        this.sp = sp;
        this.timeout = timeout;
        this.readyKey = mux.getReadyIndicatorNames()[0];
        this.enabledKey = mux.getName() + ".enabled";
    }

    public Outcome authorize(ISOMsg purchase) throws ISOException {
        if (!signedOn())
            return Outcome.HOST_UNAVAILABLE;           // nothing left our side
        ISOMsg response = mux.request(purchase, timeout);
        if (response == null)
            return Outcome.NO_ANSWER;
        return "00".equals(response.getString(39)) ? Outcome.APPROVED : Outcome.DECLINED;
    }

    /** True only when a purchase sent now would reach a host that takes it. */
    boolean signedOn() {
        return mux.isConnected() && sp.rdp(enabledKey) != null;
    }
}

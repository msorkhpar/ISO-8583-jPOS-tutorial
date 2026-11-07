package com.example.practice;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOPackager;
import org.jpos.iso.MUX;
import org.jpos.space.Space;

/**
 * Sends one message to the host and says how it ended. A message whose outcome is unknown goes
 * onto {@code saf.host}, where the reversal process picks it up.
 */
public class PaymentClient {
    public enum Outcome { APPROVED, DECLINED, REFUSED, UNAVAILABLE, UNKNOWN }

    static final String SAF = "saf.host";

    private final MUX mux;
    private final Space<String, Object> sp;
    private final ISOPackager packager;          // the packager the link's channel uses
    private final long timeout;

    public PaymentClient(MUX mux, Space<String, Object> sp, ISOPackager packager, long timeout) {
        this.mux = mux;
        this.sp = sp;
        this.packager = packager;
        this.timeout = timeout;
    }

    public Outcome pay(ISOMsg m) throws ISOException {
        if (refusal(m) != null)
            return Outcome.REFUSED;
        if (!mux.isConnected())
            return Outcome.UNAVAILABLE;
        ISOMsg response = mux.request(m, timeout);
        if (response == null)
            return unknown(m);
        return "00".equals(response.getString(39)) ? Outcome.APPROVED : Outcome.DECLINED;
    }

    /** Why this message must not go to the link, or null when it may. */
    String refusal(ISOMsg m) {
        try {
            ISOMsg copy = (ISOMsg) m.clone();
            copy.setPackager(packager);
            copy.pack();
            return null;
        } catch (ISOException e) {
            return e.getMessage();
        }
    }

    /** Given: nobody knows what the host did, so the reversal process takes over. */
    Outcome unknown(ISOMsg m) {
        sp.out(SAF, m);
        return Outcome.UNKNOWN;
    }
}

package com.example.practice;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOPackager;
import org.jpos.iso.MUX;
import org.jpos.space.Space;

import java.util.Map;

/**
 * Sends one message to the host and says how it ended. A message whose outcome is unknown goes
 * onto {@code saf.host}, where the reversal process picks it up. Answers that the developers must
 * look at go onto {@code alerts}.
 */
public class PaymentClient {
    public enum Outcome { APPROVED, DECLINED, TRY_LATER, REJECTED, REFUSED, UNAVAILABLE, UNKNOWN }

    /** How the network's specification sorts its field 39 codes. */
    public enum Category { APPROVAL, DECLINE, UNAVAILABLE, FORMAT_ERROR }

    static final String SAF = "saf.host";
    static final String ALERTS = "alerts";

    private final MUX mux;
    private final Space<String, Object> sp;
    private final ISOPackager packager;          // the packager the link's channel uses
    private final long timeout;
    private final Map<String, Category> codes;   // from the network's specification

    public PaymentClient(MUX mux, Space<String, Object> sp, ISOPackager packager, long timeout,
                         Map<String, Category> codes) {
        this.mux = mux;
        this.sp = sp;
        this.packager = packager;
        this.timeout = timeout;
        this.codes = Map.copyOf(codes);
    }

    public Outcome pay(ISOMsg m) throws ISOException {
        if (refusal(m) != null)
            return Outcome.REFUSED;
        if (!mux.isConnected())
            return Outcome.UNAVAILABLE;
        ISOMsg response = mux.request(m, timeout);
        if (response == null)
            return unknown(m);
        return outcomeOf(m, response);
    }

    /** How a message that got an answer ended. */
    Outcome outcomeOf(ISOMsg request, ISOMsg response) throws ISOException {
        String rc = response.getString(39);
        Category c = rc == null ? null : codes.get(rc);
        if (rc == null)
            return unknown(request);
        if (c == null)
            return Outcome.DECLINED;
        switch (c) {
            case APPROVAL:    return Outcome.APPROVED;
            case DECLINE:     return Outcome.DECLINED;
            case UNAVAILABLE: return Outcome.TRY_LATER;
            default:
                sp.out(ALERTS, response);
                return Outcome.REJECTED;
        }
    }

    /** Why this message must not go to the link, or null when it may. */
    String refusal(ISOMsg m) {
        if (m.hasField(4) && !m.getString(4).chars().allMatch(Character::isDigit))
            return "field 4 is not a number";
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

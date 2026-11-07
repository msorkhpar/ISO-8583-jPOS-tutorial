package com.example.practice;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;
import org.jpos.iso.MUX;
import org.jpos.space.Space;
import org.jpos.space.SpaceUtil;

/**
 * Sends one message to the host and says how it ended. Anything that must still reach the host
 * later goes onto the store-and-forward queue {@code saf.host} in the given space.
 */
public class PaymentClient {
    public enum Outcome { APPROVED, DECLINED, REVERSED, QUEUED, NO_ANSWER, NOT_SENT }

    static final String SAF = "saf.host";

    private final MUX mux;
    private final Space<String, Object> sp;
    private final ReversalBuilder reversals;
    private final long timeout;

    public PaymentClient(MUX mux, Space<String, Object> sp, long timeout) {
        this.mux = mux;
        this.sp = sp;
        this.reversals = new ReversalBuilder(sp);
        this.timeout = timeout;
    }

    public Outcome send(ISOMsg m) throws ISOException {
        if (!mux.isConnected())
            return Outcome.NOT_SENT;
        ISOMsg response = mux.request(m, timeout);
        return response != null ? outcomeOf(response) : onNoAnswer(m);
    }

    /** How a message that got an answer ended. */
    Outcome outcomeOf(ISOMsg response) {
        return "00".equals(response.getString(39)) ? Outcome.APPROVED : Outcome.DECLINED;
    }

    /** What to do when the wait for an answer ended with nothing. */
    Outcome onNoAnswer(ISOMsg m) throws ISOException {
        char messageClass = m.getMTI().charAt(1);
        boolean advice = m.getMTI().charAt(2) == '2';
        if (messageClass == '8')
            return Outcome.NO_ANSWER;
        if (advice) {
            sp.out(SAF, m);
            return Outcome.QUEUED;
        }
        ISOMsg again = mux.request(m, timeout);
        if (again != null)
            return outcomeOf(again);
        sp.out(SAF, reversals.reversalOf(m));
        return Outcome.REVERSED;
    }

    /** The reversal builder from 12.6, given: a new message that points at the original. */
    static final class ReversalBuilder {
        private static final int[] COPIED = { 2, 3, 4, 12, 13, 14, 22, 32, 33, 37, 41, 42, 49 };
        private final Space<String, Object> sp;

        ReversalBuilder(Space<String, Object> sp) {
            this.sp = sp;
        }

        ISOMsg reversalOf(ISOMsg original) throws ISOException {
            ISOMsg rev = (ISOMsg) original.clone(COPIED);
            rev.setMTI("0420");
            rev.set(7, original.getString(7));
            rev.set(11, ISOUtil.zeropad(((SpaceUtil.nextLong(sp, "stan.host") - 1) % 999999) + 1, 6));
            rev.set(90, original.getMTI()
                    + ISOUtil.zeropad(original.getString(11), 6)
                    + ISOUtil.zeropad(original.getString(7), 10)
                    + ISOUtil.zeropad(original.hasField(32) ? original.getString(32) : "", 11)
                    + ISOUtil.zeropad(original.hasField(33) ? original.getString(33) : "", 11));
            return rev;
        }
    }
}

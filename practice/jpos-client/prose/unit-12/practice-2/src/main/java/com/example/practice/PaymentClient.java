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
        // TODO: QMUX would accept a message even with the link down; it would only expire unsent.
        //       When nothing can go out, what is there to undo? (two lines)
        ISOMsg response = mux.request(m, timeout);
        return response != null ? outcomeOf(response) : onNoAnswer(m);
    }

    /** How a message that got an answer ended. */
    Outcome outcomeOf(ISOMsg response) {
        // TODO: The host answered. Field 39 says how. (one line)
        throw new IllegalStateException("TODO: outcomeOf");
    }

    /** What to do when the wait for an answer ended with nothing. */
    Outcome onNoAnswer(ISOMsg m) throws ISOException {
        // TODO: This is what the client does today, and on slow days cardholders are charged twice.
        //       Decide by what the message is: 0100/0200 ask for money, 0120/0220 report what has
        //       already happened, 0800 moves no money at all. Return the Outcome that says what
        //       you did. (10 to 15 lines)
        ISOMsg again = mux.request(m, timeout);
        return again != null ? outcomeOf(again) : Outcome.NO_ANSWER;
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

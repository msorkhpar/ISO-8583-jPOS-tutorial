package com.example.practice;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * The acquirer's side of one link. Every request sent on the link waits here until its answer
 * arrives; {@link #deliver} hands each incoming message to the request it answers.
 */
public class Inbox {
    private final List<ISOMsg> waiting = new ArrayList<>();

    /** Records a request (or an advice) that was just sent and now waits for its answer. */
    public synchronized void expect(ISOMsg sent) {
        waiting.add(sent);
    }

    /** How many sent messages are still waiting. */
    public synchronized int waiting() {
        return waiting.size();
    }

    /**
     * The waiting message that {@code incoming} answers, which stops waiting; or null when
     * {@code incoming} answers none of them (the caller logs it).
     */
    public synchronized ISOMsg deliver(ISOMsg incoming) throws ISOException {
        for (Iterator<ISOMsg> it = waiting.iterator(); it.hasNext(); ) {
            ISOMsg sent = it.next();
            if (answers(sent, incoming)) {
                it.remove();
                return sent;
            }
        }
        return null;
    }

    /** Whether {@code incoming} is the answer to {@code sent}. */
    static boolean answers(ISOMsg sent, ISOMsg incoming) throws ISOException {
        String mti = incoming.getMTI();
        if (mti == null || !sent.isRequest())
            return false;
        ISOMsg expected = (ISOMsg) sent.clone();
        expected.setResponseMTI();
        return expected.getMTI().equals(mti)
                && Objects.equals(sent.getString(11), incoming.getString(11))
                && Objects.equals(sent.getString(41), incoming.getString(41));
    }
}

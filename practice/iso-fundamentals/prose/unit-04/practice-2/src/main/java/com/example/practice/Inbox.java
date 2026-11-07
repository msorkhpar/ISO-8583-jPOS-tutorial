package com.example.practice;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
        // TODO: Many terminals share this link, and the other side also sends messages of its own.
        //       Return true only when `incoming` is the other side's answer to this very message.
        //       Neither message may change.
        throw new IllegalStateException("TODO: answers");
    }
}

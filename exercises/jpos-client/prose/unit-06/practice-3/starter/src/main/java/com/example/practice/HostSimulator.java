package com.example.practice;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;

/**
 * The acquirer's host as the client's integration tests see it: it answers each request the way
 * the host's interface document describes. The tests keep every request they send, to check later
 * what the client asked for.
 */
public class HostSimulator {
    /** The fields a response echoes, from the interface document. */
    static final int[] ECHOED = { 2, 3, 4, 7, 11, 12, 13, 32, 37, 41, 42, 49 };

    private int approvals;

    /** The host's answer to {@code request}, with {@code responseCode} in field 39. */
    public ISOMsg respond(ISOMsg request, String responseCode) throws ISOException {
        ISOMsg response = echoed(request);
        answerMti(response);
        response.set(39, responseCode);
        if ("00".equals(responseCode))
            response.set(38, ISOUtil.zeropad(Integer.toString(++approvals), 6));
        return response;
    }

    /** A new message with the request's MTI and the fields the response echoes. */
    ISOMsg echoed(ISOMsg request) {
        // TODO: The response carries the request's MTI (for now) and the ECHOED fields, and
        //       nothing else of what the client sent.
        throw new IllegalStateException("TODO: echoed");
    }

    /** Turns the MTI the response carries (still the request's) into the response's MTI. */
    void answerMti(ISOMsg response) throws ISOException {
        // TODO: The client sends authorizations, purchases, advices and reversals, first attempts
        //       and repeats.
        throw new IllegalStateException("TODO: answerMti");
    }
}

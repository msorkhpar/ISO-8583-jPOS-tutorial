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
        ISOMsg response = (ISOMsg) request.clone(0);
        for (int f : ECHOED) {
            if (request.hasField(f))
                response.set(f, request.getString(f));
        }
        return response;
    }

    /** Turns the MTI the response carries (still the request's) into the response's MTI. */
    void answerMti(ISOMsg response) throws ISOException {
        String mti = response.getMTI();
        response.setMTI(mti.substring(0, 2) + (char) (mti.charAt(2) + 1) + mti.charAt(3));
    }
}

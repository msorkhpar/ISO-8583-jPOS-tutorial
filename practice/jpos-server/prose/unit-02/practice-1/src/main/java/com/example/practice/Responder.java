package com.example.practice;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;

/**
 * The server's answers to the acquirer's terminals. A participant calls {@link #answer} once it
 * has decided, and the channel sends what it returns.
 */
public class Responder {
    /** Data elements returned in responses, from the acquirer's specification. */
    static final int[] RETURNED = { 2, 3, 4, 7, 11, 12, 13, 32, 37, 41, 42, 49, 70, 90 };

    /** The response to {@code request}: an approval when responseCode is "00". */
    public ISOMsg answer(ISOMsg request, String responseCode, String approvalCode) throws ISOException {
        ISOMsg response = turnAround(request);
        response.set(39, responseCode);
        addApproval(response, responseCode, approvalCode);
        return response;
    }

    /** A new message that the terminal's client will take as the answer to this request. */
    ISOMsg turnAround(ISOMsg request) throws ISOException {
        // TODO: Today's terminals send authorizations (0100), purchases (0200), completion advices
        //       (0220), reversals and their repeats (0420, 0421) and echoes (0800). Each must get
        //       back a message its client recognises as the answer, carrying what the acquirer
        //       asks for (RETURNED). The participant that called you, the transaction log and the
        //       terminal's own logs all see what you do here. (about 4 lines)
        throw new IllegalStateException("TODO: turnAround");
    }

    /** What an answer with this response code carries besides field 39. */
    void addApproval(ISOMsg response, String responseCode, String approvalCode) throws ISOException {
        // TODO: The approval code is the issuer's proof that it approved. (about 2 lines)
        throw new IllegalStateException("TODO: addApproval");
    }
}

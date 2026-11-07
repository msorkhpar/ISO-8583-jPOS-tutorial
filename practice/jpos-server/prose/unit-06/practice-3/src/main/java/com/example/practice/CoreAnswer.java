package com.example.practice;

import org.jpos.iso.ISOChannel;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOSource;
import org.jpos.transaction.Context;
import org.jpos.transaction.ContextConstants;
import org.jpos.transaction.TransactionParticipant;

import java.io.Serializable;

/** Turns the core banking system's decision into the response SendResponse will send. */
public class CoreAnswer implements TransactionParticipant {
    /** What the core system decided; an earlier participant puts it in the Context under CORE. */
    public record Decision(String responseCode, String approvalCode, String message) {}

    static final String CORE = "CORE";
    private static final int[] ECHOED = { 0, 2, 3, 4, 7, 11, 12, 13, 32, 37, 41, 42, 49, 70 };

    @Override
    public int prepare(long id, Serializable context) {
        Context ctx = (Context) context;
        ISOMsg request = ctx.get(ContextConstants.REQUEST.toString());
        Decision decision = ctx.get(CORE);
        try {
            ISOMsg response = (ISOMsg) request.clone(ECHOED);
            response.setResponseMTI();
            response.set(39, decision.responseCode());
            if ("00".equals(decision.responseCode()))
                response.set(38, decision.approvalCode());    // an approval carries the core's code
            if (decision.message() != null)
                response.set(44, decision.message());         // shown on the terminal; optional
            makeSendable(response, ctx.get(ContextConstants.SOURCE.toString()));
            ctx.put(ContextConstants.RESPONSE.toString(), response);
            return "00".equals(response.getString(39)) ? PREPARED | NO_JOIN : ABORTED;
        } catch (ISOException e) {
            ctx.log(e);
            return ABORTED;
        }
    }

    /** Changes the response, if it must, so that the channel the request came in on can send it. */
    void makeSendable(ISOMsg response, ISOSource source) {
        // TODO: Today a response with a field that doesn't fit is never sent. Keep as much of the
        //       core's answer as the terminal can get: the message is optional, an approval is not
        //       an approval without its code, and 96 is the answer when nothing else can go.
        throw new IllegalStateException("TODO: makeSendable");
    }

    /** Whether the channel the request came in on can pack this message. */
    static boolean packs(ISOMsg m, ISOSource source) {
        // TODO: Answer the question the channel will ask when SendResponse sends. Don't change m.
        throw new IllegalStateException("TODO: packs");
    }
}

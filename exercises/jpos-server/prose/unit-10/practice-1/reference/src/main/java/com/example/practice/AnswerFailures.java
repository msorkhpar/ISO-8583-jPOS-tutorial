package com.example.practice;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.rc.CMF;
import org.jpos.rc.IRC;
import org.jpos.rc.Result;
import org.jpos.transaction.AbortParticipant;
import org.jpos.transaction.Context;
import org.jpos.transaction.ContextConstants;

import java.io.Serializable;
import java.util.Map;

/**
 * Stands right before SendResponse and answers, on the abort path, every transaction that has no
 * proper answer yet. The ATM network's specification lists the codes below. It approves with 00,
 * and with 10 when it dispenses less than was asked for (a partial approval).
 */
public class AnswerFailures implements AbortParticipant {
    private static final int[] ECHOED = { 0, 2, 3, 4, 7, 11, 12, 13, 32, 37, 41, 42, 49 };

    /** The network's codes for the causes our participants report. */
    static final Map<IRC, String> CODES = Map.of(
        CMF.NOT_SUFFICIENT_FUNDS, "51",
        CMF.INCORRECT_PIN, "55",
        CMF.EXCEEDS_WITHDRAWAL_FREQUENCY_LIMIT, "65",
        CMF.MAX_PIN_TRIES_EXCEEDED, "75",
        CMF.HOST_UNREACHABLE, "91",
        CMF.MISSING_FIELD, "30");

    @Override
    public int prepare(long id, Serializable context) {
        return PREPARED | NO_JOIN | READONLY;
    }

    @Override
    public int prepareForAbort(long id, Serializable context) {
        Context ctx = (Context) context;
        ISOMsg request = ctx.get(ContextConstants.REQUEST.toString());
        if (request == null || !needsAnswer(ctx.get(ContextConstants.RESPONSE.toString())))
            return ABORTED | NO_JOIN | READONLY;
        try {
            ISOMsg response = (ISOMsg) request.clone(ECHOED);
            response.setResponseMTI();
            response.set(39, codeFor(ctx.getResult()));
            ctx.put(ContextConstants.RESPONSE.toString(), response);
        } catch (ISOException e) {
            ctx.log(e);
        }
        return ABORTED | NO_JOIN;
    }

    /** The code for a transaction that failed, from what the participants reported. */
    String codeFor(Result result) {
        Result.Entry failure = result.failure();
        return failure == null ? "96" : CODES.getOrDefault(failure.getIrc(), "96");
    }

    /** Whether the transaction, which is aborting, still needs this participant's answer. */
    boolean needsAnswer(ISOMsg built) {
        if (built == null)
            return true;
        String rc = built.getString(39);
        return "00".equals(rc) || "10".equals(rc);
    }
}

package com.example.practice;

import org.jpos.core.Configurable;
import org.jpos.core.Configuration;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.MUX;
import org.jpos.rc.CMF;
import org.jpos.transaction.Context;
import org.jpos.transaction.TransactionParticipant;
import org.jpos.util.Caller;
import org.jpos.util.NameRegistrar;

import java.io.Serializable;
import java.util.Date;

import static org.jpos.transaction.ContextConstants.REQUEST;
import static org.jpos.transaction.ContextConstants.TIMESTAMP;

/** Asks the card's issuer about a withdrawal and leaves the answer for the participant after it. */
public class AskIssuer implements TransactionParticipant, Configurable {
    public static final String ISSUER_RESPONSE = "ISSUER_RESPONSE";

    private String muxName;
    private long terminalWait;          // how long the ATM waits for its answer, in ms

    @Override
    public void setConfiguration(Configuration cfg) {
        muxName = cfg.get("mux", "issuer");
        terminalWait = cfg.getLong("terminal-wait", 20000);
    }

    @Override
    public int prepare(long id, Serializable context) {
        Context ctx = (Context) context;
        MUX mux = NameRegistrar.getIfExists("mux." + muxName);
        if (mux == null || !mux.isConnected())
            return ctx.getResult().fail(CMF.ISSUER_UNAVAILABLE, Caller.info(), "issuer link down").FAIL();
        try {
            return ask(ctx, mux);
        } catch (ISOException e) {
            return ctx.getResult().fail(CMF.SYSTEM_ERROR, Caller.info(), e.getMessage()).FAIL();
        }
    }

    /** The time, in ms, that this transaction's ATM will still wait for its answer. */
    long remaining(Context ctx) {
        Date received = ctx.get(TIMESTAMP.toString());
        return terminalWait - (System.currentTimeMillis() - received.getTime());
    }

    /**
     * Asks the issuer about the request under REQUEST and leaves its answer, if one comes, under
     * ISSUER_RESPONSE. Returns this participant's vote.
     */
    int ask(Context ctx, MUX mux) throws ISOException {
        long left = remaining(ctx);
        ISOMsg request = ctx.get(REQUEST.toString());
        ISOMsg response = mux.request((ISOMsg) request.clone(), Math.max(left, 1000));
        if (response != null)
            ctx.put(ISSUER_RESPONSE, response);
        return PREPARED | NO_JOIN | READONLY;
    }
}

package com.example.practice;

import org.jpos.core.Configurable;
import org.jpos.core.Configuration;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.transaction.Context;
import org.jpos.transaction.ContextConstants;
import org.jpos.transaction.TransactionParticipant;

import java.io.Serializable;

/**
 * The first participant of the server's chain. A request the authorizer behind it can't process
 * ends here; every other request goes on, untouched.
 */
public class RequiredFields implements TransactionParticipant, Configurable {
    /** The fields a response copies from its request, as unit s2 explains. */
    static final int[] ECHOED = { 0, 2, 3, 4, 7, 11, 12, 13, 32, 37, 41, 42, 49, 70 };

    private String formatError;

    @Override
    public void setConfiguration(Configuration cfg) {
        formatError = cfg.get("format-error-rc", "30");
    }

    @Override
    public int prepare(long id, Serializable context) {
        Context ctx = (Context) context;
        ISOMsg request = ctx.get(ContextConstants.REQUEST.toString());
        try {
            if (isComplete(request))
                return PREPARED | NO_JOIN | READONLY;
            return refuse(ctx, request);
        } catch (ISOException e) {
            ctx.log(e);
            return ABORTED;
        }
    }

    /** Whether the authorizer has what it needs to decide on this request. */
    boolean isComplete(ISOMsg request) throws ISOException {
        // TODO: The authorizer needs an amount (field 4) to decide on anything that moves money:
        //       authorizations (01xx) and purchases (02xx). Other messages carry no amount and
        //       still have to reach it.
        throw new IllegalStateException("TODO: isComplete");
    }

    /** Ends the transaction of a request the authorizer can't process. */
    int refuse(Context ctx, ISOMsg request) throws ISOException {
        // TODO: The terminal is waiting for an answer to this request, and must be able to tell
        //       that it is the answer to this request. The response code is formatError.
        //       Return the vote that keeps the authorizer from seeing the request.
        throw new IllegalStateException("TODO: refuse");
    }
}

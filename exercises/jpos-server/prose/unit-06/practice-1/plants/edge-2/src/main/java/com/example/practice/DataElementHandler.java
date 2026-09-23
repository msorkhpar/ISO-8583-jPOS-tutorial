package com.example.practice;

import java.io.Serializable;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.transaction.Context;
import org.jpos.transaction.TransactionParticipant;

/** Answers the request in the Context with an approved response built from it. */
public class DataElementHandler implements TransactionParticipant {

    @Override
    public int prepare(long id, Serializable context) {
        Context ctx = (Context) context;
        ISOMsg request = ctx.get("REQUEST");
        try {
            ISOMsg response = (ISOMsg) request.clone();
            String mti = response.getMTI();
            response.setMTI(mti.substring(0, 2) + (char) (mti.charAt(2) + 1) + mti.substring(3));
            response.set(39, "00");
            response.set(38, "123456");
            ctx.put("RESPONSE", response);
            return PREPARED | NO_JOIN | READONLY;
        } catch (ISOException e) {
            ctx.put("EXCEPTION", e);
            return ABORTED | NO_JOIN | READONLY;
        }
    }
}

package com.example.practice;

import org.jpos.core.Configurable;
import org.jpos.core.Configuration;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;
import org.jpos.space.Space;
import org.jpos.space.SpaceFactory;
import org.jpos.transaction.Context;
import org.jpos.transaction.ContextConstants;
import org.jpos.transaction.TransactionParticipant;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Stands right after QueryHost in the mastercard group. QueryHost has sent the purchase to the
 * network and put the network's answer, if one came, under RESPONSE.
 */
public class AfterNetwork implements TransactionParticipant, Configurable {
    private final Space<String, Object> space = SpaceFactory.getSpace();
    private final AtomicLong stans = new AtomicLong();
    private String saf;                                  // the queue the network's SAF sender reads

    @Override
    public void setConfiguration(Configuration cfg) {
        saf = cfg.get("saf", "saf.mastercard");
    }

    @Override
    public int prepare(long id, Serializable context) {
        Context ctx = (Context) context;
        ISOMsg response = ctx.get(ContextConstants.RESPONSE.toString());
        if (response != null && "00".equals(response.getString(39)))
            return PREPARED | NO_JOIN | READONLY;
        ISOMsg request = ctx.get(ContextConstants.REQUEST.toString());
        try {
            space.out(saf, reversalOf(request));
        } catch (ISOException e) {
            ctx.log(e);
        }
        return response != null ? PREPARED | NO_JOIN : ABORTED | NO_JOIN;
    }

    /** The message that asks the network to undo {@code original}: a new message pointing back at it. */
    ISOMsg reversalOf(ISOMsg original) throws ISOException {
        ISOMsg rev = (ISOMsg) original.clone(2, 3, 4, 7, 32, 37, 41, 42, 49);
        rev.setMTI("0420");
        rev.set(11, ISOUtil.zeropad((stans.incrementAndGet() - 1) % 999999 + 1, 6));
        rev.set(90, original.getMTI()
                + ISOUtil.zeropad(original.getString(11), 6)
                + ISOUtil.zeropad(original.hasField(7) ? original.getString(7) : "", 10)
                + ISOUtil.zeropad("", 22));
        return rev;
    }
}

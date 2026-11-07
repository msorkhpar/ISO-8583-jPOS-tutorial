package com.example.practice;

import org.jpos.core.Configurable;
import org.jpos.core.Configuration;
import org.jpos.iso.ISOMsg;
import org.jpos.transaction.Context;
import org.jpos.transaction.TransactionParticipant;
import org.jpos.util.NameRegistrar;

import java.io.Serializable;

import static org.jpos.transaction.ContextConstants.REQUEST;
import static org.jpos.transaction.ContextConstants.RESPONSE;

/**
 * Redeems the cardholder's loyalty points in prepare (field 48 holds how many, field 102 the
 * loyalty account); confirms the redemption on commit and cancels it on abort.
 */
public class RedeemPoints implements TransactionParticipant, Configurable {

    /** The loyalty service, outside the server; registered in the NameRegistrar. */
    public interface PointsService {
        /** Takes the points off the account until confirmed or cancelled; returns a redemption id. */
        String redeem(String account, long points) throws Exception;
        void confirm(String redemptionId) throws Exception;
        void cancel(String redemptionId) throws Exception;
    }

    static final String REDEMPTION = "REDEMPTION";
    private String serviceName;

    @Override
    public void setConfiguration(Configuration cfg) {
        serviceName = cfg.get("service", "points");
    }

    @Override
    public int prepare(long id, Serializable context) {
        Context ctx = (Context) context;
        ISOMsg request = ctx.get(REQUEST.toString());
        if (request == null || !request.hasField(48))
            return PREPARED | NO_JOIN | READONLY;              // paid without points
        try {
            PointsService points = NameRegistrar.get(serviceName);
            String redemptionId = points.redeem(request.getString(102), Long.parseLong(request.getString(48)));
            return afterRedeem(ctx, redemptionId);
        } catch (Exception e) {
            ctx.log("redemption failed: " + e);
            try {
                ISOMsg reply = (ISOMsg) request.clone();
                reply.setResponseMTI();
                reply.set(39, "96");
                ctx.put(RESPONSE.toString(), reply);
            } catch (Exception ignored) {
                // nothing to answer
            }
            return ABORTED | NO_JOIN;
        }
    }

    @Override
    public void commit(long id, Serializable context) {
        finish((Context) context, true);
    }

    @Override
    public void abort(long id, Serializable context) {
        finish((Context) context, false);
    }

    /**
     * The points are taken. Keep what commit or abort will need to finish this redemption, also
     * when they run in a restarted server after a crash, and return the vote.
     */
    int afterRedeem(Context ctx, String redemptionId) {
        ctx.put(REDEMPTION, redemptionId, true);
        return PREPARED;
    }

    /**
     * Confirms (commit) or cancels (abort) this transaction's redemption. After a restart, the
     * TransactionManager calls this for every transaction that was in flight. Never throws.
     */
    void finish(Context ctx, boolean confirm) {
        String redemptionId = ctx.getString(REDEMPTION);
        try {
            PointsService points = NameRegistrar.get(serviceName);
            if (confirm)
                points.confirm(redemptionId);
            else
                points.cancel(redemptionId);
        } catch (Exception e) {
            ctx.log((confirm ? "confirm" : "cancel") + " failed for " + redemptionId + ": " + e);
        }
    }
}

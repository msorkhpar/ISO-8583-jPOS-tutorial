package com.example.practice;

import org.jpos.core.Configurable;
import org.jpos.core.Configuration;
import org.jpos.core.ConfigurationException;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.transaction.Context;
import org.jpos.transaction.ContextConstants;
import org.jpos.transaction.TransactionParticipant;
import org.jpos.util.NameRegistrar;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * The fraud team's velocity rule: a card gets at most a fixed number of purchases a day through
 * this server. A purchase over the count is refused with 65. Sits before the authorizer.
 */
public class VelocityLimit implements TransactionParticipant, Configurable {
    private static final String COUNTED = "VELOCITY_COUNTED";
    private DailyCounts counts;

    @Override
    public void setConfiguration(Configuration cfg) throws ConfigurationException {
        counts = NameRegistrar.getIfExists(cfg.get("counts", "daily-counts"));
        if (counts == null)
            throw new ConfigurationException("no daily counts registered");
    }

    @Override
    public int prepare(long id, Serializable context) {
        Context ctx = (Context) context;
        ISOMsg request = ctx.get(ContextConstants.REQUEST.toString());
        try {
            if (!counts.take(request.getString(2))) {
                ctx.put(ContextConstants.RESPONSE.toString(), tooMany(request));
                return ABORTED;
            }
            ctx.put(COUNTED, Boolean.TRUE);
            return PREPARED;
        } catch (ISOException e) {
            ctx.log(e);
            return ABORTED | NO_JOIN;
        }
    }

    @Override
    public void abort(long id, Serializable context) {
        Context ctx = (Context) context;
        ISOMsg request = ctx.get(ContextConstants.REQUEST.toString());
        counts.giveBack(request.getString(2));
    }

    /** Given: the answer for a card that has had its purchases for today. */
    static ISOMsg tooMany(ISOMsg request) throws ISOException {
        ISOMsg response = (ISOMsg) request.clone();
        response.setResponseMTI();
        response.set(39, "65");                       // exceeds withdrawal frequency limit
        return response;
    }

    /** Given: purchases per card today, shared by every session. */
    public static final class DailyCounts {
        private final Map<String, Integer> counts = new HashMap<>();
        private final int max;

        public DailyCounts(int max) {
            this.max = max;
        }

        /** Counts one more purchase for the card, unless it already has max of them. */
        public synchronized boolean take(String card) {
            int now = counts.getOrDefault(card, 0);
            if (now >= max)
                return false;
            counts.put(card, now + 1);
            return true;
        }

        /** Takes back one purchase counted for the card. */
        public synchronized void giveBack(String card) {
            counts.merge(card, -1, Integer::sum);
        }

        public synchronized int count(String card) {
            return counts.getOrDefault(card, 0);
        }
    }
}

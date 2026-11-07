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
 * Answers a reversal (0400, or its repeat 0401) of a pre-authorization (0100). A hotel or a fuel
 * pump pre-authorizes an amount, and the server places a hold for it on the cardholder's account;
 * the hold table below remembers each one. A reversal asks for the hold to be lifted.
 */
public class ReleaseHold implements TransactionParticipant, Configurable {
    private static final int[] ECHOED = { 0, 2, 3, 4, 7, 11, 32, 37, 41, 42, 49, 90 };
    private HoldTable holds;

    @Override
    public void setConfiguration(Configuration cfg) throws ConfigurationException {
        try {
            holds = NameRegistrar.get(cfg.get("holds", "holds"));
        } catch (NameRegistrar.NotFoundException e) {
            throw new ConfigurationException("no hold table registered", e);
        }
    }

    @Override
    public int prepare(long id, Serializable context) {
        Context ctx = (Context) context;
        ISOMsg reversal = ctx.get(ContextConstants.REQUEST.toString());
        lift(reversal);
        try {
            ctx.put(ContextConstants.RESPONSE.toString(), reply(reversal, answerCode(reversal)));
        } catch (ISOException e) {
            ctx.log(e);
        }
        return PREPARED | NO_JOIN;
    }

    /** Lifts the hold this reversal is about, if there is one to lift. */
    void lift(ISOMsg reversal) {
        String original = reversal.getString(90);
        HoldTable.Hold hold = holds.find(reversal.getString(41), original.substring(4, 10), original.substring(10, 20));
        if (hold != null)
            holds.credit(hold.account, hold.amount);
    }

    /** Field 39 of the answer to this reversal. */
    String answerCode(ISOMsg reversal) {
        return "00";
    }

    private static ISOMsg reply(ISOMsg request, String rc) throws ISOException {
        ISOMsg r = (ISOMsg) request.clone(ECHOED);
        r.setResponseMTI();
        r.set(39, rc);
        return r;
    }

    /** The hold table: one row per pre-authorization, and the accounts' available balances. */
    public static class HoldTable {
        public static final class Hold {
            final String account;
            final long amount;
            boolean open = true;

            Hold(String account, long amount) {
                this.account = account;
                this.amount = amount;
            }
        }

        private final Map<String, Hold> rows = new HashMap<>();
        private final Map<String, Long> available = new HashMap<>();

        /** What the authorization chain does: takes the amount from the account and remembers why. */
        public synchronized void place(String terminal, String stan, String transmitted, String account, long amount) {
            rows.put(terminal + "/" + stan + "/" + transmitted, new Hold(account, amount));
            available.merge(account, -amount, Long::sum);
        }

        /** The hold placed by this terminal's request with this STAN and field 7, or null. */
        public synchronized Hold find(String terminal, String stan, String transmitted) {
            return rows.get(terminal + "/" + stan + "/" + transmitted);
        }

        public synchronized void credit(String account, long amount) {
            available.merge(account, amount, Long::sum);
        }

        public synchronized long available(String account) {
            return available.getOrDefault(account, 0L);
        }

        public synchronized void open(String account, long balance) {
            available.put(account, balance);
        }
    }
}

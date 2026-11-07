package com.example.practice;

import org.jpos.core.Configurable;
import org.jpos.core.Configuration;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.transaction.Context;
import org.jpos.transaction.ContextConstants;
import org.jpos.transaction.TransactionParticipant;
import org.jpos.util.Log;
import org.jpos.util.LogEvent;
import org.jpos.util.Logger;

import java.io.Serializable;
import java.time.YearMonth;
import java.time.ZoneOffset;

/**
 * Refuses a purchase whose card has expired: 54 when the expiry date (field 14, YYMM) is before
 * this month, 30 when field 14 can't be read. Deployed as
 * {@code <participant class="com.example.practice.CardExpiry" logger="Q2" realm="card-expiry"/>}.
 */
public class CardExpiry extends Log implements TransactionParticipant, Configurable {
    private YearMonth fixedMonth;                       // configuration only: set by tests

    @Override
    public void setConfiguration(Configuration cfg) {
        String m = cfg.get("this-month", null);
        fixedMonth = m == null ? null : YearMonth.parse(m);
    }

    @Override
    public int prepare(long id, Serializable context) {
        Context ctx = (Context) context;
        ISOMsg request = ctx.get(ContextConstants.REQUEST.toString());
        LogEvent ev = createInfo();
        ev.addMessage("txn " + id);
        ev.addMessage(request);
        try {
            String reason = refusal(request.getString(14));
            if (reason == null) {
                ev.addMessage("expiry ok");
                Logger.log(ev);
                return PREPARED | NO_JOIN | READONLY;
            }
            ev.addMessage(reason);
            ctx.put(ContextConstants.RESPONSE.toString(), reply(request, "54"));
            Logger.log(ev);
        } catch (IllegalArgumentException e) {
            ctx.put(ContextConstants.RESPONSE.toString(), reply(request, "30"));
        }
        return ABORTED;
    }

    /**
     * Why a card with this expiry date is refused, or null when it may be used.
     * Throws IllegalArgumentException when the date can't be read.
     */
    String refusal(String field14) {
        if (field14 == null || !field14.matches("\\d{2}(0[1-9]|1[0-2])"))
            throw new IllegalArgumentException("expiry unreadable: '" + field14 + "'");
        YearMonth expiry = YearMonth.of(2000 + Integer.parseInt(field14.substring(0, 2)),
                                        Integer.parseInt(field14.substring(2)));
        YearMonth now = fixedMonth != null ? fixedMonth : YearMonth.now(ZoneOffset.UTC);
        return expiry.isBefore(now) ? "card expired " + field14 + ", this month is " + now : null;
    }

    /** The answer the terminal gets, carrying response code rc. */
    static ISOMsg reply(ISOMsg request, String rc) {
        try {
            ISOMsg r = (ISOMsg) request.clone();
            r.setResponseMTI();
            r.set(39, rc);
            r.unset(14, 35, 45, 52, 55);
            return r;
        } catch (ISOException e) {
            throw new IllegalStateException(e);
        }
    }
}

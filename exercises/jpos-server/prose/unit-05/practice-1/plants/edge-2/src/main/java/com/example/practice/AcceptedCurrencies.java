package com.example.practice;

import org.jpos.core.Configurable;
import org.jpos.core.Configuration;
import org.jpos.core.ConfigurationException;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.transaction.Context;
import org.jpos.transaction.ContextConstants;
import org.jpos.transaction.TransactionParticipant;

import java.io.Serializable;
import java.util.Set;

/**
 * Declines a purchase whose currency (field 49) the brand's merchant contract doesn't cover.
 * The descriptor puts one in each brand's group, each with its own properties:
 * {@code currencies} (codes separated by spaces or commas) and {@code rc} (default "58").
 */
public class AcceptedCurrencies implements TransactionParticipant, Configurable {
    private static final int[] ECHOED = { 0, 2, 3, 4, 7, 11, 12, 13, 32, 37, 41, 42, 49 };

    private Set<String> currencies;
    private String rc;

    @Override
    public void setConfiguration(Configuration cfg) throws ConfigurationException {
        String list = cfg.get("currencies").trim();
        currencies = Set.of(list.split("[\\s,]+"));
        rc = cfg.get("rc", "58");
    }

    @Override
    public int prepare(long id, Serializable context) {
        Context ctx = (Context) context;
        ISOMsg request = ctx.get(ContextConstants.REQUEST.toString());
        if (currencies.contains(request.getString(49)))
            return PREPARED | NO_JOIN | READONLY;
        ctx.put(ContextConstants.RESPONSE.toString(), answer(request, rc));
        return ABORTED | NO_JOIN;
    }

    /** The response to {@code request} with response code {@code rc}. */
    static ISOMsg answer(ISOMsg request, String rc) {
        try {
            ISOMsg response = (ISOMsg) request.clone(ECHOED);
            response.setResponseMTI();
            response.set(39, rc);
            return response;
        } catch (ISOException e) {
            throw new IllegalArgumentException(e);
        }
    }
}

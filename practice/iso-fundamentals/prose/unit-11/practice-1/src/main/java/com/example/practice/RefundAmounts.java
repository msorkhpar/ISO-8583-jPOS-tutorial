package com.example.practice;

import org.jpos.iso.ISOCurrency;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;

import java.math.BigDecimal;

/**
 * The merchant portal's refunds. A merchant types the amount to refund as they would write it
 * ("42.10", "25000"), and the portal shows the amounts of past purchases from their field 4.
 */
public class RefundAmounts {

    /** A refund (0200, processing code 20) for part or all of an earlier purchase, in its currency. */
    public ISOMsg refund(ISOMsg purchase, String typed) throws ISOException {
        String currency = purchase.getString(49);
        ISOMsg refund = new ISOMsg("0200");
        refund.set(2, purchase.getString(2));
        refund.set(3, "200000");
        refund.set(4, field4(typed, currency));
        refund.set(41, purchase.getString(41));
        refund.set(49, currency);
        return refund;
    }

    /** What the portal shows for a past purchase. */
    public String shown(ISOMsg purchase) {
        return shown(purchase.getString(4), purchase.getString(49));
    }

    /**
     * Field 4 for an amount typed by the merchant in the given currency (ISO 4217 numeric code).
     * Throws IllegalArgumentException when the amount can't be sent in that currency as typed.
     */
    String field4(String typed, String currency) {
        // TODO: The merchants who complain are in Chile, Bahrain and Korea. Build field 4 so that
        //       the amount the issuer reads is the amount the merchant typed, and refuse what the
        //       currency can't carry.
        throw new IllegalStateException("TODO: field4");
    }

    /** The amount in field 4 as the merchant would write it, for example 42.10. */
    String shown(String field4, String currency) {
        // TODO: Today the portal shows every amount with two decimals.
        throw new IllegalStateException("TODO: shown");
    }
}

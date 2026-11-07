package com.example.practice;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;

/**
 * A merchant's end-of-day report: names each message the acquirer sent for the merchant and adds up
 * the amounts per kind, so the merchant can compare them with the till.
 */
public final class DailyReport {

    public enum Kind { PURCHASE, CASH, REFUND, REVERSAL, OTHER }

    private DailyReport() {
    }

    /** The amount, in minor units, of every message of each kind. */
    public static Map<Kind, Long> totals(List<ISOMsg> sent) throws ISOException {
        Map<Kind, Long> totals = new EnumMap<>(Kind.class);
        for (ISOMsg m : sent) {
            long amount = m.hasField(4) ? Long.parseLong(m.getString(4)) : 0;
            totals.merge(kind(m), amount, Long::sum);
        }
        return totals;
    }

    /** What kind of transaction a message is. */
    public static Kind kind(ISOMsg m) throws ISOException {
        String mti = m.getMTI();
        if (undoesAnother(mti))
            return Kind.REVERSAL;
        if (!asksAboutMoney(mti) || !m.hasField(3))
            return Kind.OTHER;
        return byProcessingCode(m.getString(3));
    }

    /** True for a message that cancels an earlier one. */
    static boolean undoesAnother(String mti) {
        return mti.charAt(1) == '4';
    }

    /** True for a message that asks for, or reports, a transaction with a cardholder's money. */
    static boolean asksAboutMoney(String mti) {
        return mti.charAt(1) == '1' || mti.charAt(1) == '2';
    }

    /** The kind of transaction field 3 names. */
    static Kind byProcessingCode(String processingCode) {
        switch (processingCode.substring(0, 2)) {
            case "00": return Kind.PURCHASE;
            case "01": return Kind.CASH;
            case "20": return Kind.REFUND;
            default:   return Kind.OTHER;
        }
    }
}

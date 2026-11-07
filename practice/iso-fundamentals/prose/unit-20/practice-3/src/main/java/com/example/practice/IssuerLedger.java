package com.example.practice;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;

/**
 * An issuer's ledger for the financial messages and reversals it receives from one network. On this
 * network every transmission has its own STAN and field 7; see the statement for what stays the same.
 */
public class IssuerLedger {

    public enum Result { POSTED, DUPLICATE, REVERSED, ALREADY_REVERSED, NOTHING_TO_REVERSE, LATE_ORIGINAL }

    private final Map<String, Long> posted = new HashMap<>();     // key -> amount posted
    private final Set<String> reversed = new HashSet<>();          // keys a reversal has named
    private long balance;

    /** Applies one incoming 02xx or 04xx message. Every one of them is acknowledged by the caller. */
    public Result handle(ISOMsg m) throws ISOException {
        String key = key(m);
        return m.getMTI().startsWith("04") ? onReversal(key, m) : onFinancial(key, m);
    }

    /** What every transmission of one transaction, and its reversal, have in common on this network. */
    static String key(ISOMsg m) {
        throw new IllegalStateException("TODO: key");
    }

    /** A financial request or advice {@code m}, for the transaction {@code key}. Its amount is field 4. */
    Result onFinancial(String key, ISOMsg m) throws ISOException {
        throw new IllegalStateException("TODO: onFinancial");
    }

    /** A reversal {@code m} of the transaction {@code key}. */
    Result onReversal(String key, ISOMsg m) throws ISOException {
        throw new IllegalStateException("TODO: onReversal");
    }

    /** What the cardholders owe on the transactions this ledger has applied, in minor units. */
    public long balance() {
        return balance;
    }
}

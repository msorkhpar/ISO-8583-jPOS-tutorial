package com.example.practice;

import java.util.HashMap;
import java.util.Map;
import org.jpos.iso.ISOMsg;
import org.jpos.tlv.TLVList;

/**
 * The issuer's record of chip counters: for each card, the last application transaction counter
 * (tag 9F36) of an authorization whose cryptogram verified.
 */
public final class AtcGuard {

    public enum Verdict {
        /** The cryptogram verified and the counter is new for this card. */
        ACCEPTED,
        /** The cryptogram verified, but this card has already used this counter or a later one. */
        REPLAYED,
        /** The HSM did not verify the cryptogram. */
        CRYPTOGRAM_FAILED
    }

    private final Map<String, Integer> lastVerified = new HashMap<>();

    /** Called for each chip authorization request, once the HSM has said whether its ARQC verified. */
    public synchronized Verdict check(ISOMsg request, boolean cryptogramVerified) {
        return decide(card(request), atc(request), cryptogramVerified);
    }

    /** What identifies the card whose counter this is. */
    static String card(ISOMsg request) {
        // TODO: two cards can share a card number. Say which one this is.
        throw new IllegalStateException("TODO: card");
    }

    /** The application transaction counter in DE 55. */
    static int atc(ISOMsg request) {
        // TODO: read the counter the card sent.
        throw new IllegalStateException("TODO: atc");
    }

    /** The verdict for this counter on this card, keeping the record up to date. */
    Verdict decide(String card, int atc, boolean cryptogramVerified) {
        // TODO: decide, and remember what later requests from this card must be compared with.
        throw new IllegalStateException("TODO: decide");
    }
}

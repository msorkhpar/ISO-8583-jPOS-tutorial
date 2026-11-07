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
        return request.getString(2);
    }

    /** The application transaction counter in DE 55. */
    static int atc(ISOMsg request) {
        TLVList chip = new TLVList();
        chip.unpack(request.getBytes(55));
        byte[] v = chip.getValue(0x9F36);
        return ((v[0] & 0xFF) << 8) | (v[1] & 0xFF);
    }

    /** The verdict for this counter on this card, keeping the record up to date. */
    Verdict decide(String card, int atc, boolean cryptogramVerified) {
        if (!cryptogramVerified)
            return Verdict.CRYPTOGRAM_FAILED;
        Integer last = lastVerified.get(card);
        if (last != null && atc <= last)
            return Verdict.REPLAYED;
        lastVerified.put(card, atc);
        return Verdict.ACCEPTED;
    }
}

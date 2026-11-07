package com.example.practice;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;

import java.util.HashMap;
import java.util.Map;

/**
 * Sits in the switch in front of every issuer link. Every authorization for an issuer passes
 * through {@link #forward}, from many threads at once.
 */
public class IssuerLimit {

    /** One issuer link: returns the issuer's answer, or null when none came in time. May throw. */
    public interface Issuer {
        ISOMsg authorize(ISOMsg request) throws Exception;
    }

    private final Map<String, Integer> limits;
    private final Map<String, Integer> inFlight = new HashMap<>();

    /** @param limits for each issuer id, how many of its requests may be in flight at once */
    public IssuerLimit(Map<String, Integer> limits) {
        this.limits = Map.copyOf(limits);
    }

    /** The answer for the acquirer: the issuer's, or the switch's own when it doesn't ask the issuer. */
    public ISOMsg forward(ISOMsg request, String issuerId, Issuer issuer) throws Exception {
        if (!tryEnter(issuerId))
            return refused(request);
        try {
            return issuer.authorize(request);
        } finally {
            leave(issuerId);
        }
    }

    /** Takes a place for one more request to this issuer, if it has one free. */
    synchronized boolean tryEnter(String issuerId) {
        int now = inFlight.getOrDefault(issuerId, 0);
        if (now >= limits.get(issuerId))
            return false;
        inFlight.put(issuerId, now + 1);
        return true;
    }

    /** Gives back the place a request took. */
    synchronized void leave(String issuerId) {
        inFlight.merge(issuerId, -1, Integer::sum);
    }

    /** The switch's own answer to a request it does not send to the issuer. */
    ISOMsg refused(ISOMsg request) throws ISOException {
        ISOMsg answer = (ISOMsg) request.clone();
        answer.setResponseMTI();
        answer.set(39, "91");
        return answer;
    }
}

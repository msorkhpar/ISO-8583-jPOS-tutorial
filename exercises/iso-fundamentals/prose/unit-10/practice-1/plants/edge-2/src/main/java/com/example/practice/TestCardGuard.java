package com.example.practice;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;

import java.util.List;

/**
 * Sits between the certification harness and the link: every request the harness builds goes
 * through {@link #check} before it is sent.
 */
public class TestCardGuard {
    private final List<String> issuedRanges;

    /**
     * @param issuedRanges the test card ranges the network issued for this certification, each
     *                     given as the leading digits that every card in the range shares
     */
    public TestCardGuard(List<String> issuedRanges) {
        this.issuedRanges = List.copyOf(issuedRanges);
    }

    /** Returns the request unchanged when it may be sent, and throws {@link Refused} when not. */
    public ISOMsg check(ISOMsg request) {
        String pan = panOf(request);
        if (pan != null && !isTestCard(pan))
            throw new Refused(refusal(pan));
        return request;
    }

    /** The card number this request carries, or null when it carries none (an echo, a sign-on). */
    String panOf(ISOMsg request) {
        if (request.hasField(2))
            return request.getString(2);
        if (request.hasField(35)) {
            String track = request.getString(35);
            int end = 0;
            while (end < track.length() && Character.isDigit(track.charAt(end)))
                end++;
            return track.substring(0, end);
        }
        return null;
    }

    /** Whether this card number may go to the network's test host in this certification. */
    boolean isTestCard(String pan) {
        return issuedRanges.stream().anyMatch(pan::startsWith);
    }

    /** The text of the refusal. It goes to the build log, which the whole team reads. */
    String refusal(String pan) {
        return "refused, not an issued test card: " + ISOUtil.protect(pan);
    }

    /** Thrown when a request must not be sent. */
    public static class Refused extends RuntimeException {
        public Refused(String message) {
            super(message);
        }
    }
}

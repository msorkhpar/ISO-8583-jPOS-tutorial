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
        // TODO: Find the card number in the request, wherever this request carries it.
        throw new IllegalStateException("TODO: panOf");
    }

    /** Whether this card number may go to the network's test host in this certification. */
    boolean isTestCard(String pan) {
        // TODO: Decide whether this number is one the harness may send to the test host.
        throw new IllegalStateException("TODO: isTestCard");
    }

    /** The text of the refusal. It goes to the build log, which the whole team reads. */
    String refusal(String pan) {
        // TODO: Tell the tester which card was stopped, in a form the build log may keep.
        throw new IllegalStateException("TODO: refusal");
    }

    /** Thrown when a request must not be sent. */
    public static class Refused extends RuntimeException {
        public Refused(String message) {
            super(message);
        }
    }
}

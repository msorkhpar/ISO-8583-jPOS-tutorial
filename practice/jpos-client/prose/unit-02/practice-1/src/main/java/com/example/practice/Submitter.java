package com.example.practice;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOPackager;
import org.jpos.iso.MUX;

/**
 * Sends the shop's requests to the acquirer through one MUX and says what became of each one.
 * The MUX's channel packs every message with {@code packager}.
 */
public class Submitter {
    public enum Outcome { ANSWERED, NO_ANSWER, NOT_SENT }

    /** What became of one request. {@code response} is set only when ANSWERED, {@code reason} only when NOT_SENT. */
    public record Result(Outcome outcome, ISOMsg response, String reason) { }

    private final MUX mux;
    private final ISOPackager packager;
    private final long timeout;

    public Submitter(MUX mux, ISOPackager packager, long timeout) {
        this.mux = mux;
        this.packager = packager;
        this.timeout = timeout;
    }

    public Result submit(ISOMsg request) throws ISOException {
        // TODO: Today every request goes straight to the MUX. Make sure a request the link can't
        //       carry is reported as NOT_SENT, with the reason, and never reaches the MUX.
        ISOMsg response = mux.request(request, timeout);
        return outcomeOf(response);
    }

    /** Null when the link can carry this request as it is; otherwise what is wrong with it. */
    String packProblem(ISOMsg request) {
        // TODO: Find out whether this request can go out on this link, the way the channel would.
        throw new IllegalStateException("TODO: packProblem");
    }

    /** The result of a request that went to the MUX, given what came back (null: nothing in time). */
    Result outcomeOf(ISOMsg response) {
        // TODO: Say what the caller now knows about a request that went to the MUX.
        throw new IllegalStateException("TODO: outcomeOf");
    }
}

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
        String problem = packProblem(request);
        if (problem != null)
            return new Result(Outcome.NOT_SENT, null, problem);
        ISOMsg response = mux.request(request, timeout);
        return outcomeOf(response);
    }

    /** Null when the link can carry this request as it is; otherwise what is wrong with it. */
    String packProblem(ISOMsg request) {
        try {
            request.setPackager(packager);
            request.pack();
            return null;
        } catch (ISOException e) {
            return e.getMessage();
        }
    }

    /** The result of a request that went to the MUX, given what came back (null: nothing in time). */
    Result outcomeOf(ISOMsg response) {
        if (response == null)
            return new Result(Outcome.NO_ANSWER, null, null);
        return new Result(Outcome.ANSWERED, response, null);
    }
}

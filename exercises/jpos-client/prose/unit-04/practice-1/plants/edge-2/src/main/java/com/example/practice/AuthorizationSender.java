package com.example.practice;

import java.util.concurrent.CompletableFuture;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOResponseListener;
import org.jpos.iso.MUX;

/** Sends authorization requests over a MUX and says what came of them. */
public final class AuthorizationSender {

    public enum Outcome { APPROVED, DECLINED, NO_ANSWER }

    private final MUX mux;
    private final long timeoutMillis;

    public AuthorizationSender(MUX mux, long timeoutMillis) {
        this.mux = mux;
        this.timeoutMillis = timeoutMillis;
    }

    public Outcome authorize(ISOMsg request) throws ISOException {
        return outcomeOf(mux.request(request, timeoutMillis));
    }

    public CompletableFuture<Outcome> authorizeAsync(ISOMsg request) throws ISOException {
        CompletableFuture<Outcome> outcome = new CompletableFuture<>();
        mux.request(request, timeoutMillis, new ISOResponseListener() {
            @Override
            public void responseReceived(ISOMsg response, Object handBack) {
                outcome.complete("00".equals(response.getString(39)) ? Outcome.APPROVED : Outcome.DECLINED);
            }
        }, null);
        return outcome;
    }

    private static Outcome outcomeOf(ISOMsg response) {
        if (response == null) {
            return Outcome.NO_ANSWER;
        }
        return "00".equals(response.getString(39)) ? Outcome.APPROVED : Outcome.DECLINED;
    }
}

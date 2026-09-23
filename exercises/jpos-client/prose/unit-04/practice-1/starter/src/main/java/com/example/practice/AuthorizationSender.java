package com.example.practice;

import java.util.concurrent.CompletableFuture;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.MUX;

/** Sends authorization requests over a MUX and says what came of them. */
public final class AuthorizationSender {

    public enum Outcome { APPROVED, DECLINED, NO_ANSWER }

    public AuthorizationSender(MUX mux, long timeoutMillis) {
    }

    public Outcome authorize(ISOMsg request) throws ISOException {
        throw new UnsupportedOperationException("write authorize");
    }

    public CompletableFuture<Outcome> authorizeAsync(ISOMsg request) throws ISOException {
        throw new UnsupportedOperationException("write authorizeAsync");
    }
}

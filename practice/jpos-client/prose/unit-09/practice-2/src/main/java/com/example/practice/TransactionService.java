package com.example.practice;

import java.util.concurrent.TimeoutException;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.MUX;

/** Sends a request and turns the host's reply, or its silence, into an outcome. */
public final class TransactionService {

    /** The host answered, and declined with a response code. */
    public static final class TransactionDeclinedException extends RuntimeException {
        public TransactionDeclinedException(String message, String responseCode) {
            super(message);
            throw new UnsupportedOperationException("write TransactionDeclinedException");
        }

        public String getResponseCode() {
            throw new UnsupportedOperationException("write getResponseCode");
        }
    }

    public TransactionService(MUX mux, long timeoutMillis) {
    }

    /** The approved reply to `request`. */
    public ISOMsg send(ISOMsg request) throws ISOException, TimeoutException {
        throw new UnsupportedOperationException("write send");
    }
}

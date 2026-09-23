package com.example.practice;

import java.util.concurrent.TimeoutException;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.MUX;

/** Sends a request and turns the host's reply, or its silence, into an outcome. */
public final class TransactionService {

    /** The host answered, and declined with a response code. */
    public static final class TransactionDeclinedException extends RuntimeException {
        private final String responseCode;

        public TransactionDeclinedException(String message, String responseCode) {
            super(message);
            this.responseCode = responseCode;
        }

        public String getResponseCode() {
            return responseCode;
        }
    }

    private final MUX mux;
    private final long timeoutMillis;

    public TransactionService(MUX mux, long timeoutMillis) {
        this.mux = mux;
        this.timeoutMillis = timeoutMillis;
    }

    /** The approved reply to `request`. */
    public ISOMsg send(ISOMsg request) throws ISOException, TimeoutException {
        ISOMsg response = mux.request(request, timeoutMillis);
        String responseCode = response.getString(39);
        if (!"00".equals(responseCode)) {
            throw new TransactionDeclinedException("Transaction declined", responseCode);
        }
        return response;
    }
}

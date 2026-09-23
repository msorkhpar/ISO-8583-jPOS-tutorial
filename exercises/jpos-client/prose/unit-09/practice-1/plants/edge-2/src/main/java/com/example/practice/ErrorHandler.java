package com.example.practice;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import org.jpos.iso.ISOException;

/** One place that decides what a caller sees when the client fails. */
public final class ErrorHandler {

    /** What a caller is told about a failure. */
    public static final class ErrorResponse {
        private final String type;
        private final String message;
        private final int status;

        public ErrorResponse(String type, String message, int status) {
            this.type = type;
            this.message = message;
            this.status = status;
        }

        public String getType() { return type; }
        public String getMessage() { return message; }
        public int getStatus() { return status; }
    }

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

    /** The response a caller sees for `failure`. */
    public ErrorResponse handle(Throwable failure) {
        if (failure instanceof ISOException) {
            return new ErrorResponse("ISO-8583 Error", failure.getMessage(), 400);
        }
        if (failure instanceof IOException) {
            return new ErrorResponse("Network Error", "Failed to communicate with the server", 503);
        }
        if (failure instanceof TimeoutException) {
            return new ErrorResponse("Timeout Error", "The operation timed out", 408);
        }
        if (failure instanceof TransactionDeclinedException) {
            String code = ((TransactionDeclinedException) failure).getResponseCode();
            return new ErrorResponse("Transaction Declined",
                    "Transaction was declined with response code: " + code, 402);
        }
        return new ErrorResponse("Internal Server Error", failure.getMessage(), 500);
    }
}

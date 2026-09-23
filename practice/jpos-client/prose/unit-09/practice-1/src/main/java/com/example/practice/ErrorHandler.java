package com.example.practice;

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
        public TransactionDeclinedException(String message, String responseCode) {
            super(message);
            throw new UnsupportedOperationException("write TransactionDeclinedException");
        }

        public String getResponseCode() {
            throw new UnsupportedOperationException("write getResponseCode");
        }
    }

    /** The response a caller sees for `failure`. */
    public ErrorResponse handle(Throwable failure) {
        throw new UnsupportedOperationException("write handle");
    }
}

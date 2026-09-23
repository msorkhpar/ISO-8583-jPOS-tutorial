package com.example.practice;

import java.io.Serializable;
import org.jpos.iso.ISOMsg;
import org.jpos.transaction.TransactionParticipant;

/** A participant that turns every failure into an ISO-8583 error response. */
public final class ErrorHandlingParticipant implements TransactionParticipant {

    public static class ISO8583ParseException extends RuntimeException {
        public ISO8583ParseException(String message) {
            super(message);
        }
    }

    public static class TransactionProcessingException extends RuntimeException {
        public TransactionProcessingException(String message) {
            super(message);
        }
    }

    public static class NetworkCommunicationException extends RuntimeException {
        public NetworkCommunicationException(String message) {
            super(message);
        }
    }

    /** The error response for `failure`: its code in field 39, its message in field 63. */
    public static ISOMsg errorResponse(Exception failure) {
        throw new UnsupportedOperationException("write errorResponse");
    }

    @Override
    public int prepare(long id, Serializable context) {
        throw new UnsupportedOperationException("write prepare");
    }

    @Override
    public void commit(long id, Serializable context) {
    }

    @Override
    public void abort(long id, Serializable context) {
    }
}

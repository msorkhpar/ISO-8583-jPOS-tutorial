package com.example.practice;

import java.io.Serializable;
import org.jpos.iso.ISOMsg;
import org.jpos.transaction.Context;
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
        String code;
        String message = failure.getMessage();
        if (failure instanceof ISO8583ParseException) {
            code = "01";
        } else if (failure instanceof TransactionProcessingException) {
            code = "02";
        } else if (failure instanceof NetworkCommunicationException) {
            code = "03";
        } else {
            code = "99";
            message = "An unexpected error occurred";
        }
        ISOMsg response = new ISOMsg();
        response.set(39, code);
        response.set(63, message);
        return response;
    }

    @Override
    public int prepare(long id, Serializable context) {
        Context ctx = (Context) context;
        try {
            ISOMsg msg = (ISOMsg) ctx.get("REQUEST");
            if (msg == null) {
                throw new ISO8583ParseException("Invalid or missing ISO message");
            }
            String processingCode = msg.getString(3);
            if ("999999".equals(processingCode)) {
                throw new TransactionProcessingException("Invalid processing code");
            }
            if ("888888".equals(processingCode)) {
                throw new NetworkCommunicationException("Network is unavailable");
            }
            return PREPARED | NO_JOIN;
        } catch (RuntimeException failure) {
            ctx.put("RESPONSE", errorResponse(failure));
            return ABORTED | NO_JOIN;
        }
    }

    @Override
    public void commit(long id, Serializable context) {
    }

    @Override
    public void abort(long id, Serializable context) {
    }
}

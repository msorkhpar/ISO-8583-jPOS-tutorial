package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.net.ConnectException;
import java.util.concurrent.TimeoutException;
import org.jpos.iso.ISOException;
import org.junit.jupiter.api.Test;

class ErrorHandlerTest {

    private final ErrorHandler handler = new ErrorHandler();

    private static void expect(ErrorHandler.ErrorResponse r, String type, String message, int status) {
        assertEquals(type, r.getType());
        assertEquals(message, r.getMessage());
        assertEquals(status, r.getStatus());
    }

    @Test
    void eachKindOfFailureGetsItsOwnResponse() {
        expect(handler.handle(new ISOException("Invalid message format")),
                "ISO-8583 Error", "Invalid message format", 400);
        expect(handler.handle(new IOException("broken pipe")),
                "Network Error", "Failed to communicate with the server", 503);
        expect(handler.handle(new TimeoutException("no reply")),
                "Timeout Error", "The operation timed out", 408);
        ErrorHandler.TransactionDeclinedException declined =
                new ErrorHandler.TransactionDeclinedException("Insufficient funds", "51");
        assertEquals("51", declined.getResponseCode());
        assertEquals("Insufficient funds", declined.getMessage());
        expect(handler.handle(declined),
                "Transaction Declined", "Transaction was declined with response code: 51", 402);
    }

    @Test
    void aNarrowerNetworkFailureIsStillANetworkError() {
        expect(handler.handle(new ConnectException("Connection refused")),
                "Network Error", "Failed to communicate with the server", 503);
    }

    @Test
    void anUnexpectedFailureRevealsNothingOfItsCause() {
        ErrorHandler.ErrorResponse r =
                handler.handle(new IllegalStateException("pool exhausted at db-7.internal"));
        expect(r, "Internal Server Error", "An unexpected error occurred", 500);
        assertFalse(r.getMessage().contains("db-7"));
    }
}

package com.example.practice;

import java.time.LocalDate;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;

/** Carries an ISO-8583 request to a REST payment gateway and its reply back. */
public final class GatewayBridge {

    /** What the gateway's API expects. */
    public record PaymentRequest(String amount, String cardNumber, String expirationDate,
                                 String transactionType, String transactionDate) {
    }

    /** What the gateway's API answers. */
    public record GatewayResponse(String status, String transactionId, String errorMessage) {
    }

    private GatewayBridge() {
    }

    /** The gateway request for an ISO-8583 request received on {@code today}. */
    public static PaymentRequest toGateway(ISOMsg request, LocalDate today) {
        throw new UnsupportedOperationException("write toGateway");
    }

    /** The ISO-8583 response to {@code request}, from the gateway's reply. */
    public static ISOMsg toIso(ISOMsg request, GatewayResponse reply) throws ISOException {
        throw new UnsupportedOperationException("write toIso");
    }
}

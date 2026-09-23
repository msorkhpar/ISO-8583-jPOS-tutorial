package com.example.practice;

import java.math.BigDecimal;
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
        String amount = new BigDecimal(request.getString(4)).movePointLeft(2).toPlainString();
        String yymm = request.getString(14);
        String expiry = yymm.substring(2, 4) + "/" + yymm.substring(0, 2);
        return new PaymentRequest(amount, request.getString(2), expiry,
            type(request.getString(3)), date(request.getString(13), today));
    }

    private static String type(String processingCode) {
        switch (processingCode) {
            case "000000":
                return "PURCHASE";
            case "200000":
                return "REFUND";
            default:
                return "PURCHASE";
        }
    }

    private static String date(String mmdd, LocalDate today) {
        int month = Integer.parseInt(mmdd.substring(0, 2));
        int day = Integer.parseInt(mmdd.substring(2, 4));
        LocalDate date = LocalDate.of(today.getYear(), month, day);
        if (date.isAfter(today)) {
            date = date.minusYears(1);
        }
        return date.toString();
    }

    /** The ISO-8583 response to {@code request}, from the gateway's reply. */
    public static ISOMsg toIso(ISOMsg request, GatewayResponse reply) throws ISOException {
        ISOMsg response = (ISOMsg) request.clone();
        response.setResponseMTI();
        if ("SUCCESS".equals(reply.status())) {
            response.set(39, "00");
            response.set(38, reply.transactionId());
        } else {
            response.set(39, "05");
            response.unset(38);
            response.set(63, reply.errorMessage());
        }
        return response;
    }
}

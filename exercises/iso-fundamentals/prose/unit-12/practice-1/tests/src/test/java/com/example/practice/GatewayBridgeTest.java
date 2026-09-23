package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import org.jpos.iso.ISOMsg;
import org.junit.jupiter.api.Test;

class GatewayBridgeTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 3, 10);

    private static ISOMsg request(String processingCode, String amount, String mmdd) throws Exception {
        ISOMsg m = new ISOMsg();
        m.setMTI("0200");
        m.set(2, "4111111111111111");
        m.set(3, processingCode);
        m.set(4, amount);
        m.set(11, "000042");
        m.set(13, mmdd);
        m.set(14, "2405");
        return m;
    }

    @Test
    void mapsARequestIntoTheGatewaysFormat() throws Exception {
        GatewayBridge.PaymentRequest sent =
            GatewayBridge.toGateway(request("000000", "000000100000", "0215"), TODAY);
        assertEquals(new GatewayBridge.PaymentRequest(
            "1000.00", "4111111111111111", "05/24", "PURCHASE", "2026-02-15"), sent);
        GatewayBridge.PaymentRequest refund =
            GatewayBridge.toGateway(request("200000", "000000000005", "0310"), TODAY);
        assertEquals("0.05", refund.amount());
        assertEquals("REFUND", refund.transactionType());
        assertEquals("2026-03-10", refund.transactionDate());
    }

    @Test
    void answersTheRequestFromTheGatewaysReply() throws Exception {
        ISOMsg original = request("000000", "000000100000", "0215");
        ISOMsg approved = GatewayBridge.toIso(original,
            new GatewayBridge.GatewayResponse("SUCCESS", "123456", null));
        assertEquals("0210", approved.getMTI());
        assertEquals("00", approved.getString(39));
        assertEquals("123456", approved.getString(38));
        assertEquals("000042", approved.getString(11));

        ISOMsg declined = GatewayBridge.toIso(original,
            new GatewayBridge.GatewayResponse("FAILED", null, "Insufficient funds"));
        assertEquals("0210", declined.getMTI());
        assertEquals("05", declined.getString(39));
        assertEquals("Insufficient funds", declined.getString(63));
        assertFalse(declined.hasField(38));
        assertEquals("0200", original.getMTI());
    }

    @Test
    void aDayStillAheadBelongsToLastYear() throws Exception {
        GatewayBridge.PaymentRequest sent =
            GatewayBridge.toGateway(request("000000", "000000001000", "1231"), TODAY);
        assertEquals("2025-12-31", sent.transactionDate());
    }

    @Test
    void refusesAProcessingCodeTheGatewayHasNoTypeFor() throws Exception {
        ISOMsg cashAdvance = request("010000", "000000001000", "0215");
        assertThrows(IllegalArgumentException.class, () -> GatewayBridge.toGateway(cashAdvance, TODAY));
    }
}

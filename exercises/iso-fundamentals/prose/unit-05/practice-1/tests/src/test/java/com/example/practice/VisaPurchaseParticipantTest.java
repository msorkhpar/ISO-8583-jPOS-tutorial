package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.jpos.iso.ISOMsg;
import org.jpos.transaction.Context;
import org.jpos.transaction.TransactionConstants;
import org.junit.jupiter.api.Test;

class VisaPurchaseParticipantTest {

    private final VisaPurchaseParticipant participant =
        new VisaPurchaseParticipant("VISAMERCHANT1", "VISA STORE/NEW YORK");

    private static ISOMsg request(String mti, String processingCode) throws Exception {
        ISOMsg request = new ISOMsg();
        request.setMTI(mti);
        request.set(2, "4111111111111111");
        if (processingCode != null) {
            request.set(3, processingCode);
        }
        request.set(4, "000000010000");
        request.set(11, "000321");
        return request;
    }

    private static Context holding(ISOMsg request) {
        Context context = new Context();
        context.put("REQUEST", request);
        return context;
    }

    private void aborts(ISOMsg request) {
        Context context = holding(request);
        assertEquals(TransactionConstants.ABORTED, participant.prepare(1L, context));
        assertEquals("Invalid transaction type", context.get("RESULT"));
        assertNull(context.get("RESPONSE"));
    }

    @Test
    void namesTheTransactionTypeFromTheProcessingCode() {
        assertEquals("Purchase", VisaPurchaseParticipant.transactionType("000000"));
        assertEquals("Cash Advance", VisaPurchaseParticipant.transactionType("010000"));
        assertEquals("Void", VisaPurchaseParticipant.transactionType("020000"));
        assertEquals("Refund", VisaPurchaseParticipant.transactionType("200000"));
        assertNull(VisaPurchaseParticipant.transactionType("310000"));
        assertNull(VisaPurchaseParticipant.transactionType(null));
    }

    @Test
    void preparesAnApprovedResponseForAPurchase() throws Exception {
        Context context = holding(request("0100", "000000"));
        assertEquals(TransactionConstants.PREPARED, participant.prepare(1L, context));
        ISOMsg response = (ISOMsg) context.get("RESPONSE");
        assertEquals("0110", response.getMTI());
        assertEquals("00", response.getString(39));
        assertEquals("VISAMERCHANT1  ", response.getString(42));
        assertEquals("VISA STORE/NEW YORK                     ", response.getString(43));
        assertEquals("4111111111111111", response.getString(2));
        assertEquals("000000010000", response.getString(4));
        assertEquals("000321", response.getString(11));

        aborts(request("0100", "200000"));
        aborts(request("0100", "010000"));
        aborts(request("0200", "000000"));
        aborts(request("0100", null));
    }

    @Test
    void aPurchaseFromAnyAccountIsStillAPurchase() throws Exception {
        assertEquals("Purchase", VisaPurchaseParticipant.transactionType("003000"));
        Context context = holding(request("0100", "003000"));
        assertEquals(TransactionConstants.PREPARED, participant.prepare(1L, context));
        assertEquals("00", ((ISOMsg) context.get("RESPONSE")).getString(39));
    }

    @Test
    void leavesTheRequestAsItArrived() throws Exception {
        ISOMsg request = request("0100", "000000");
        Context context = holding(request);
        participant.prepare(1L, context);
        assertNotSame(request, context.get("RESPONSE"));
        assertEquals("0100", request.getMTI());
        assertFalse(request.hasField(39));
        assertFalse(request.hasField(42));
    }
}

package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import org.jpos.transaction.Context;
import org.jpos.transaction.TransactionParticipant;
import org.junit.jupiter.api.Test;

class FraudScreeningParticipantTest {

    private static final String CARD = "4000000000000002";

    private final FraudScreeningParticipant.FraudService service =
            mock(FraudScreeningParticipant.FraudService.class);
    private final FraudScreeningParticipant participant = new FraudScreeningParticipant(service);

    private static Context purchase(String amount) {
        Context ctx = new Context();
        ctx.put("cardNumber", CARD);
        if (amount != null) {
            ctx.put("amount", amount);
        }
        return ctx;
    }

    @Test
    void aValidAmountIsScreenedAndTheVerdictDecides() {
        when(service.isFraudulent(CARD, new BigDecimal("100.00"))).thenReturn(false);
        Context clean = purchase("100.00");
        assertEquals(TransactionParticipant.PREPARED, participant.prepare(1L, clean));
        assertNull(clean.get("fraudDetected"));
        verify(service).isFraudulent(CARD, new BigDecimal("100.00"));

        when(service.isFraudulent(CARD, new BigDecimal("5000.00"))).thenReturn(true);
        Context fraud = purchase("5000.00");
        assertEquals(TransactionParticipant.ABORTED, participant.prepare(2L, fraud));
        assertEquals(Boolean.TRUE, fraud.get("fraudDetected"));
    }

    @Test
    void aMissingMalformedOrNegativeAmountAbortsWithoutAskingTheService() {
        Context missing = purchase(null);
        assertEquals(TransactionParticipant.ABORTED, participant.prepare(1L, missing));
        assertEquals("Amount is missing", missing.get("error"));

        Context malformed = purchase("not-a-number");
        assertEquals(TransactionParticipant.ABORTED, participant.prepare(2L, malformed));
        assertEquals("Invalid amount format", malformed.get("error"));

        Context negative = purchase("-50.00");
        assertEquals(TransactionParticipant.ABORTED, participant.prepare(3L, negative));
        assertEquals("Amount must be positive", negative.get("error"));

        verifyNoInteractions(service);
    }

    @Test
    void aZeroAmountIsNotPositive() {
        Context zero = purchase("0.00");
        assertEquals(TransactionParticipant.ABORTED, participant.prepare(1L, zero));
        assertEquals("Amount must be positive", zero.get("error"));
        verifyNoInteractions(service);
    }

    @Test
    void aFailingFraudServiceAbortsInsteadOfThrowing() {
        when(service.isFraudulent(any(), any())).thenThrow(new IllegalStateException("timed out"));
        Context ctx = purchase("100.00");
        assertEquals(TransactionParticipant.ABORTED, participant.prepare(1L, ctx));
        assertEquals("Fraud check unavailable", ctx.get("error"));
        assertNull(ctx.get("fraudDetected"));
    }
}

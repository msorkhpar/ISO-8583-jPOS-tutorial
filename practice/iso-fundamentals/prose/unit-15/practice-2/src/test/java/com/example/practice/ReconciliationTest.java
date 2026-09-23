package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ReconciliationTest {

    @Test
    void anOutOfBalanceDayReportsItsDifferenceAndItsMissingIds() {
        Reconciliation.Result result = Reconciliation.check(
            new BigDecimal("300.00"),
            List.of("T3", "T1", "T2"),
            Map.of("T1", new BigDecimal("100.00"), "T3", new BigDecimal("150.00")));
        assertFalse(result.balanced());
        assertEquals(0, new BigDecimal("50.00").compareTo(result.difference()));
        assertEquals(List.of("T2"), result.missingIds());
    }

    @Test
    void amountsThatDifferOnlyInScaleBalance() {
        Reconciliation.Result result = Reconciliation.check(
            new BigDecimal("100.0"), List.of("T1"), Map.of("T1", new BigDecimal("100.00")));
        assertTrue(result.balanced());
        assertEquals(0, BigDecimal.ZERO.compareTo(result.difference()));
    }

    @Test
    void aProcessedTransactionNobodyExpectedIsNotMissing() {
        Reconciliation.Result result = Reconciliation.check(
            new BigDecimal("100.00"),
            List.of("T1"),
            Map.of("T1", new BigDecimal("60.00"), "T9", new BigDecimal("40.00")));
        assertEquals(List.of(), result.missingIds());
    }
}

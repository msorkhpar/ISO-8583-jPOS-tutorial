package com.example.practice;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** One business day's settlement, checked against what was processed. */
public final class Reconciliation {

    /** What the check found. */
    public record Result(boolean balanced, BigDecimal difference, List<String> missingIds) {
    }

    private Reconciliation() {
    }

    /** Compare the network's expected total and ids with what was processed. */
    public static Result check(BigDecimal expectedTotal, List<String> expectedIds,
                               Map<String, BigDecimal> processed) {
        throw new UnsupportedOperationException("write check");
    }
}

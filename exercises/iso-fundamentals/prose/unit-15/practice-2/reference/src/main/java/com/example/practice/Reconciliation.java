package com.example.practice;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

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
        BigDecimal actual = processed.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal difference = expectedTotal.subtract(actual);
        TreeSet<String> missing = new TreeSet<>(expectedIds);
        missing.removeAll(processed.keySet());
        return new Result(difference.compareTo(BigDecimal.ZERO) == 0, difference, List.copyOf(missing));
    }
}

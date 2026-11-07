package com.example.practice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The summary of one load run. Each sample is one call to mux.request: how long the calling
 * thread waited, in microseconds, and whether an answer came back (false: request returned null).
 * The threads add samples in the order their requests finish.
 */
public final class LoadReport {
    public record Sample(long micros, boolean answered) {}

    public final int count;
    public final int nulls;
    public final long p50, p95, p99, max;              // microseconds

    private LoadReport(int count, int nulls, List<Long> latencies) {
        this.count = count;
        this.nulls = nulls;
        this.p50 = percentile(latencies, 50);
        this.p95 = percentile(latencies, 95);
        this.p99 = percentile(latencies, 99);
        this.max = percentile(latencies, 100);
    }

    public static LoadReport of(List<Sample> samples) {
        return new LoadReport(samples.size(), nulls(samples), latencies(samples));
    }

    @Override
    public String toString() {
        return String.format("n=%d nulls=%d p50=%d p95=%d p99=%d max=%d (us)", count, nulls, p50, p95, p99, max);
    }

    /** How many requests came back with no answer. */
    static int nulls(List<Sample> samples) {
        // TODO
        throw new IllegalStateException("TODO: nulls");
    }

    /** The waits the percentiles are taken from, ready for percentile(). */
    static List<Long> latencies(List<Sample> samples) {
        // TODO: which waits belong in a latency report, and in what order percentile() needs them.
        throw new IllegalStateException("TODO: latencies");
    }

    /** The smallest wait such that at least p percent of all waits are at or below it. */
    static long percentile(List<Long> latencies, int p) {
        // TODO: nearest rank, as the comment above says. p is 1 to 100.
        throw new IllegalStateException("TODO: percentile");
    }
}

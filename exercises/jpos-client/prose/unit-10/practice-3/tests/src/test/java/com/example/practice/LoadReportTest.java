package com.example.practice;

import com.example.practice.LoadReport.Sample;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoadReportTest {

    @Test
    void aSteadyRunIsSummarised() {
        List<Sample> samples = new ArrayList<>();
        for (int i = 1; i <= 200; i++)
            samples.add(new Sample(500L + 10L * i, true));
        LoadReport r = LoadReport.of(samples);
        assertEquals(200, r.count);
        assertEquals(0, r.nulls);
        assertEquals(2500, r.max);
        assertTrue(500 < r.p50 && r.p50 <= r.p95 && r.p95 <= r.p99 && r.p99 <= r.max, r.toString());
    }

    @Test
    void requestsWithNoAnswerAreCounted() {
        List<Sample> samples = new ArrayList<>();
        for (int i = 0; i < 300; i++)
            samples.add(new Sample(700L + i, i % 60 != 59));
        LoadReport r = LoadReport.of(samples);
        assertEquals(300, r.count);
        assertEquals(5, r.nulls);
    }

    @Test
    void requestsWithNoAnswerAreInTheTail() {
        List<Sample> samples = new ArrayList<>();
        for (int i = 0; i < 1500; i++) {
            boolean answered = i % 40 != 7;                       // 2.5% get no answer
            samples.add(new Sample(answered ? 900L + i % 300 : 1_500_000L, answered));
        }
        LoadReport r = LoadReport.of(samples);
        assertEquals(1_500_000L, r.p99, "a null waited the whole timeout: " + r);
        assertEquals(1_500_000L, r.max);
    }

    @Test
    void samplesArriveInCompletionOrder() {
        List<Sample> samples = new ArrayList<>();
        for (int i = 1; i <= 400; i++)
            samples.add(new Sample(2000L + 5L * i, true));
        Collections.shuffle(samples, new Random(8583));
        LoadReport r = LoadReport.of(samples);
        assertEquals(3000, r.p50, r.toString());
        assertEquals(3900, r.p95, r.toString());
        assertEquals(4000, r.max, r.toString());
    }

    @Test
    void percentilesAreNearestRank() {
        List<Sample> samples = new ArrayList<>();
        for (int i = 1; i <= 20; i++)
            samples.add(new Sample(1000L * i, true));
        LoadReport r = LoadReport.of(samples);
        assertEquals(10_000, r.p50, r.toString());
        assertEquals(19_000, r.p95, r.toString());
        assertEquals(20_000, r.p99, r.toString());
    }
}

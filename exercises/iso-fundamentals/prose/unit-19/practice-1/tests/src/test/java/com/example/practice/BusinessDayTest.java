package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Set;
import java.util.TimeZone;

import org.jpos.iso.ISOMsg;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class BusinessDayTest {

    private static TimeZone saved;

    @BeforeAll
    static void aServerFarFromTheNetwork() {
        saved = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"));
    }

    @AfterAll
    static void restore() {
        TimeZone.setDefault(saved);
    }

    /** The London network: cutover 16:00 London time, closed on 25 and 28 December 2026. */
    private static BusinessDay london() {
        return new BusinessDay(ZoneId.of("Europe/London"), LocalTime.of(16, 0),
                Set.of(LocalDate.of(2026, 12, 25), LocalDate.of(2026, 12, 28)));
    }

    private static LocalDate settles(String instant) {
        return london().settles(Instant.parse(instant));
    }

    // ---- main cases ----

    @Test
    void aWinterWeekdayBeforeTheCutoverSettlesThatDay() {
        assertEquals(LocalDate.of(2026, 11, 17), settles("2026-11-17T09:20:00Z"));     // Tue 09:20
        assertEquals(LocalDate.of(2026, 11, 17), settles("2026-11-17T00:00:00Z"));     // Tue 00:00
    }

    @Test
    void aWinterWeekdayAfterTheCutoverSettlesTheNextDay() {
        assertEquals(LocalDate.of(2026, 11, 19), settles("2026-11-18T17:45:00Z"));     // Wed 17:45
        assertEquals(LocalDate.of(2026, 11, 19), settles("2026-11-18T23:59:59Z"));     // Wed 23:59:59
    }

    @Test
    void withoutTheHostsDateTheCalendarGivesDe15() throws Exception {
        assertEquals("0105", london().de15For(null, Instant.parse("2027-01-05T11:00:00Z")));
        assertEquals("0107", london().de15For(null, Instant.parse("2027-01-06T16:10:00Z")));
        ISOMsg answer = new ISOMsg("0210");
        answer.set(39, "00");                                 // an answer without DE 15
        assertEquals("0108", london().de15For(answer, Instant.parse("2027-01-07T16:20:00Z")));
    }

    // ---- edge cases ----

    @Test
    void theCutoverMomentBelongsToTheNextDay() {
        assertEquals(LocalDate.of(2026, 11, 18), settles("2026-11-17T16:00:00Z"));     // Tue 16:00:00
        assertEquals(LocalDate.of(2026, 11, 17), settles("2026-11-17T15:59:59Z"));     // Tue 15:59:59
    }

    @Test
    void inSummerTheCutoverComesAnHourEarlierInUtc() {
        assertEquals(LocalDate.of(2026, 7, 15), settles("2026-07-14T15:30:00Z"));      // Tue 16:30 BST
        assertEquals(LocalDate.of(2026, 7, 14), settles("2026-07-14T14:59:59Z"));      // Tue 15:59:59 BST
    }

    @Test
    void aSaturdaySettlesOnMonday() {
        assertEquals(LocalDate.of(2026, 11, 23), settles("2026-11-21T11:00:00Z"));     // Sat
        assertEquals(LocalDate.of(2026, 11, 23), settles("2026-11-20T16:05:00Z"));     // Fri after cutover
        assertEquals(LocalDate.of(2026, 11, 23), settles("2026-11-22T15:00:00Z"));     // Sun
    }

    @Test
    void holidaysAroundAWeekendRollOnToTheNextWorkingDay() {
        assertEquals(LocalDate.of(2026, 12, 29), settles("2026-12-24T16:30:00Z"));     // Thu after cutover
        assertEquals(LocalDate.of(2026, 12, 29), settles("2026-12-25T10:00:00Z"));     // Fri, holiday
        assertEquals(LocalDate.of(2026, 12, 24), settles("2026-12-23T16:30:00Z"));     // Wed after cutover
    }

    @Test
    void de15CarriesTheDayOfTheMonth() {
        assertEquals("0203", BusinessDay.de15(LocalDate.of(2027, 2, 3)));
        assertEquals("1130", BusinessDay.de15(LocalDate.of(2026, 11, 30)));
    }

    @Test
    void theHostsSettlementDateIsKept() throws Exception {
        ISOMsg answer = new ISOMsg("0210");
        answer.set(15, "0106");
        answer.set(39, "00");
        assertEquals("0106", london().de15For(answer, Instant.parse("2027-01-05T15:58:00Z")));
    }
}

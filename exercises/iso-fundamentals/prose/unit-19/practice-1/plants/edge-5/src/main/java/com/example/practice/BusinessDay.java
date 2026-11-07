package com.example.practice;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

import org.jpos.iso.ISOMsg;

/** Gives each transaction on one network link the settlement date the network counts it on. */
public final class BusinessDay {
    private final ZoneId zone;            // where the network's operations guide sets its cutover
    private final LocalTime cutover;      // from this time on, a transaction belongs to the next day
    private final Set<LocalDate> holidays;

    public BusinessDay(ZoneId zone, LocalTime cutover, Set<LocalDate> holidays) {
        this.zone = zone;
        this.cutover = cutover;
        this.holidays = Set.copyOf(holidays);
    }

    /** The DE 15 to record for a transaction made at {@code when}; {@code response} may be null. */
    public String de15For(ISOMsg response, Instant when) {
        String fromHost = hostDate(response);
        return fromHost != null ? fromHost : de15(settles(when));
    }

    /** The business day on which a transaction made at {@code when} settles. */
    public LocalDate settles(Instant when) {
        return rollForward(dayOf(when));
    }

    /** True on the days the network settles. */
    public boolean settlesOn(LocalDate day) {
        DayOfWeek d = day.getDayOfWeek();
        return d != DayOfWeek.SATURDAY && d != DayOfWeek.SUNDAY && !holidays.contains(day);
    }

    /** The network's day this moment falls in, before weekends and holidays are considered. */
    LocalDate dayOf(Instant when) {
        ZonedDateTime local = when.atZone(zone);
        LocalDate day = local.toLocalDate();
        return local.toLocalTime().isBefore(cutover) ? day : day.plusDays(1);
    }

    /** The first day, this one or a later one, on which the network settles. */
    LocalDate rollForward(LocalDate day) {
        while (!settlesOn(day))
            day = day.plusDays(1);
        return day;
    }

    /** DE 15 as this link carries it, in the 1987 layout. */
    static String de15(LocalDate day) {
        return day.format(DateTimeFormatter.ofPattern("MMDD"));
    }

    /** The settlement date the host gave in its answer, or null when there is none to keep. */
    String hostDate(ISOMsg response) {
        return response != null && response.hasField(15) ? response.getString(15) : null;
    }
}

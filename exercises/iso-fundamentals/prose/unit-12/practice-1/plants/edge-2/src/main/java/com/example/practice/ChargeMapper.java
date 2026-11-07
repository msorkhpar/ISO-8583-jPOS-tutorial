package com.example.practice;

import org.jpos.iso.ISOCurrency;
import org.jpos.iso.ISODate;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

/** Turns a 1987 purchase or refund from a terminal into the REST gateway's charge. */
public final class ChargeMapper {

    /** The body of the gateway's POST /charges. */
    public record Charge(String kind, BigDecimal amount, String currency, LocalDateTime localTime, String terminal) {
    }

    /** The currencies the gateway's contract accepts (field 49 codes). */
    static final Set<String> ACCEPTED = Set.of("978", "826", "756", "152", "414");

    /**
     * The charge for request {@code m}, mapped at {@code now} for a terminal in {@code terminalZone}.
     * Throws ISOException for a request the gateway can't take.
     */
    public Charge toCharge(ISOMsg m, Instant now, ZoneId terminalZone) throws ISOException {
        String kind = kind(m.getString(3));
        BigDecimal amount = amount(m.getString(4), m.getString(49));
        String currency = ISOCurrency.getCurrency(m.getString(49)).getAlphaCode();
        LocalDateTime localTime = localTime(m.getString(13), m.getString(12), now, terminalZone);
        return new Charge(kind, amount, currency, localTime, m.getString(41));
    }

    private static final Map<String, String> KINDS = Map.of("00", "sale", "20", "refund");

    /** The gateway's kind, "sale" or "refund", for processing code {@code de3}. */
    String kind(String de3) throws ISOException {
        String kind = KINDS.get(de3.substring(0, 2));
        if (kind == null)
            throw new ISOException("no gateway kind for processing code " + de3);
        return kind;
    }

    /** Field 4 as the decimal amount the cardholder agreed to, in the currency of field 49. */
    BigDecimal amount(String de4, String de49) throws ISOException {
        return ISOCurrency.parseFromISO87String(de4, de49);
    }

    /** Fields 13 (MMDD) and 12 (hhmmss), the terminal's local time, with the year filled in. */
    LocalDateTime localTime(String de13, String de12, Instant now, ZoneId terminalZone) {
        return LocalDateTime.ofInstant(
                ISODate.parseISODate(de13 + de12, now.toEpochMilli(), TimeZone.getTimeZone(terminalZone)).toInstant(),
                terminalZone);
    }
}

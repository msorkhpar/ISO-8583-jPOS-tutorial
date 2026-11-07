package com.example.practice;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;

import java.time.Clock;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.function.LongSupplier;

/** Builds the requests one ATM sends to its acquirer on one link. */
public class AtmRequests {
    private static final DateTimeFormatter MMDDHHMMSS = DateTimeFormatter.ofPattern("MMddHHmmss");

    private final String atmId;              // field 41
    private final Clock clock;               // the ATM's clock, in the ATM's own time zone
    private final LongSupplier counter;      // the link's counter: 1, 2, 3, ... shared by every caller

    public AtmRequests(String atmId, Clock clock, LongSupplier counter) {
        this.atmId = atmId;
        this.clock = clock;
        this.counter = counter;
    }

    public ISOMsg balanceInquiry(String pan) throws ISOException {
        ISOMsg m = stamped("0100");
        m.set(2, pan);
        m.set(3, "310000");
        m.set(41, atmId);
        return m;
    }

    public ISOMsg withdrawal(String pan, long amountMinor, String currency) throws ISOException {
        ISOMsg m = stamped("0200");
        m.set(2, pan);
        m.set(3, "010000");
        m.set(4, ISOUtil.zeropad(amountMinor, 12));
        m.set(41, atmId);
        m.set(49, currency);
        return m;
    }

    /** The 0800 the ATM sends when it comes into service (field 70 = 001). */
    public ISOMsg signOn() throws ISOException {
        ISOMsg m = stamped("0800");
        m.set(70, "001");
        return m;
    }

    /** A new message with what every message on this link carries besides its own fields. */
    ISOMsg stamped(String mti) throws ISOException {
        ISOMsg m = new ISOMsg(mti);
        m.set(7, transmissionTime());
        m.set(11, nextStan());
        return m;
    }

    /** Field 7 for a message sent now. */
    String transmissionTime() {
        return clock.instant().atZone(ZoneOffset.UTC).format(MMDDHHMMSS);
    }

    /** Field 11 for the next message on this link. */
    String nextStan() {
        long n = counter.getAsLong();
        return ISOUtil.zeropad((n - 1) % 999999 + 1, 6);
    }
}

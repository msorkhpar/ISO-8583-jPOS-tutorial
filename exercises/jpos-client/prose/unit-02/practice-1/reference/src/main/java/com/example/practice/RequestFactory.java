package com.example.practice;

import java.time.Clock;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;

/** Builds client requests on one set of common fields. */
public final class RequestFactory {

    private static final DateTimeFormatter FIELD_7 =
            DateTimeFormatter.ofPattern("MMddHHmmss").withZone(ZoneOffset.UTC);

    private final String terminalId;
    private final Clock clock;
    private int traceNumber;

    public RequestFactory(String terminalId, Clock clock, int lastTraceNumber) {
        this.terminalId = terminalId;
        this.clock = clock;
        this.traceNumber = lastTraceNumber;
    }

    /** A request of type `mti` with the fields every request shares. */
    public ISOMsg create(String mti) throws ISOException {
        ISOMsg msg = new ISOMsg();
        msg.setMTI(mti);
        msg.set(7, FIELD_7.format(clock.instant()));
        msg.set(11, String.format("%06d", nextTraceNumber()));
        msg.set(41, terminalId);
        return msg;
    }

    public ISOMsg authorization(String pan, long amountMinor) throws ISOException {
        return cardRequest("0100", pan, amountMinor);
    }

    public ISOMsg financial(String pan, long amountMinor) throws ISOException {
        return cardRequest("0200", pan, amountMinor);
    }

    public ISOMsg reversal(ISOMsg original) throws ISOException {
        ISOMsg msg = create("0400");
        for (int field : new int[] {2, 3, 4, 49}) {
            msg.set(field, original.getString(field));
        }
        msg.set(90, original.getMTI() + original.getString(11) + original.getString(7) + "0".repeat(22));
        return msg;
    }

    private ISOMsg cardRequest(String mti, String pan, long amountMinor) throws ISOException {
        ISOMsg msg = create(mti);
        msg.set(2, pan);
        msg.set(3, "000000");
        msg.set(4, String.format("%012d", amountMinor));
        msg.set(49, "840");
        return msg;
    }

    private int nextTraceNumber() {
        traceNumber = traceNumber >= 999999 ? 1 : traceNumber + 1;
        return traceNumber;
    }
}

package com.example.practice;

import org.jpos.iso.ISODate;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;
import org.jpos.transaction.Context;
import org.jpos.transaction.ContextConstants;
import org.jpos.transaction.TransactionParticipant;

import java.io.Serializable;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.TimeZone;

/**
 * Lets a request through only when it was sent within WINDOW of the server's clock, and records
 * when it was sent for the participants after it.
 */
public class TransmissionWindow implements TransactionParticipant {
    static final Duration WINDOW = Duration.ofMinutes(5);
    static final String SENT_AT = "SENT_AT";
    private static final int[] ECHOED = { 0, 2, 3, 4, 7, 11, 12, 13, 32, 37, 41, 42, 49, 70 };

    private final Clock clock;

    public TransmissionWindow() {
        this(Clock.systemUTC());
    }

    TransmissionWindow(Clock clock) {
        this.clock = clock;
    }

    @Override
    public int prepare(long id, Serializable context) {
        Context ctx = (Context) context;
        ISOMsg request = ctx.get(ContextConstants.REQUEST.toString());
        Instant sent = sentAt(request.getString(7));
        if (sent != null && withinWindow(sent)) {
            ctx.put(SENT_AT, sent);
            return PREPARED | NO_JOIN;
        }
        try {
            ISOMsg response = (ISOMsg) request.clone(ECHOED);
            response.setResponseMTI();
            response.set(39, "30");
            ctx.put(ContextConstants.RESPONSE.toString(), response);
        } catch (ISOException e) {
            ctx.log(e);
        }
        return ABORTED;
    }

    /** When the request was sent, from field 7; null when field 7 doesn't say. */
    Instant sentAt(String field7) {
        if (field7 == null || field7.length() != 10 || !ISOUtil.isNumeric(field7, 10))
            return null;
        return ISODate.parseISODate(field7, clock.millis()).toInstant();
    }

    /** Whether a request sent at that moment may still be processed. */
    boolean withinWindow(Instant sent) {
        return Duration.between(sent, clock.instant()).abs().compareTo(WINDOW) <= 0;
    }
}

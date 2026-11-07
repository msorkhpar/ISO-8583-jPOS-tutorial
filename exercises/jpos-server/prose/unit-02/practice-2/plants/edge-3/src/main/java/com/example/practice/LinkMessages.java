package com.example.practice;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;

import java.time.Clock;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * The messages the server starts on the link to one terminal: echoes, sign-off notices before
 * maintenance, and text messages for the merchant.
 */
public class LinkMessages {
    private static final DateTimeFormatter FIELD_7 = DateTimeFormatter.ofPattern("MMddHHmmss");

    private final Map<String, ISOMsg> templates = new HashMap<>();
    private final Clock clock;
    private long lastStan;

    /**
     * @param lastStanUsed the last STAN the server sent on this link, as stored before a restart
     *                     (0 on a new link)
     */
    public LinkMessages(String institutionId, String terminalId, long lastStanUsed, Clock clock)
            throws ISOException {
        this.clock = clock;
        this.lastStan = lastStanUsed;
        templates.put("echo", template("0800", institutionId, terminalId, "301"));
        templates.put("sign-off", template("0800", institutionId, terminalId, "002"));
        templates.put("text", template("0620", institutionId, terminalId, null));
    }

    public ISOMsg echo() throws ISOException {
        return fromTemplate("echo");
    }

    public ISOMsg signOffNotice() throws ISOException {
        return fromTemplate("sign-off");
    }

    public ISOMsg merchantText(String text) throws ISOException {
        ISOMsg m = fromTemplate("text");
        m.set(63, text);
        return m;
    }

    private static ISOMsg template(String mti, String institutionId, String terminalId, String code)
            throws ISOException {
        ISOMsg t = new ISOMsg(mti);
        t.set(33, institutionId);
        t.set(41, terminalId);
        if (code != null)
            t.set(70, code);
        return t;
    }

    /** A message of the given kind, ready to send: its template's fields, plus when and which. */
    ISOMsg fromTemplate(String kind) throws ISOException {
        ISOMsg m = (ISOMsg) templates.get(kind).clone();
        m.set(7, clock.instant().atZone(ZoneOffset.UTC).format(FIELD_7));
        m.set(11, nextStan());
        return m;
    }

    /** The STAN of the next message the server sends on this link. */
    synchronized String nextStan() {
        lastStan = (lastStan + 1) % 1000000;
        return ISOUtil.zeropad(lastStan, 6);
    }
}

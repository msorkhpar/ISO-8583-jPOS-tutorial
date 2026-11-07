package com.example.practice;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOPackager;
import org.jpos.iso.ISOUtil;
import org.jpos.util.Log;
import org.jpos.util.LogEvent;
import org.jpos.util.Logger;

/**
 * What the client hands over when the host rejects a message as unreadable (response code 30):
 * an entry in the node's log, and a hex dump for the host's support desk.
 */
public class DeclineReport {
    private final Log log;
    private final ISOPackager packager;

    /** {@code logger} is the node's logger: ProtectedLogListener first, then the writers. */
    public DeclineReport(Logger logger, ISOPackager packager) {
        this.log = new Log(logger, "client.declines");
        this.packager = packager;
    }

    /** Records a rejected exchange and returns the dump for the host's support desk. */
    public String report(ISOMsg request, ISOMsg response) throws ISOException {
        logRejection(request, response);
        return ISOUtil.hexdump(shareableImage(request));
    }

    /** The entry in the node's log. */
    void logRejection(ISOMsg request, ISOMsg response) {
        LogEvent ev = log.createWarn();
        ev.addMessage(request);
        ev.addMessage(response);
        Logger.log(ev);
    }

    /** The bytes the support desk gets. */
    byte[] shareableImage(ISOMsg request) throws ISOException {
        for (int f : new int[] { 2, 35, 45 })
            if (request.hasField(f))
                request.set(f, ISOUtil.protect(request.getString(f)));
        for (int f : new int[] { 52, 55 })
            if (request.hasField(f))
                request.set(f, new byte[request.getBytes(f).length]);
        request.setPackager(packager);
        return request.pack();
    }
}

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
        log.warn(new Rejected(request, response));
    }

    /** Keeps the two messages together in the log. */
    record Rejected(ISOMsg request, ISOMsg response) implements org.jpos.util.Loggeable {
        @Override
        public void dump(java.io.PrintStream p, String indent) {
            p.println(indent + "<rejected>");
            request.dump(p, indent + "  ");
            response.dump(p, indent + "  ");
            p.println(indent + "</rejected>");
        }
    }

    /** The bytes the support desk gets. */
    byte[] shareableImage(ISOMsg request) throws ISOException {
        ISOMsg copy = (ISOMsg) request.clone();
        for (int f : new int[] { 2, 35, 45 })
            if (copy.hasField(f))
                copy.set(f, ISOUtil.protect(copy.getString(f)));
        for (int f : new int[] { 52, 55 })
            if (copy.hasField(f))
                copy.set(f, new byte[copy.getBytes(f).length]);
        copy.setPackager(packager);
        return copy.pack();
    }
}

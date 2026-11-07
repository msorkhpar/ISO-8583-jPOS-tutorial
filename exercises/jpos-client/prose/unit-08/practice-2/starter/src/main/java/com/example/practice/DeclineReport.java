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
        // TODO: One entry for the rejected exchange, with both messages field by field, so the
        //       team can compare them with the host's trace. The log is shipped to a platform
        //       that many people can search.
        throw new IllegalStateException("TODO: logRejection");
    }

    /** The bytes the support desk gets. */
    byte[] shareableImage(ISOMsg request) throws ISOException {
        // TODO: The support desk works for another company and gets this by e-mail, so no card
        //       data in it. They are looking for a format error, so it must still be the message
        //       as their host received it. The request is the one the client keeps for its own
        //       records (a reversal, say), and must come out of this as it went in.
        throw new IllegalStateException("TODO: shareableImage");
    }
}

package com.example.practice;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.util.LogEvent;
import org.jpos.util.LogSource;
import org.jpos.util.Logger;
import org.jpos.util.SimpleLogSource;

/** A transaction log that never holds a full card number. */
public final class TransactionLogger {

    private final LogSource source;

    public TransactionLogger(Logger logger) {
        this.source = new SimpleLogSource(logger, "client");
    }

    /** Log one `Transaction` event for `msg`. */
    public void logTransaction(ISOMsg msg) throws ISOException {
        LogEvent event = new LogEvent(source, "Transaction");
        event.addMessage("MTI: " + msg.getMTI());
        event.addMessage("PAN: " + (msg.hasField(2) ? masked(msg.getString(2)) : "none"));
        event.addMessage("Amount: " + msg.getString(4));
        Logger.log(event);
    }

    private static String masked(String pan) {
        return pan.substring(0, 6) + "*".repeat(pan.length() - 10) + pan.substring(pan.length() - 4);
    }
}

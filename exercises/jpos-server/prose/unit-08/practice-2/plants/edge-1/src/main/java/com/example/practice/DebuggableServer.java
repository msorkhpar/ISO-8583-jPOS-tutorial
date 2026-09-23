package com.example.practice;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.util.LogEvent;
import org.jpos.util.LogSource;
import org.jpos.util.Logger;
import org.jpos.util.SimpleLogSource;

/** A server whose debug output can be switched on and off. */
public final class DebuggableServer {

    private final LogSource source;
    private boolean debug;

    public DebuggableServer(Logger logger) {
        this.source = new SimpleLogSource(logger, "server");
    }

    /** Switch debug mode on or off. */
    public void setDebugMode(boolean on) {
        this.debug = this.debug || on;
    }

    /** Log `msg` as processed, after a debug event while debug mode is on. */
    public void processTransaction(ISOMsg msg) throws ISOException {
        if (debug) {
            LogEvent details = new LogEvent(source, "transaction.debug");
            details.addMessage("MTI: " + msg.getMTI());
            details.addMessage("Amount: " + (msg.hasField(4) ? msg.getString(4) : "none"));
            Logger.log(details);
        }
        LogEvent event = new LogEvent(source, "transaction.process");
        event.addMessage("Processing transaction: " + msg.getMTI());
        Logger.log(event);
    }
}

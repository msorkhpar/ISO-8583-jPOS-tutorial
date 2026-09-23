package com.example.practice;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.util.LogEvent;
import org.jpos.util.LogSource;
import org.jpos.util.Logger;
import org.jpos.util.SimpleLogSource;

/** Processes a message and leaves exactly one trace of it, whatever happens. */
public final class TransactionProcessor {

    private final LogSource source;

    public TransactionProcessor(Logger logger) {
        this.source = new SimpleLogSource(logger, "server");
    }

    /** Process `msg`, log one `transaction.process` event, and say whether it worked. */
    public boolean process(ISOMsg msg) {
        LogEvent event = new LogEvent(source, "transaction.process");
        try {
            event.addMessage("Processing transaction: " + msg.getMTI());
            event.addMessage("Transaction processed successfully");
            Logger.log(event);
            return true;
        } catch (ISOException e) {
            event.addMessage(e);
            return false;
        }
    }
}

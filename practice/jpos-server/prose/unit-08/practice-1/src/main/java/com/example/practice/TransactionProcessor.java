package com.example.practice;

import org.jpos.iso.ISOMsg;
import org.jpos.util.Logger;

/** Processes a message and leaves exactly one trace of it, whatever happens. */
public final class TransactionProcessor {

    public TransactionProcessor(Logger logger) {
    }

    /** Process `msg`, log one `transaction.process` event, and say whether it worked. */
    public boolean process(ISOMsg msg) {
        throw new UnsupportedOperationException("write process");
    }
}

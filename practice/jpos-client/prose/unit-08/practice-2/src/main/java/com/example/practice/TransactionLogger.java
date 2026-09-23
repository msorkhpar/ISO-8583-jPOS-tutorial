package com.example.practice;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.util.Logger;

/** A transaction log that never holds a full card number. */
public final class TransactionLogger {

    public TransactionLogger(Logger logger) {
    }

    /** Log one `Transaction` event for `msg`. */
    public void logTransaction(ISOMsg msg) throws ISOException {
        throw new UnsupportedOperationException("write logTransaction");
    }
}

package com.example.practice;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.util.Logger;

/** A server whose debug output can be switched on and off. */
public final class DebuggableServer {

    public DebuggableServer(Logger logger) {
    }

    /** Switch debug mode on or off. */
    public void setDebugMode(boolean on) {
        throw new UnsupportedOperationException("write setDebugMode");
    }

    /** Log `msg` as processed, after a debug event while debug mode is on. */
    public void processTransaction(ISOMsg msg) throws ISOException {
        throw new UnsupportedOperationException("write processTransaction");
    }
}

package com.example.practice;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOResponseListener;
import org.jpos.iso.MUX;

/** A host that answers in-process, so a client can be tested without one. */
public final class SimulatedHost implements MUX {

    public SimulatedHost(long limitInMinorUnits) {
    }

    @Override
    public ISOMsg request(ISOMsg request, long timeout) throws ISOException {
        throw new UnsupportedOperationException("write request");
    }

    @Override
    public void request(ISOMsg request, long timeout, ISOResponseListener listener, Object handBack)
            throws ISOException {
        throw new UnsupportedOperationException("write request");
    }

    @Override
    public void send(ISOMsg message) {
        throw new UnsupportedOperationException("write send");
    }

    @Override
    public boolean isConnected() {
        throw new UnsupportedOperationException("write isConnected");
    }
}

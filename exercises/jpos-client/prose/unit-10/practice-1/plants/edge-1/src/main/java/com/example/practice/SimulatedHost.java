package com.example.practice;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOResponseListener;
import org.jpos.iso.MUX;

/** A host that answers in-process, so a client can be tested without one. */
public final class SimulatedHost implements MUX {

    private final long limit;

    public SimulatedHost(long limitInMinorUnits) {
        this.limit = limitInMinorUnits;
    }

    @Override
    public ISOMsg request(ISOMsg request, long timeout) throws ISOException {
        ISOMsg reply = request;
        reply.setResponseMTI();
        boolean over = request.hasField(4) && Long.parseLong(request.getString(4)) > limit;
        reply.set(39, over ? "51" : "00");
        return reply;
    }

    @Override
    public void request(ISOMsg request, long timeout, ISOResponseListener listener, Object handBack)
            throws ISOException {
        listener.responseReceived(request(request, timeout), handBack);
    }

    @Override
    public void send(ISOMsg message) {
    }

    @Override
    public boolean isConnected() {
        return true;
    }
}

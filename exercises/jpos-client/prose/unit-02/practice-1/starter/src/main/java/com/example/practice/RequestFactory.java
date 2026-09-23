package com.example.practice;

import java.time.Clock;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;

/** Builds client requests on one set of common fields. */
public final class RequestFactory {

    public RequestFactory(String terminalId, Clock clock, int lastTraceNumber) {
    }

    /** A request of type `mti` with the fields every request shares. */
    public ISOMsg create(String mti) throws ISOException {
        throw new UnsupportedOperationException("write create");
    }

    public ISOMsg authorization(String pan, long amountMinor) throws ISOException {
        throw new UnsupportedOperationException("write authorization");
    }

    public ISOMsg financial(String pan, long amountMinor) throws ISOException {
        throw new UnsupportedOperationException("write financial");
    }

    public ISOMsg reversal(ISOMsg original) throws ISOException {
        throw new UnsupportedOperationException("write reversal");
    }
}

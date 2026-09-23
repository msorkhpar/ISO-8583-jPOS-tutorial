package com.example.practice;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;

/** The answering half of a host simulator: it stands in for the card network. */
public final class HostSimulator {

    public HostSimulator(String approvalCode) {
        throw new UnsupportedOperationException("write HostSimulator");
    }

    /** Sets up one test card to be declined with the given response code. */
    public void declineCard(String pan, String responseCode) {
        throw new UnsupportedOperationException("write declineCard");
    }

    /** The network's answer to the request. */
    public ISOMsg respond(ISOMsg request) throws ISOException {
        throw new UnsupportedOperationException("write respond");
    }
}

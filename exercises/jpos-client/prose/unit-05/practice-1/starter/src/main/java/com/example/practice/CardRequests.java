package com.example.practice;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;

/** Authorization requests for Visa and Mastercard, built from one set of rules. */
public final class CardRequests {

    public ISOMsg purchase(String network, String pan, String amount, String terminalId)
            throws ISOException {
        throw new UnsupportedOperationException("write purchase");
    }

    public ISOMsg cashAdvance(String network, String pan, String amount, String terminalId)
            throws ISOException {
        throw new UnsupportedOperationException("write cashAdvance");
    }

    public ISOMsg refund(String network, String pan, String amount, String terminalId,
            String originalRrn) throws ISOException {
        throw new UnsupportedOperationException("write refund");
    }

    public ISOMsg balanceInquiry(String network, String pan, String terminalId)
            throws ISOException {
        throw new UnsupportedOperationException("write balanceInquiry");
    }
}

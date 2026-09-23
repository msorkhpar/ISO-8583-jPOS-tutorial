package com.example.practice;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;

/** Authorization requests for Visa and Mastercard, built from one set of rules. */
public final class CardRequests {

    private enum Network {
        VISA("123456", false), MASTERCARD("654321", true);

        final String acquirer;
        final boolean posData;

        Network(String acquirer, boolean posData) {
            this.acquirer = acquirer;
            this.posData = posData;
        }

        static Network named(String name) {
            for (Network n : values()) {
                if (n.name().equalsIgnoreCase(name)) {
                    return n;
                }
            }
            return VISA;
        }
    }

    public ISOMsg purchase(String network, String pan, String amount, String terminalId)
            throws ISOException {
        return request(network, "000000", 1, pan, amount, terminalId, null);
    }

    public ISOMsg cashAdvance(String network, String pan, String amount, String terminalId)
            throws ISOException {
        return request(network, "010000", 2, pan, amount, terminalId, null);
    }

    public ISOMsg refund(String network, String pan, String amount, String terminalId,
            String originalRrn) throws ISOException {
        return request(network, "200000", 3, pan, amount, terminalId, originalRrn);
    }

    public ISOMsg balanceInquiry(String network, String pan, String terminalId)
            throws ISOException {
        return request(network, "300000", 4, pan, null, terminalId, null);
    }

    private static ISOMsg request(String name, String processingCode, int pos, String pan,
            String amount, String terminalId, String originalRrn) throws ISOException {
        Network network = Network.named(name);
        ISOMsg msg = new ISOMsg();
        msg.setMTI("0100");
        msg.set(2, pan);
        msg.set(3, processingCode);
        if (amount != null) {
            msg.set(4, amount);
        }
        msg.set(32, network.acquirer);
        if (originalRrn != null) {
            msg.set(37, originalRrn);
        }
        msg.set(41, terminalId);
        msg.set(49, "840");
        if (network.posData) {
            msg.set(61, String.format("%016d", pos));
        }
        return msg;
    }
}

package com.example.practice;

import java.util.HashMap;
import java.util.Map;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;

/** The answering half of a host simulator: it stands in for the card network. */
public final class HostSimulator {

    private final String approvalCode;
    private final Map<String, String> declined = new HashMap<>();

    public HostSimulator(String approvalCode) {
        this.approvalCode = approvalCode;
    }

    /** Sets up one test card to be declined with the given response code. */
    public void declineCard(String pan, String responseCode) {
        declined.put(pan, responseCode);
    }

    /** The network's answer to the request. */
    public ISOMsg respond(ISOMsg request) throws ISOException {
        ISOMsg response = (ISOMsg) request.clone();
        response.setResponseMTI();
        String code = declined.get(request.getString(2));
        if (code == null) {
            response.set(39, "00");
            response.set(38, approvalCode);
        } else {
            response.set(39, code);
            response.unset(38);
        }
        return response;
    }
}

package com.example.practice;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;

/** Builds the response to a request, leaving the request alone. */
public final class ResponseMaker {

    public ISOMsg respond(ISOMsg request, String responseCode) throws ISOException {
        ISOMsg response = (ISOMsg) request.clone();
        response.setResponseMTI();
        response.set(39, responseCode);
        response.unset(55);
        return response;
    }
}

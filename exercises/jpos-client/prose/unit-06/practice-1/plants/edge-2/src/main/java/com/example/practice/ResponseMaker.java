package com.example.practice;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;

/** Builds the response to a request, leaving the request alone. */
public final class ResponseMaker {

    public ISOMsg respond(ISOMsg request, String responseCode) throws ISOException {
        request.setResponseMTI();
        request.set(39, responseCode);
        request.unset(55);
        return request;
    }
}

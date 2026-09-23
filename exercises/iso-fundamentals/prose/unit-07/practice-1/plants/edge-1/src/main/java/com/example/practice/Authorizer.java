package com.example.practice;

import java.io.IOException;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.MUX;

/** The acquirer's side of an authorization: send through the MUX, read the issuer's answer. */
public final class Authorizer {

    /** How long to wait for the issuer's answer. */
    public static final long TIMEOUT_MILLIS = 30_000L;

    /** The issuer's answer. */
    public record Decision(boolean approved, String responseCode) {
    }

    private Authorizer() {
    }

    /** Send the request and report the issuer's decision. */
    public static Decision authorize(MUX mux, ISOMsg request) throws ISOException, IOException {
        ISOMsg response = mux.request(request, TIMEOUT_MILLIS);
        if (response == null) {
            return new Decision(false, null);
        }
        String code = response.getString(39);
        return new Decision("00".equals(code), code);
    }
}

package com.example.practice;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.security.SMAdapter;
import org.jpos.security.SMException;

/** Replaces a clear PIN by the PIN block a security module makes of it, in field 52. */
public class PinEncryptionService {

    public PinEncryptionService(SMAdapter<?> securityModule) {
    }

    /** Ask the module for the PIN block of `clearPin` with the card in field 2, and put it in field 52. */
    public ISOMsg encryptPIN(ISOMsg msg, String clearPin) throws ISOException, SMException {
        throw new UnsupportedOperationException("write encryptPIN");
    }
}

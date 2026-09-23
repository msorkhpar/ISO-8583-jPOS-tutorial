package com.example.practice;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;
import org.jpos.security.EncryptedPIN;
import org.jpos.security.SMAdapter;
import org.jpos.security.SMException;

/** Replaces a clear PIN by the PIN block a security module makes of it, in field 52. */
public class PinEncryptionService {

    private final SMAdapter<?> securityModule;

    public PinEncryptionService(SMAdapter<?> securityModule) {
        this.securityModule = securityModule;
    }

    /** Ask the module for the PIN block of `clearPin` with the card in field 2, and put it in field 52. */
    public ISOMsg encryptPIN(ISOMsg msg, String clearPin) throws ISOException, SMException {
        String pan = msg.getString(2);
        if (pan == null) {
            throw new IllegalArgumentException("no card number (field 2): cannot make a PIN block");
        }
        EncryptedPIN pin = securityModule.encryptPIN(clearPin, pan);
        msg.set(52, ISOUtil.hexString(pin.getPINBlock()));
        return msg;
    }
}

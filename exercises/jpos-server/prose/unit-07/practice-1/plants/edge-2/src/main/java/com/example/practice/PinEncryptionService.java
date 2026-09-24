package com.example.practice;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.security.EncryptedPIN;
import org.jpos.security.SMAdapter;
import org.jpos.security.SMException;
import org.jpos.security.SecureDESKey;

/** Replaces a clear PIN by the PIN block for the next hop, under the zone PIN key, in field 52. */
public class PinEncryptionService {

    private final SMAdapter<SecureDESKey> securityModule;
    private final SecureDESKey zpk;
    private final byte pinBlockFormat;

    public PinEncryptionService(SMAdapter<SecureDESKey> securityModule, SecureDESKey zpk, byte pinBlockFormat) {
        this.securityModule = securityModule;
        this.zpk = zpk;
        this.pinBlockFormat = pinBlockFormat;
    }

    /** Encrypt `clearPin` with the card in field 2, export it under the ZPK, and put the block in field 52. */
    public ISOMsg encryptPIN(ISOMsg msg, String clearPin) throws ISOException, SMException {
        String pan = msg.getString(2);
        EncryptedPIN pinUnderLmk = securityModule.encryptPIN(clearPin, pan);
        EncryptedPIN pinUnderZpk = securityModule.exportPIN(pinUnderLmk, zpk, pinBlockFormat);
        msg.set(52, pinUnderZpk.getPINBlock());
        return msg;
    }
}

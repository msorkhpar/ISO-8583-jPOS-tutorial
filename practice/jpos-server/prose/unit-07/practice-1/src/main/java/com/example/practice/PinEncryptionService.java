package com.example.practice;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.security.SMAdapter;
import org.jpos.security.SMException;
import org.jpos.security.SecureDESKey;

/** Replaces a clear PIN by the PIN block for the next hop, under the zone PIN key, in field 52. */
public class PinEncryptionService {

    public PinEncryptionService(SMAdapter<SecureDESKey> securityModule, SecureDESKey zpk, byte pinBlockFormat) {
    }

    /** Encrypt `clearPin` with the card in field 2, export it under the ZPK, and put the block in field 52. */
    public ISOMsg encryptPIN(ISOMsg msg, String clearPin) throws ISOException, SMException {
        throw new UnsupportedOperationException("write encryptPIN");
    }
}

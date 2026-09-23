package com.example.practice;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;

/** A client that encrypts the PIN, signs what it sends and checks what comes back. */
public final class SecureClient {

    /** The security module. */
    public interface Security {
        String encryptPin(String pin, String pan) throws ISOException;

        byte[] mac(ISOMsg msg) throws ISOException;
    }

    /** The channel to the host. */
    public interface Link {
        ISOMsg exchange(ISOMsg request) throws ISOException;
    }

    public SecureClient(Security security, Link link) {
    }

    public ISOMsg send(ISOMsg msg) throws ISOException {
        throw new UnsupportedOperationException("write send");
    }
}

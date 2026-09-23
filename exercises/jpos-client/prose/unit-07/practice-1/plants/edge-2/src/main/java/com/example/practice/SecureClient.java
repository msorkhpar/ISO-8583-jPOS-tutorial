package com.example.practice;

import java.util.Arrays;
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

    private final Security security;
    private final Link link;

    public SecureClient(Security security, Link link) {
        this.security = security;
        this.link = link;
    }

    public ISOMsg send(ISOMsg msg) throws ISOException {
        msg.set(52, security.encryptPin(msg.getString(52), msg.getString(2)));
        msg.unset(64);
        msg.set(64, security.mac(msg));

        ISOMsg response = link.exchange(msg);
        byte[] received = response.hasField(64) ? response.getBytes(64) : null;
        ISOMsg unsigned = (ISOMsg) response.clone();
        unsigned.unset(64);
        if (received != null && !Arrays.equals(security.mac(unsigned), received)) {
            throw new SecurityException("Invalid MAC in response");
        }
        return response;
    }
}

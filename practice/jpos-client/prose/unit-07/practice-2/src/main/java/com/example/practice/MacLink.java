package com.example.practice;

import java.security.MessageDigest;
import java.util.Arrays;

import org.jpos.iso.ISOBasePackager;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.MUX;
import org.jpos.security.SMAdapter;
import org.jpos.security.SMException;
import org.jpos.security.SecureDESKey;

/**
 * The client's link to a host that MACs every message, both ways. The host's spec: the MAC covers
 * the packed message up to the MAC field, with the MAC field's bit set; the MAC goes in field 64,
 * or in field 128 when the message has any field above 64.
 */
public class MacLink {
    public enum Outcome { APPROVED, DECLINED, UNKNOWN }

    private final MUX mux;
    private final SMAdapter<SecureDESKey> hsm;
    private final SecureDESKey macKey;

    public MacLink(MUX mux, SMAdapter<SecureDESKey> hsm, SecureDESKey macKey) {
        this.mux = mux;
        this.hsm = hsm;
        this.macKey = macKey;
    }

    /** Signs a copy of the request, sends it and says what became of it. */
    public Outcome send(ISOMsg request, long timeout) throws ISOException, SMException {
        ISOMsg out = (ISOMsg) request.clone();
        sign(out);
        return outcomeOf(mux.request(out, timeout));
    }

    /** The field that carries this message's MAC. */
    static int macField(ISOMsg m) {
        // TODO: 64 or 128, as the host's spec (in the class comment) says for this message.
        throw new IllegalStateException("TODO: macField");
    }

    /** The bytes the MAC of this message covers. */
    static byte[] macInput(ISOMsg m) throws ISOException {
        // TODO: Exactly what the host's spec says the MAC covers. The same method serves a request
        //       about to be signed and a response that arrived with its MAC, so leave `m` as it is.
        throw new IllegalStateException("TODO: macInput");
    }

    /** Adds the MAC to a message that is otherwise ready to go. */
    void sign(ISOMsg m) throws ISOException, SMException {
        // TODO: One line. The module computes MACs with generateCBC_MAC(data, macKey).
        throw new IllegalStateException("TODO: sign");
    }

    /** What the client may conclude from the host's response, or from no response (null). */
    Outcome outcomeOf(ISOMsg response) throws ISOException, SMException {
        // TODO: APPROVED or DECLINED only when the client can rely on what the response says;
        //       UNKNOWN whenever the purchase may or may not have gone through. Field 39 "00" is
        //       an approval.
        throw new IllegalStateException("TODO: outcomeOf");
    }
}

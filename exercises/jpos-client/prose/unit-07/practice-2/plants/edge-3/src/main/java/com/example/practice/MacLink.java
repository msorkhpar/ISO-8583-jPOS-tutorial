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
        for (int f = 65; f < 128; f++)
            if (m.hasField(f))
                return 128;
        return 64;
    }

    /** The bytes the MAC of this message covers. */
    static byte[] macInput(ISOMsg m) throws ISOException {
        int field = macField(m);
        ISOMsg copy = (ISOMsg) m.clone();
        copy.set(field, new byte[8]);
        byte[] packed = copy.pack();
        int macLength = ((ISOBasePackager) copy.getPackager()).getFieldPackager(field).getMaxPackedLength();
        return Arrays.copyOf(packed, packed.length - macLength);
    }

    /** Adds the MAC to a message that is otherwise ready to go. */
    void sign(ISOMsg m) throws ISOException, SMException {
        m.set(macField(m), hsm.generateCBC_MAC(macInput(m), macKey));
    }

    /** What the client may conclude from the host's response, or from no response (null). */
    Outcome outcomeOf(ISOMsg response) throws ISOException, SMException {
        if (response == null)
            return Outcome.UNKNOWN;
        byte[] received = response.getBytes(macField(response));
        if (received != null && !MessageDigest.isEqual(received, hsm.generateCBC_MAC(macInput(response), macKey)))
            return Outcome.UNKNOWN;
        return "00".equals(response.getString(39)) ? Outcome.APPROVED : Outcome.DECLINED;
    }
}

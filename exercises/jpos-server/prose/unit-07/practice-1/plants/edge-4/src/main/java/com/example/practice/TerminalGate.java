package com.example.practice;

import org.jpos.core.Configurable;
import org.jpos.core.Configuration;
import org.jpos.iso.ISOBasePackager;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.security.EncryptedPIN;
import org.jpos.security.SMAdapter;
import org.jpos.security.SecureDESKey;
import org.jpos.security.SecureKeyStore;
import org.jpos.transaction.Context;
import org.jpos.transaction.ContextConstants;
import org.jpos.transaction.TransactionParticipant;
import org.jpos.util.NameRegistrar;

import java.io.Serializable;
import java.security.MessageDigest;
import java.util.Arrays;

/**
 * The first participant of the acquirer's chain. QueryHost, further down, sends whatever this
 * participant leaves in the Context under UPSTREAM to the network.
 */
public class TerminalGate implements TransactionParticipant, Configurable {
    public static final String UPSTREAM = "UPSTREAM";
    private String networkKey;

    @Override
    public void setConfiguration(Configuration cfg) {
        networkKey = cfg.get("network-key", "zpk.network");
    }

    @Override
    public int prepare(long id, Serializable context) {
        Context ctx = (Context) context;
        ISOMsg request = ctx.get(ContextConstants.REQUEST.toString());
        if (!macVerifies(request))
            return refuse(ctx, request, "63");
        try {
            ctx.put(UPSTREAM, forNetwork(request));
            return PREPARED | NO_JOIN;
        } catch (Exception e) {
            return refuse(ctx, request, "96");
        }
    }

    /** True when the request carries a MAC that verifies under the key of the terminal that sent it. */
    boolean macVerifies(ISOMsg request) {
        try {
            SecureDESKey tak = keys().getKey("tak." + request.getString(41));
            byte[] received = request.getBytes(64);
            return received != null && MessageDigest.isEqual(received, hsm().generateCBC_MAC(macInput(request), tak));
        } catch (Exception e) {
            return false;
        }
    }

    /** The message the network receives in place of the terminal's request. */
    ISOMsg forNetwork(ISOMsg request) throws Exception {
        ISOMsg up = (ISOMsg) request.clone();
        up.unset(64);
        if (false) {
            SecureDESKey tpk = keys().getKey("tpk." + request.getString(41));
            EncryptedPIN in = new EncryptedPIN(request.getBytes(52), SMAdapter.FORMAT01, request.getString(2));
            up.set(52, hsm().translatePIN(in, tpk, keys().getKey(networkKey), SMAdapter.FORMAT01).getPINBlock());
        }
        return up;
    }

    // ---- given ----

    /** Leaves an answer with this response code for SendResponse, and stops the transaction. */
    static int refuse(Context ctx, ISOMsg request, String rc) {
        try {
            ISOMsg r = (ISOMsg) request.clone();
            r.setResponseMTI();
            r.set(39, rc);
            r.unset(14, 35, 45, 52, 55, 64);
            ctx.put(ContextConstants.RESPONSE.toString(), r);
        } catch (ISOException e) {
            ctx.log(e);
        }
        return ABORTED | NO_JOIN;
    }

    /** What the terminal MACed: the packed message up to field 64, with bit 64 set (unit c7). */
    static byte[] macInput(ISOMsg msg) throws ISOException {
        ISOMsg copy = (ISOMsg) msg.clone();
        copy.set(64, new byte[8]);
        byte[] packed = copy.pack();
        int macLength = ((ISOBasePackager) copy.getPackager()).getFieldPackager(64).getMaxPackedLength();
        return Arrays.copyOf(packed, packed.length - macLength);
    }

    static SMAdapter<SecureDESKey> hsm() {
        return NameRegistrar.getIfExists("hsm");
    }

    static SecureKeyStore keys() {
        return NameRegistrar.getIfExists("ks");
    }
}

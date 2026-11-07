package com.example.practice;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.packager.GenericPackager;
import org.jpos.security.SMAdapter;
import org.jpos.security.SecureDESKey;
import org.jpos.security.SecureKeyStore;
import org.jpos.transaction.AbortParticipant;
import org.jpos.transaction.Context;
import org.jpos.transaction.ContextConstants;
import org.jpos.transaction.TransactionParticipant;
import org.jpos.util.NameRegistrar;

import java.io.Serializable;
import java.security.MessageDigest;
import java.util.Arrays;

/** The two participants that handle MACs on the way back: one after QueryHost, one right before SendResponse. */
public final class NetworkAnswers {
    private NetworkAnswers() { }

    /** Right after QueryHost. The network signs its answers with the key it shares with us, "zak.network". */
    public static class CheckNetworkMac implements TransactionParticipant {
        @Override
        public int prepare(long id, Serializable context) {
            Context ctx = (Context) context;
            ISOMsg answer = ctx.get(ContextConstants.RESPONSE.toString());
            if (answer == null)
                return ABORTED | NO_JOIN | READONLY;          // QueryHost got nothing: the chain answers 91
            try {
                byte[] received = answer.getBytes(64);
                if (received != null && MessageDigest.isEqual(received, mac(answer, key("zak.network"))))
                    return PREPARED | NO_JOIN | READONLY;
            } catch (Exception e) {
                ctx.log(e.getClass().getSimpleName());
            }
            ctx.remove(ContextConstants.RESPONSE.toString());
            return ABORTED | NO_JOIN;
        }
    }

    /** Right before SendResponse: the terminal checks every answer under its own key, "tak." + its terminal ID. */
    public static class SignForTerminal implements TransactionParticipant {
        @Override
        public int prepare(long id, Serializable context) {
            sign((Context) context);
            return PREPARED | NO_JOIN;
        }

        void sign(Context ctx) {
            ISOMsg request = ctx.get(ContextConstants.REQUEST.toString());
            ISOMsg answer = ctx.get(ContextConstants.RESPONSE.toString());
            if (answer == null)
                return;
            try {
                answer.set(64, mac(answer, key("tak." + request.getString(41))));
            } catch (Exception e) {
                ctx.log(e.getClass().getSimpleName());
            }
        }
    }

    // ---- given ----

    /** The MAC of m as both links pack it: the packed message up to field 64, with bit 64 set (unit c7). */
    static byte[] mac(ISOMsg m, SecureDESKey key) throws Exception {
        ISOMsg copy = (ISOMsg) m.clone();
        copy.setPackager(new GenericPackager("jar:packager/iso87ascii.xml"));
        copy.set(64, new byte[8]);
        byte[] packed = copy.pack();
        byte[] input = Arrays.copyOf(packed, packed.length - 16);      // field 64 travels as 16 hex characters
        SMAdapter<SecureDESKey> hsm = NameRegistrar.get("hsm");
        return hsm.generateCBC_MAC(input, key);
    }

    static SecureDESKey key(String alias) throws Exception {
        SecureKeyStore keys = NameRegistrar.get("ks");
        return keys.getKey(alias);
    }
}

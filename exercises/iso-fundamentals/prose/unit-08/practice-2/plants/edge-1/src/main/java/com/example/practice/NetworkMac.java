package com.example.practice;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * MACs on the regional debit network's link. Its specification, section 6.2:
 *
 *   The MAC is ISO 9797-1 MAC algorithm 3 under the double-length ZAK, over the MAC input with
 *   padding method 2: append one byte 0x80, then as few zero bytes (possibly none) as make the
 *   length a multiple of 8. All 8 bytes of the MAC are sent, and a message whose MAC does not
 *   verify is refused.
 */
public class NetworkMac {

    /** The 8-byte MAC of {@code data} under {@code key16}. */
    public static byte[] mac(byte[] key16, byte[] data) throws GeneralSecurityException {
        if (key16.length != 16)
            throw new IllegalArgumentException("a double-length (16-byte) key is required");
        SecretKeySpec left = new SecretKeySpec(Arrays.copyOfRange(key16, 0, 8), "DES");
        SecretKeySpec right = new SecretKeySpec(Arrays.copyOfRange(key16, 8, 16), "DES");
        byte[] padded = pad(data);
        if (padded.length == 0 || padded.length % 8 != 0)
            throw new IllegalStateException("padding must give whole 8-byte blocks");

        Cipher des = Cipher.getInstance("DES/ECB/NoPadding");
        des.init(Cipher.ENCRYPT_MODE, left);
        byte[] chain = new byte[8];
        for (int off = 0; off < padded.length; off += 8) {
            for (int i = 0; i < 8; i++)
                chain[i] ^= padded[off + i];
            chain = des.doFinal(chain);
        }
        des.init(Cipher.DECRYPT_MODE, right);
        chain = des.doFinal(chain);
        des.init(Cipher.ENCRYPT_MODE, left);
        return des.doFinal(chain);
    }

    /** The MAC input as the network pads it. */
    static byte[] pad(byte[] data) {
        if (data.length % 8 == 0)
            return data.clone();                        // already whole blocks
        byte[] padded = Arrays.copyOf(data, (data.length / 8 + 1) * 8);
        padded[data.length] = (byte) 0x80;
        return padded;
    }

    /** Whether {@code received} is the MAC the sender computed over {@code data}. */
    public static boolean verify(byte[] key16, byte[] data, byte[] received) throws GeneralSecurityException {
        if (received == null)
            return false;
        return MessageDigest.isEqual(mac(key16, data), received);
    }
}

package com.example.practice;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import org.jpos.iso.ISOUtil;

/** Keeps card numbers encrypted under data keys that are rotated, and finds them by a keyed value. */
public final class PanVault {

    /** What is stored: the id of the key that opens it, then the IV and the ciphertext. */
    public record Sealed(String keyId, byte[] ivAndCiphertext) { }

    private static final int IV_BYTES = 12;
    private final Map<String, SecretKey> dataKeys = new HashMap<>();
    private final SecretKey lookupKey;
    private final SecureRandom random = new SecureRandom();
    private String current;

    public PanVault(String keyId, SecretKey dataKey, SecretKey lookupKey) {
        this.lookupKey = lookupKey;
        rotate(keyId, dataKey);
    }

    /** From now on, new records are sealed under this key. Older keys stay until retired. */
    public void rotate(String keyId, SecretKey dataKey) {
        dataKeys.put(keyId, dataKey);
        current = keyId;
    }

    /** Destroys a key that no record needs any more. */
    public void retire(String keyId) {
        if (keyId.equals(current))
            throw new IllegalArgumentException(keyId + " is the current key");
        dataKeys.remove(keyId);
    }

    public Sealed seal(String pan) throws GeneralSecurityException {
        byte[] iv = new byte[IV_BYTES];
        random.nextBytes(iv);
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.ENCRYPT_MODE, dataKey(current), new GCMParameterSpec(128, iv));
        byte[] ct = c.doFinal(pan.getBytes(StandardCharsets.US_ASCII));
        byte[] out = Arrays.copyOf(iv, IV_BYTES + ct.length);
        System.arraycopy(ct, 0, out, IV_BYTES, ct.length);
        return new Sealed(current, out);
    }

    /** The card number in a record, sealed under any key this vault still holds. */
    public String open(Sealed s) throws GeneralSecurityException {
        byte[] in = s.ivAndCiphertext();
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.DECRYPT_MODE, dataKey(s.keyId()), new GCMParameterSpec(128, in, 0, IV_BYTES));
        return new String(c.doFinal(in, IV_BYTES, in.length - IV_BYTES), StandardCharsets.US_ASCII);
    }

    /** The same card number as a record the background job can store in place of the old one. */
    public Sealed rewrap(Sealed s) throws GeneralSecurityException {
        return seal(open(s));
    }

    /** The value support searches by: the same for a card every time, and useless without a key. */
    public String lookupValue(String pan) throws GeneralSecurityException {
        java.security.MessageDigest sha = java.security.MessageDigest.getInstance("SHA-256");
        return ISOUtil.hexString(sha.digest(pan.getBytes(StandardCharsets.US_ASCII)));
    }

    private SecretKey dataKey(String keyId) throws GeneralSecurityException {
        SecretKey k = dataKeys.get(keyId);
        if (k == null)
            throw new GeneralSecurityException("no key " + keyId);
        return k;
    }
}

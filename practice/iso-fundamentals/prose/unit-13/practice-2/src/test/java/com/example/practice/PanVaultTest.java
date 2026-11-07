package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;

class PanVaultTest {

    /** A test key: 32 bytes counting up from {@code first}. Real keys come from a keystore or an HSM. */
    private static SecretKey key(String algorithm, int first) {
        byte[] b = new byte[32];
        for (int i = 0; i < b.length; i++)
            b[i] = (byte) (first + i);
        return new SecretKeySpec(b, algorithm);
    }

    private static String pan(String digits) {
        return new String(digits.toCharArray());           // a new object each time
    }

    @Test
    void aCardSealedTodayOpens() throws Exception {
        PanVault vault = new PanVault("data-q1", key("AES", 3), key("HmacSHA256", 140));
        PanVault.Sealed s = vault.seal(pan("5555555555554444"));
        assertEquals("data-q1", s.keyId());
        assertFalse(new String(s.ivAndCiphertext(), StandardCharsets.ISO_8859_1).contains("5555555555554444"));
        assertEquals("5555555555554444", vault.open(s));
        assertEquals(vault.lookupValue(pan("4111111111111111")), vault.lookupValue(pan("4111111111111111")));
        assertNotEquals(vault.lookupValue(pan("4111111111111111")), vault.lookupValue(pan("5555555555554444")));
    }

    @Test
    void aCardSealedBeforeTheRotationStillOpens() throws Exception {
        PanVault vault = new PanVault("data-q1", key("AES", 3), key("HmacSHA256", 140));
        PanVault.Sealed old = vault.seal(pan("4111111111111111"));
        vault.rotate("data-q2", key("AES", 60));
        vault.rotate("data-q3", key("AES", 90));
        assertEquals("4111111111111111", vault.open(old));
        assertEquals("5555555555554444", vault.open(vault.seal(pan("5555555555554444"))));
    }

    @Test
    void aRewrappedCardOutlivesTheKeyItWasSealedUnder() throws Exception {
        PanVault vault = new PanVault("data-q1", key("AES", 3), key("HmacSHA256", 140));
        PanVault.Sealed old = vault.seal(pan("4111111111111111"));
        vault.rotate("data-q2", key("AES", 60));
        PanVault.Sealed moved = vault.rewrap(old);
        vault.retire("data-q1");
        assertEquals("data-q2", moved.keyId());
        assertEquals("4111111111111111", vault.open(moved));
    }

    @Test
    void theLookupValueSurvivesARotation() throws Exception {
        PanVault vault = new PanVault("data-q1", key("AES", 3), key("HmacSHA256", 140));
        String before = vault.lookupValue(pan("5555555555554444"));
        vault.rotate("data-q2", key("AES", 60));
        assertEquals(before, vault.lookupValue(pan("5555555555554444")),
                "support's index still finds the card after the data key changed");
    }

    @Test
    void theLookupValueTakesTheLookupKey() throws Exception {
        PanVault ours = new PanVault("data-q1", key("AES", 3), key("HmacSHA256", 140));
        PanVault copy = new PanVault("data-q1", key("AES", 3), key("HmacSHA256", 201));
        assertNotEquals(ours.lookupValue(pan("5555555555554444")), copy.lookupValue(pan("5555555555554444")),
                "without our lookup key, nobody computes our lookup values");
    }
}

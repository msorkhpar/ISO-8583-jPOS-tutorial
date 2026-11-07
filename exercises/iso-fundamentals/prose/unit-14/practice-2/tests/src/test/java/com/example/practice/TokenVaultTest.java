package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Random;
import java.util.Set;
import org.jpos.iso.ISOException;
import org.junit.jupiter.api.Test;

class TokenVaultTest {

    private static final String VISA = "4111111111111111";
    private static final String MASTERCARD = "5555555555554444";
    private static final String TOKEN_BIN = "98765432";
    private static final Set<String> STATION = Set.of("DETOKENIZE");
    private static final Set<String> SHOP_APP = Set.of("TOKENIZE", "REVOKE", "READ_DISPLAY_DATA");

    /** A stand-in for encryption and a keyed lookup. It is NOT encryption: tests only. */
    static final class TestSealer implements TokenVault.Sealer {
        @Override
        public String seal(String pan) {
            return "sealed:" + new StringBuilder(pan).reverse();
        }

        @Override
        public String open(String sealed) {
            return new StringBuilder(sealed.substring("sealed:".length())).reverse().toString();
        }

        @Override
        public String lookup(String pan) {
            return "lookup:" + Integer.toHexString(("k1" + pan).hashCode());
        }
    }

    private static TokenVault vault() {
        return new TokenVault(TOKEN_BIN, new Random(23), new TestSealer());
    }

    @Test
    void theStationGetsTheCardBehindAShopsToken() throws Exception {
        TokenVault vault = vault();
        String visa = vault.tokenize("pharmacy-4", VISA);
        String mastercard = vault.tokenize("pharmacy-4", MASTERCARD);

        assertEquals(VISA, vault.detokenize(STATION, "pharmacy-4", visa));
        assertEquals(MASTERCARD, vault.detokenize(STATION, "pharmacy-4", mastercard));
        assertEquals(VISA, vault.detokenize(Set.of("DETOKENIZE", "AUDIT"), "pharmacy-4", visa));
    }

    @Test
    void onlyACallerAllowedToDetokenizeGetsTheCard() throws Exception {
        TokenVault vault = vault();
        String token = vault.tokenize("pharmacy-4", VISA);

        assertThrows(SecurityException.class, () -> vault.detokenize(SHOP_APP, "pharmacy-4", token));
        assertThrows(SecurityException.class, () -> vault.detokenize(Set.of(), "pharmacy-4", token));
    }

    @Test
    void anotherShopsTokenIsRefused() throws Exception {
        TokenVault vault = vault();
        String pharmacys = vault.tokenize("pharmacy-4", MASTERCARD);
        vault.tokenize("cinema-9", MASTERCARD);

        assertThrows(ISOException.class, () -> vault.detokenize(STATION, "cinema-9", pharmacys));
        assertThrows(ISOException.class, () -> vault.detokenize(STATION, "bakery-1", pharmacys));
    }

    @Test
    void anUnknownTokenIsRefused() throws Exception {
        TokenVault vault = vault();
        vault.tokenize("pharmacy-4", VISA);

        assertThrows(ISOException.class, () -> vault.detokenize(STATION, "pharmacy-4", "9876543200000006"));
        assertThrows(ISOException.class, () -> vault.detokenize(STATION, "pharmacy-4", VISA));
    }

    @Test
    void aRevokedTokenLeadsNowhere() throws Exception {
        TokenVault vault = vault();
        String revoked = vault.tokenize("pharmacy-4", VISA);

        vault.revoke(revoked);

        assertThrows(ISOException.class, () -> vault.detokenize(STATION, "pharmacy-4", revoked));
    }

    @Test
    void aCardWhoseTokenWasRevokedGetsANewOne() throws Exception {
        TokenVault vault = vault();
        String revoked = vault.tokenize("pharmacy-4", MASTERCARD);
        String kept = vault.tokenize("cinema-9", MASTERCARD);

        vault.revoke(revoked);
        String fresh = vault.tokenize("pharmacy-4", MASTERCARD);

        assertNotEquals(revoked, fresh);
        assertEquals(MASTERCARD, vault.detokenize(STATION, "pharmacy-4", fresh));
        assertEquals(kept, vault.tokenize("cinema-9", MASTERCARD), "another requestor's token is not touched");
    }
}

package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.junit.jupiter.api.Test;

class PanVaultTest {

    private static ISOMsg carrying(String field2) throws Exception {
        ISOMsg message = new ISOMsg();
        message.setMTI("0200");
        message.set(2, field2);
        message.set(4, "000000010000");
        return message;
    }

    private static void assertShapeOf(String pan, String token) {
        assertEquals(pan.length(), token.length(), "same length as the card number");
        assertTrue(token.matches("[0-9]+"), "digits only: " + token);
        assertEquals(pan.substring(0, 6), token.substring(0, 6), "first six kept");
        assertEquals(pan.substring(pan.length() - 4), token.substring(token.length() - 4), "last four kept");
        assertNotEquals(pan, token, "a token is not the card number");
    }

    @Test
    void tokenKeepsTheShapeOfTheCardAndRevealsBack() throws Exception {
        PanVault vault = new PanVault();
        for (String pan : new String[] {"4111111111111111", "5413330089020011", "6011000990139424123"}) {
            ISOMsg message = carrying(pan);
            vault.protect(message);
            String token = message.getString(2);
            assertShapeOf(pan, token);
            assertEquals("000000010000", message.getString(4), "other fields untouched");

            ISOMsg later = carrying(token);
            vault.reveal(later);
            assertEquals(pan, later.getString(2));
        }
    }

    @Test
    void sameCardAlwaysGetsTheSameToken() throws Exception {
        PanVault vault = new PanVault();
        ISOMsg first = carrying("4111111111111111");
        ISOMsg second = carrying("4111111111111111");
        vault.protect(first);
        vault.protect(second);
        assertEquals(first.getString(2), second.getString(2));
    }

    @Test
    void refusesATokenTheVaultNeverIssued() throws Exception {
        PanVault vault = new PanVault();
        vault.protect(carrying("4111111111111111"));
        // Never protected: no token this vault issued can start with 541333.
        ISOMsg stranger = carrying("5413330000000011");
        assertThrows(ISOException.class, () -> vault.reveal(stranger));
        assertEquals("5413330000000011", stranger.getString(2), "field 2 left as it was");
    }
}

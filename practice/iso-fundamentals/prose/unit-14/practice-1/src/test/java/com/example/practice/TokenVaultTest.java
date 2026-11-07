package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TokenVaultTest {

    private static final String VISA = "4111111111111111";
    private static final String MASTERCARD = "5555555555554444";
    private static final String TOKEN_BIN = "98765432";
    private static final Set<String> STATION = Set.of("DETOKENIZE");

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

    /** Hands out the given digits, in order, to nextInt(10). */
    static final class ScriptedRandom extends Random {
        private final int[] digits;
        private int next;

        ScriptedRandom(String digits) {
            this.digits = digits.chars().map(c -> c - '0').toArray();
        }

        @Override
        public int nextInt(int bound) {
            return digits[next++];
        }
    }

    private static TokenVault vault() {
        return new TokenVault(TOKEN_BIN, new Random(11), new TestSealer());
    }

    private static boolean luhnValid(String number) {
        int sum = 0;
        for (int i = 0; i < number.length(); i++) {
            int d = number.charAt(number.length() - 1 - i) - '0';
            if (i % 2 == 1) {
                d *= 2;
                if (d > 9) {
                    d -= 9;
                }
            }
            sum += d;
        }
        return sum % 10 == 0;
    }

    /** The fifteen digits plus the check digit that makes them pass the Luhn check. */
    private static String withCheckDigit(String fifteen) {
        for (char c = '0'; c <= '9'; c++) {
            if (luhnValid(fifteen + c)) {
                return fifteen + c;
            }
        }
        throw new AssertionError();
    }

    @Test
    void aCardGetsATokenThatLeadsBackToIt() throws Exception {
        TokenVault vault = vault();

        String token = vault.tokenize("cafe-12", VISA);
        assertTrue(token.matches("\\d{16}"), token);
        assertNotEquals(VISA, token);
        assertEquals(token, vault.tokenize("cafe-12", new String(VISA.toCharArray())), "same requestor, same card");

        String other = vault.tokenize("cafe-12", MASTERCARD);
        assertNotEquals(token, other);
        assertEquals(VISA, vault.detokenize(STATION, "cafe-12", token));
        assertEquals(MASTERCARD, vault.detokenize(STATION, "cafe-12", other));
    }

    @Test
    void tokensPassTheCardNumberCheck() throws Exception {
        TokenVault vault = vault();
        for (int i = 0; i < 40; i++) {
            String token = vault.tokenize("kiosk-" + i, MASTERCARD);
            assertTrue(luhnValid(token), "a checkout's card-number check refuses " + token);
        }
    }

    @Test
    void tokensComeFromTheTokenRange() throws Exception {
        TokenVault vault = vault();
        for (int i = 0; i < 40; i++) {
            String token = vault.tokenize("kiosk-" + i, VISA);
            assertTrue(token.startsWith(TOKEN_BIN), token + " could be routed to a card issuer");
        }
    }

    @Test
    void eachRequestorGetsItsOwnToken() throws Exception {
        TokenVault vault = vault();

        String forCafe = vault.tokenize("cafe-12", MASTERCARD);
        String forGarage = vault.tokenize("garage-3", MASTERCARD);

        assertNotEquals(forCafe, forGarage, "a token leaked from one merchant must be useless at another");
        assertEquals(forGarage, vault.tokenize("garage-3", MASTERCARD));
        assertEquals(MASTERCARD, vault.detokenize(STATION, "garage-3", forGarage));
    }

    @Test
    void aTokenSaysNothingAboutTheCard() throws Exception {
        // Two vaults that draw the same digits, one for each card.
        TokenVault one = new TokenVault(TOKEN_BIN, new ScriptedRandom("3094817"), new TestSealer());
        TokenVault other = new TokenVault(TOKEN_BIN, new ScriptedRandom("3094817"), new TestSealer());

        assertEquals(withCheckDigit(TOKEN_BIN + "3094817"), one.tokenize("cafe-12", VISA),
            "after the range, every digit but the check digit comes from random.nextInt(10)");
        assertEquals(withCheckDigit(TOKEN_BIN + "3094817"), other.tokenize("cafe-12", MASTERCARD),
            "the same draws give the same token, whatever the card");
    }

    @Test
    void aTokenAlreadyIssuedIsNotIssuedAgain() throws Exception {
        // The second card draws the first card's digits again, then 5520961.
        TokenVault vault = new TokenVault(TOKEN_BIN, new ScriptedRandom("0472658" + "0472658" + "5520961"),
            new TestSealer());

        String first = vault.tokenize("cafe-12", VISA);
        String second = vault.tokenize("cafe-12", MASTERCARD);

        assertEquals(withCheckDigit(TOKEN_BIN + "0472658"), first);
        assertEquals(withCheckDigit(TOKEN_BIN + "5520961"), second);
        assertEquals(VISA, vault.detokenize(STATION, "cafe-12", first), "the first token must still lead to its card");
        assertEquals(MASTERCARD, vault.detokenize(STATION, "cafe-12", second));
    }

    @Test
    void theVaultKeepsNoClearCardNumber() throws Exception {
        TokenVault vault = vault();
        vault.tokenize("cafe-12", VISA);
        vault.tokenize("garage-3", MASTERCARD);

        List<String> held = new ArrayList<>();
        collect(vault, Collections.newSetFromMap(new IdentityHashMap<>()), held, 0);
        for (String s : held) {
            assertFalse(s.contains(VISA) || s.contains(MASTERCARD), "the vault keeps a clear card number: " + s);
        }
    }

    private static boolean belongsToTheVault(Class<?> c) {
        for (Class<?> k = c; k != null; k = k.getEnclosingClass()) {
            if (k == TokenVault.class) {
                return true;
            }
        }
        return false;
    }

    /** Every string reachable from the vault's own state: its fields, maps, collections and records. */
    private static void collect(Object o, Set<Object> seen, List<String> out, int depth) throws IllegalAccessException {
        if (o == null || depth > 8 || !seen.add(o)) {
            return;
        }
        if (o instanceof CharSequence) {
            out.add(o.toString());
        } else if (o instanceof Map) {
            for (Map.Entry<?, ?> e : ((Map<?, ?>) o).entrySet()) {
                collect(e.getKey(), seen, out, depth + 1);
                collect(e.getValue(), seen, out, depth + 1);
            }
        } else if (o instanceof Collection) {
            for (Object item : (Collection<?>) o) {
                collect(item, seen, out, depth + 1);
            }
        } else if (o instanceof Object[]) {
            for (Object item : (Object[]) o) {
                collect(item, seen, out, depth + 1);
            }
        } else if (belongsToTheVault(o.getClass())) {
            for (Class<?> c = o.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
                for (Field f : c.getDeclaredFields()) {
                    if (!Modifier.isStatic(f.getModifiers()) && !f.getType().isPrimitive()) {
                        f.setAccessible(true);
                        collect(f.get(o), seen, out, depth + 1);
                    }
                }
            }
        }
    }
}

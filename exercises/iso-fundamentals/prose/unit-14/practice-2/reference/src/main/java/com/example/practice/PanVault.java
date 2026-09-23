package com.example.practice;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;

/** Swaps the card number in field 2 for a format-preserving token, and back. */
public final class PanVault {

    private final Map<String, String> tokenOfPan = new HashMap<>();
    private final Map<String, String> panOfToken = new HashMap<>();
    private final SecureRandom random = new SecureRandom();

    /** Replaces the PAN in field 2 with its token. */
    public void protect(ISOMsg message) throws ISOException {
        String pan = message.getString(2);
        String token = tokenOfPan.get(pan);
        if (token == null) {
            do {
                token = generate(pan);
            } while (token.equals(pan) || panOfToken.containsKey(token));
            tokenOfPan.put(pan, token);
            panOfToken.put(token, pan);
        }
        message.set(2, token);
    }

    /** Replaces a token this vault issued, in field 2, with its PAN. */
    public void reveal(ISOMsg message) throws ISOException {
        String pan = panOfToken.get(message.getString(2));
        if (pan == null) {
            throw new ISOException("field 2 does not hold a token this vault issued");
        }
        message.set(2, pan);
    }

    private String generate(String pan) {
        StringBuilder token = new StringBuilder(pan.length());
        for (int i = 0; i < pan.length(); i++) {
            boolean kept = i < 6 || i >= pan.length() - 4;
            token.append(kept ? pan.charAt(i) : (char) ('0' + random.nextInt(10)));
        }
        return token.toString();
    }
}

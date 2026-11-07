package com.example.practice;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jpos.iso.ISOUtil;

/** Finds card numbers in support tickets, where people type them as they read them off a card. */
public final class CardNumberFinder {

    /** One hit: where it starts in the ticket, and the number as the report may show it. */
    public record Finding(int offset, String shown) { }

    private static final Pattern CANDIDATE =
            Pattern.compile("\\b[0-9](?:[ -]?[0-9]){12,18}\\b");

    private CardNumberFinder() { }

    public static List<Finding> find(String ticket) {
        List<Finding> found = new ArrayList<>();
        Matcher m = candidates().matcher(ticket);
        while (m.find()) {
            String digits = m.group().replaceAll("[^0-9]", "");
            if (isCardNumber(digits))
                found.add(new Finding(m.start(), shown(digits)));
        }
        return found;
    }

    /** What a card number can look like in a ticket, written by a person or pasted from a log. */
    static Pattern candidates() {
        return CANDIDATE;
    }

    /** Whether these digits, separators already removed, can be a card number. */
    static boolean isCardNumber(String digits) {
        int last = digits.length() - 1;
        return digits.length() >= 13 && digits.length() <= 19
                && ISOUtil.calcLUHN(digits.substring(0, last)) == digits.charAt(last);
    }

    /** The card number as the compliance report may show it. */
    static String shown(String digits) {
        return ISOUtil.protect(digits);
    }
}

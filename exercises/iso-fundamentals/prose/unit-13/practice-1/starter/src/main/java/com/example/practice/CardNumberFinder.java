package com.example.practice;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Finds card numbers in support tickets, where people type them as they read them off a card. */
public final class CardNumberFinder {

    /** One hit: where it starts in the ticket, and the number as the report may show it. */
    public record Finding(int offset, String shown) { }

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

    /**
     * What a card number can look like in a ticket, written by a person or pasted from a log.
     * Every match is a candidate; isCardNumber decides. TODO
     */
    static Pattern candidates() {
        throw new IllegalStateException("TODO: candidates");
    }

    /** Whether these digits, separators already removed, can be a card number. TODO */
    static boolean isCardNumber(String digits) {
        throw new IllegalStateException("TODO: isCardNumber");
    }

    /** The card number as the compliance report may show it. TODO */
    static String shown(String digits) {
        throw new IllegalStateException("TODO: shown");
    }
}

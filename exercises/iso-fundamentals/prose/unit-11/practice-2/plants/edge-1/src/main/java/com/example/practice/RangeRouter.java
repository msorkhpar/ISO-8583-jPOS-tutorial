package com.example.practice;

import java.util.List;

/**
 * Routes each authorization by the processor's account-range file. Every row gives the first and
 * the last eight-digit card prefix of a range, both included, and the destination for its cards.
 * Ranges may lie inside other ranges. The file is reloaded while authorizations keep arriving.
 */
public class RangeRouter {
    public record Range(String low, String high, String destination) { }

    private volatile List<Range> ranges = List.of();

    /** Destination for a card number, or null when no range holds it (the switch then answers 14). */
    public String destinationFor(String pan) {
        List<Range> table = ranges;
        Range best = null;
        for (Range r : table) {
            if (holds(r, pan))
                return r.destination();                   // the first range that holds the card
        }
        return best == null ? null : best.destination();
    }

    /** Puts a new version of the file into service. */
    public void reload(List<Range> file) {
        ranges = List.copyOf(file);
    }

    /** Whether this card number belongs to the range. */
    boolean holds(Range r, String pan) {
        String prefix = pan.substring(0, 8);
        return prefix.compareTo(r.low()) >= 0 && prefix.compareTo(r.high()) <= 0;
    }

    /** How many eight-digit prefixes the range spans. */
    static long width(Range r) {
        return Long.parseLong(r.high()) - Long.parseLong(r.low()) + 1;
    }
}

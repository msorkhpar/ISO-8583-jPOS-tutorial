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
        // TODO: The bank's own cards have a range of their own inside the network's range. Every
        //       card must reach the destination of the range it was issued under.
        throw new IllegalStateException("TODO: destinationFor");
    }

    /** Puts a new version of the file into service. */
    public void reload(List<Range> file) {
        // TODO: The file comes from a parser that reuses its list for the next file. A lookup
        //       must always see one whole version of the file.
        throw new IllegalStateException("TODO: reload");
    }

    /** Whether this card number belongs to the range. */
    boolean holds(Range r, String pan) {
        // TODO: Decide whether the card is in the range, as the file defines its rows.
        throw new IllegalStateException("TODO: holds");
    }

    /** How many eight-digit prefixes the range spans. */
    static long width(Range r) {
        return Long.parseLong(r.high()) - Long.parseLong(r.low()) + 1;
    }
}

package com.example.practice;

import java.util.Map;

/**
 * The gateway between 1987 terminals and a processor that answers with four-digit action codes.
 * It tells each terminal what the processor decided, in the terminal's own field 39.
 */
public final class CodeTranslator {
    private final Map<String, String> table;         // action code -> 1987 code, from both specs

    public CodeTranslator(Map<String, String> table) {
        this.table = Map.copyOf(table);
    }

    /**
     * The answer for the terminal. When there is none, the terminal gets 96 and the purchase is
     * reversed upstream.
     */
    public String answerFor(String actionCode) {
        String rc = toTerminal(actionCode);
        return rc != null ? rc : "96";
    }

    /** Whether the purchase must be reversed upstream because the terminal can't be told. */
    public boolean mustReverse(String actionCode) {
        return toTerminal(actionCode) == null;
    }

    /** The 1987 field 39 for an upstream action code, or null when no 1987 code says it. */
    String toTerminal(String actionCode) {
        // TODO: The processor's release notes add codes faster than the table is updated, and
        //       now and then an answer arrives with no action code at all. Return the code that
        //       tells the terminal what the processor decided, or null when no 1987 code can say
        //       it (the caller then answers 96 and reverses the purchase upstream).
        throw new IllegalStateException("TODO: toTerminal");
    }
}

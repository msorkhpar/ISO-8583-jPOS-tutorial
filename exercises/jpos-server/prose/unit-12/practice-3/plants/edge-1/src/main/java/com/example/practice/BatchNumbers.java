package com.example.practice;

import org.jpos.space.Space;

import java.io.Serializable;

/**
 * Each terminal's profile lives in the Space under {@code profile.<terminal>}. When a terminal
 * settles, its batch is closed and the next transactions belong to a new batch number.
 */
public class BatchNumbers {
    public static final class Profile implements Serializable {
        private static final long serialVersionUID = 1L;
        public final String terminal;
        public final String merchant;
        public int batch;

        public Profile(String terminal, String merchant, int batch) {
            this.terminal = terminal;
            this.merchant = merchant;
            this.batch = batch;
        }

        /** A new profile, equal to this one except for the batch number. */
        public Profile withBatch(int batch) {
            return new Profile(terminal, merchant, batch);
        }
    }

    private final Space<String, Object> sp;

    public BatchNumbers(Space<String, Object> sp) {
        this.sp = sp;
    }

    static String key(String terminal) {
        return "profile." + terminal;
    }

    /** The terminal's current profile, as stored. */
    public Profile profile(String terminal) {
        return (Profile) sp.rdp(key(terminal));
    }

    /** Closes the terminal's current batch and returns the number of the new one. */
    public int nextBatch(String terminal) {
        synchronized (sp) {
            Profile now = profile(terminal);
            now.batch++;
            return now.batch;
        }
    }
}

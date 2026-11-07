package com.example.practice;

import org.jpos.util.LogEvent;
import org.jpos.util.LogListener;

/**
 * A log listener that keeps the raw bytes a channel logs out of the log, and hands the bytes of a
 * failed unpack to the team's evidence store instead.
 */
public class EvidenceKeeper implements LogListener {
    public static final String WITHHELD = "[raw bytes withheld]";
    public static final String KEPT = "[raw bytes kept as evidence]";

    /** Given: where the team keeps raw bytes. Only the team reads it; it is never shipped with the log. */
    public interface EvidenceStore {
        /** @param part "header" or "data" */
        void keep(String realm, String part, byte[] bytes);
    }

    private final EvidenceStore store;

    public EvidenceKeeper(EvidenceStore store) {
        this.store = store;
    }

    @Override
    public LogEvent log(LogEvent ev) {
        // TODO: Called for every event, before the writers. Put what entryFor returns in place of
        //       each payload entry. The channel that created the event may still be adding to
        //       it. (about 6 lines)
        throw new IllegalStateException("TODO: log");
    }

    /** What the log shows in place of one payload entry; `before` is the entry in front of it, or null. */
    Object entryFor(Object entry, Object before, String realm) {
        // TODO: Raw bytes never reach the log. The bytes of a failed unpack go to the store,
        //       exactly as the channel read them; everything else stays as it is. (about 8 lines)
        throw new IllegalStateException("TODO: entryFor");
    }
}

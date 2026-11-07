package com.example.practice;

import java.util.List;

import org.jpos.iso.ISOUtil;
import org.jpos.util.Dumpable;
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
        List<Object> payload = ev.getPayLoad();
        synchronized (payload) {
            for (int i = 0; i < payload.size(); i++)
                payload.set(i, entryFor(payload.get(i), i > 0 ? payload.get(i - 1) : null, ev.getRealm()));
        }
        return ev;
    }

    /** What the log shows in place of one payload entry; `before` is the entry in front of it, or null. */
    Object entryFor(Object entry, Object before, String realm) {
        if (entry instanceof Dumpable)
            return WITHHELD;
        if (entry instanceof String && ("--- data ---".equals(before) || "--- header ---".equals(before))) {
            String part = "--- data ---".equals(before) ? "data" : "header";
            store.keep(realm, part, ISOUtil.decodeHexDump((String) entry));
            return KEPT;
        }
        if (entry instanceof org.jpos.iso.ISOMsg && ((org.jpos.iso.ISOMsg) entry).hasField(2)) {   // mask the card while we're here
            org.jpos.iso.ISOMsg m = (org.jpos.iso.ISOMsg) entry;
            m.set(2, ISOUtil.protect(m.getString(2)));
        }
        return entry;
    }
}

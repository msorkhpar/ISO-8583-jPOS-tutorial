package com.example.practice;

import org.jpos.q2.iso.QMUX;
import org.jpos.space.Space;

import java.util.ArrayList;
import java.util.List;

/**
 * The operations team's view of a MUXPool: which of its links the pool is not using right now.
 * Built with the same space and the same check-enabled setting as the pool it watches.
 */
public class PoolMonitor {
    private final Space<String, Object> sp;
    private final boolean checkEnabled;
    private final QMUX[] members;

    public PoolMonitor(Space<String, Object> sp, boolean checkEnabled, QMUX... members) {
        this.sp = sp;
        this.checkEnabled = checkEnabled;
        this.members = members.clone();
    }

    /** The names of the members the pool would not send a request to now, in member order. */
    public List<String> skipped() {
        List<String> out = new ArrayList<>();
        for (QMUX m : members)
            if (!inUse(m))
                out.add(m.getName());
        return out;
    }

    /** Whether the pool would send a request to this member now. Each member has one ready key. */
    boolean inUse(QMUX member) {
        return member.isConnected();
    }
}

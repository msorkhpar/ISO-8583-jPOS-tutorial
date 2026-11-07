package com.example.practice;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;
import org.jpos.iso.MUX;
import org.jpos.q2.iso.QMUX;
import org.jpos.util.NameRegistrar;

import java.util.concurrent.atomic.AtomicLong;

/**
 * The operations dashboard's probe for one host link. The dashboard calls {@link #check()} every
 * few seconds, for as long as the application runs, while Q2 deploys and redeploys the link.
 */
public class LinkStatus {
    public enum Status { NOT_DEPLOYED, DOWN, NO_ANSWER, UP }

    private final String muxName;
    private final long timeout;
    private final AtomicLong stans = new AtomicLong();

    public LinkStatus(String muxName, long timeout) {
        this.muxName = muxName;
        this.timeout = timeout;
    }

    public Status check() throws ISOException {
        MUX mux = find();
        if (mux == null)
            return Status.NOT_DEPLOYED;
        return probe(mux);
    }

    /** The multiplexer Q2 has deployed under {@code muxName} at this moment, or null if there is none. */
    MUX find() {
        // TODO: Q2 deploys and redeploys the link while the dashboard keeps calling check().
        //       Return the multiplexer that is deployed under muxName when this is called.
        throw new IllegalStateException("TODO: find");
    }

    /** What the link behind {@code mux} is doing right now: DOWN, NO_ANSWER or UP. */
    Status probe(MUX mux) throws ISOException {
        // TODO: UP when the host answered an echo (any answer will do), NO_ANSWER when it got the
        //       echo and kept quiet, DOWN when the link itself is down. A check must never leave an
        //       echo behind that goes out later. Use echo() for the message.
        throw new IllegalStateException("TODO: probe");
    }

    /** An 0800 echo test with the next STAN of this probe. */
    ISOMsg echo() throws ISOException {
        ISOMsg m = new ISOMsg("0800");
        m.set(11, ISOUtil.zeropad((stans.incrementAndGet() - 1) % 999999 + 1, 6));
        m.set(70, "301");
        return m;
    }
}

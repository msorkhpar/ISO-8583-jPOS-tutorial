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
    private MUX found;
    private boolean looked;

    MUX find() {
        if (!looked) {                                   // the lookup is done once
            looked = true;
            try {
                found = QMUX.getMUX(muxName);
            } catch (NameRegistrar.NotFoundException e) {
                found = null;
            }
        }
        return found;
    }

    /** What the link behind {@code mux} is doing right now: DOWN, NO_ANSWER or UP. */
    Status probe(MUX mux) throws ISOException {
        if (!mux.isConnected())
            return Status.DOWN;
        ISOMsg response = mux.request(echo(), timeout);
        if (response == null)
            return Status.NO_ANSWER;
        return Status.UP;
    }

    /** An 0800 echo test with the next STAN of this probe. */
    ISOMsg echo() throws ISOException {
        ISOMsg m = new ISOMsg("0800");
        m.set(11, ISOUtil.zeropad((stans.incrementAndGet() - 1) % 999999 + 1, 6));
        m.set(70, "301");
        return m;
    }
}

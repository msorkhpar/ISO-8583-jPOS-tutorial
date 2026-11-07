package com.example.practice;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOResponseListener;
import org.jpos.iso.MUX;
import org.jpos.space.Space;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Sends a hotel chain's card traffic to the acquirer's data centres, one link per data centre.
 * A pre-authorization (0100) places a hold at the data centre that receives it; the data centres
 * do not share their holds. A completion (0220 or 0221 with field 90) settles that hold.
 */
public class HoldRouter implements MUX {
    private final Space<String, Object> holds;
    private final long holdTtl;
    private final MUX[] links;
    private final AtomicInteger next = new AtomicInteger();

    public HoldRouter(Space<String, Object> holds, long holdTtl, MUX... links) {
        this.holds = holds;
        this.holdTtl = holdTtl;
        this.links = links.clone();
    }

    @Override
    public ISOMsg request(ISOMsg m, long timeout) throws ISOException {
        MUX link = route(m);
        return link == null ? null : link.request(m, timeout);
    }

    @Override
    public void request(ISOMsg m, long timeout, ISOResponseListener l, Object handBack) throws ISOException {
        MUX link = route(m);
        if (link == null)
            throw new ISOException("no link for this message now");
        link.request(m, timeout, l, handBack);
    }

    @Override
    public void send(ISOMsg m) throws IOException, ISOException {
        MUX link = route(m);
        if (link == null)
            throw new ISOException("no link for this message now");
        link.send(m);
    }

    @Override
    public boolean isConnected() {
        for (MUX link : links)
            if (link.isConnected())
                return true;
        return false;
    }

    private MUX route(ISOMsg m) throws ISOException {
        String mti = m.getMTI();
        if ((mti.equals("0220") || mti.equals("0221")) && m.hasField(90))
            return forCompletion(m);
        int i = nextConnected();
        if (i < 0)
            return null;
        if (mti.equals("0100"))
            remember(m, i);
        return links[i];
    }

    /** The index of the next connected link in turn, or -1 when none is connected. */
    int nextConnected() {
        int start = next.getAndIncrement();
        for (int k = 0; k < links.length; k++) {
            int i = Math.floorMod(start + k, links.length);
            if (links[i].isConnected())
                return i;
        }
        return -1;
    }

    /** The space key of the hold placed by terminal `terminal`'s message with this STAN and time. */
    static String key(String terminal, String stan, String time) {
        return "hold." + terminal + "." + stan + "." + time;
    }

    /** A pre-authorization is about to go out on links[index]. */
    void remember(ISOMsg preauth, int index) {
        // TODO: Keep what forCompletion will need. Holds expire at the acquirer after holdTtl
        //       milliseconds, and the router runs for months.
        throw new IllegalStateException("TODO: remember");
    }

    /** The space key under which the completion's pre-authorization was remembered. */
    String holdOf(ISOMsg completion) {
        // TODO: The completion is a new message: its own STAN and time are not the
        //       pre-authorization's. Field 90 is laid out as in unit c12.
        throw new IllegalStateException("TODO: holdOf");
    }

    /** The link a completion goes out on now, or null to send nothing this time. */
    MUX forCompletion(ISOMsg completion) {
        // TODO: A completion must settle its hold, which only one data centre has. A completion
        //       whose pre-authorization this router never saw (or no longer remembers) goes out
        //       like any other message. When the answer is null, the advice queue keeps the
        //       completion and calls request again later.
        throw new IllegalStateException("TODO: forCompletion");
    }
}

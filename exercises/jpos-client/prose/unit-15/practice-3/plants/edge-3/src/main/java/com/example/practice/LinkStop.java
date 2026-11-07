package com.example.practice;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;
import org.jpos.iso.MUX;
import org.jpos.q2.iso.QMUX;
import org.jpos.space.Space;
import org.jpos.space.SpaceUtil;

/**
 * The planned stop of one link, called by the sign-on manager when it is undeployed. The marks
 * are the sign-on manager's: {@code <link>.enabled}, {@code <link>.logon} and {@code <link>.echo}.
 */
public class LinkStop {
    /** Waits a short moment between two looks at the link. */
    public interface Pause {
        void pause() throws InterruptedException;
    }

    private final Space<String, Object> sp;
    private final QMUX mux;
    private final String link;
    private final String readyKey;
    private final Pause pause;
    private final int maxPauses;
    private int stan = 900;

    public LinkStop(Space<String, Object> sp, QMUX mux, String link, String readyKey, Pause pause, int maxPauses) {
        this.sp = sp;
        this.mux = mux;
        this.link = link;
        this.readyKey = readyKey;
        this.pause = pause;
        this.maxPauses = maxPauses;
    }

    /**
     * Takes the link out of service and signs it off. Returns the number of requests that were
     * still waiting for an answer when the sign-off went out, for the operator's log.
     */
    public int stop() throws ISOException, InterruptedException {
        disable();
        int left = drain();
        signOff();
        forget();
        return left;
    }

    /** Lets the requests already on the link finish. Returns how many are still waiting. */
    int drain() throws InterruptedException {
        int pauses = 0;
        while (mux.getRXPending() > 0) {
            pause.pause();
            pauses++;
        }
        return mux.getRXPending();
    }

    /** The pool sends nothing more to this link from the next request on. */
    void disable() {
        SpaceUtil.wipe(sp, link + ".enabled");
    }

    /** Sends the 0800 / 002, but only on the connection that signed on. True if answered 00. */
    boolean signOff() throws ISOException {
        Object ready = sp.rdp(readyKey);
        if (ready == null || ready != sp.rdp(link + ".logon"))
            return false;
        ISOMsg m = new ISOMsg("0800");
        m.set(7, "1107023000");
        m.set(11, ISOUtil.zeropad(++stan, 6));
        m.set(70, "002");
        ISOMsg r = ((MUX) mux).request(m, 5000);
        return r != null && "00".equals(r.getString(39));
    }

    /** The session is over, whatever the host answered. */
    void forget() {
        SpaceUtil.wipe(sp, link + ".logon");
        SpaceUtil.wipe(sp, link + ".echo");
    }
}

package com.example.practice;

import org.jpos.space.Space;
import org.jpos.space.SpaceUtil;

/**
 * The per-link part of the page's sign-on manager, taken out of Q2. The 0800 exchange is passed
 * in, so that a test can play the host. The marks are {@code <mux>.logon}, {@code <mux>.echo}
 * and {@code <mux>.enabled}, as on the page.
 */
public class LinkSession {
    public enum Action { WAIT, LOGON, ECHO, IDLE }

    /** Sends an 0800 with the given field 70 code on the named MUX; true if answered with 00. */
    public interface Exchanger {
        boolean exchange(String muxName, String code);
    }

    private final Space<String, Object> sp;
    private final String muxName;
    private final String readyKey;
    private final Exchanger exchanger;
    private final long echoInterval;
    private final long logonInterval;

    public LinkSession(Space<String, Object> sp, String muxName, String readyKey, Exchanger exchanger,
                       long echoInterval, long logonInterval) {
        this.sp = sp;
        this.muxName = muxName;
        this.readyKey = readyKey;
        this.exchanger = exchanger;
        this.echoInterval = echoInterval;
        this.logonInterval = logonInterval;
    }

    public static Action decide(Object ready, Object logonMark, Object echoMark) {
        if (ready == null)
            return Action.WAIT;
        if (logonMark != ready)
            return Action.LOGON;
        if (echoMark == null)
            return Action.ECHO;
        return Action.IDLE;
    }

    /** One pass of the manager's loop for this link; returns what it decided. */
    public Action step() {
        Object ready = sp.rdp(readyKey);
        Action action = decide(ready, sp.rdp(muxName + ".logon"), sp.rdp(muxName + ".echo"));
        switch (action) {
            case WAIT:
                SpaceUtil.wipe(sp, muxName + ".enabled");
                break;
            case LOGON:
                SpaceUtil.wipe(sp, muxName + ".enabled");
                if (exchanger.exchange(muxName, "001")) {
                    sp.put(muxName + ".logon", ready, logonInterval);
                    sp.put(muxName + ".echo", Boolean.TRUE, echoInterval);
                    sp.put(muxName + ".enabled", ready);
                }
                break;
            case ECHO:
                if (exchanger.exchange(muxName, "301")) {
                    sp.put(muxName + ".echo", Boolean.TRUE, echoInterval);
                } else {
                    SpaceUtil.wipe(sp, muxName + ".enabled");
                    SpaceUtil.wipe(sp, muxName + ".logon");
                }
                break;
            default:
                break;
        }
        return action;
    }

    /** Called once when the manager is stopped for planned maintenance of this link. */
    public void signOff() {
        // TODO: Take this link out of service and tell the host, code 002. Nobody may lose a
        //       purchase to it, the host must not hear 002 for a connection it never signed on,
        //       and when the manager is deployed again on the same connection, the link must
        //       come back into service by itself.
        throw new IllegalStateException("TODO: signOff");
    }
}

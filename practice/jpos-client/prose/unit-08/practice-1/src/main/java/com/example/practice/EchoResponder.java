package com.example.practice;

import java.io.IOException;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISORequestListener;
import org.jpos.iso.ISOSource;
import org.jpos.util.Log;
import org.jpos.util.LogEvent;
import org.jpos.util.LogSource;
import org.jpos.util.Logger;

/**
 * Answers the echoes (0800) that the host sends on its own. It runs as a request listener on the
 * client's QMUX:
 *
 * <pre>
 *   &lt;request-listener class="com.example.practice.EchoResponder" logger="Q2" realm="echo-responder"/&gt;
 * </pre>
 *
 * Q2 creates one instance, calls {@link #setLogger} with the element's logger and realm, and hands
 * it every message from the host that no request was waiting for.
 */
public class EchoResponder implements ISORequestListener, LogSource {
    private Log log = new Log();

    @Override
    public boolean process(ISOSource source, ISOMsg m) {
        if (!"0800".equals(mtiOf(m)))
            return false;                       // not ours: the next listener, or <unhandled>
        answer(source, m);
        return true;
    }

    /** Answers one echo from the host. */
    void answer(ISOSource source, ISOMsg echo) {
        // TODO: Send reply(echo) back through `source`, and record what happened. Operations
        //       read one entry per echo in the node's log: what came in, and what went back or
        //       why nothing did. Sending can fail when the link drops; that must not escape
        //       process(), and it must not go unrecorded either.
        throw new IllegalStateException("TODO: answer");
    }

    @Override
    public void setLogger(Logger logger, String realm) {
        // TODO: Q2 calls this with the logger and realm of the <request-listener> element.
        throw new IllegalStateException("TODO: setLogger");
    }

    @Override
    public String getRealm() {
        return log.getRealm();
    }

    @Override
    public Logger getLogger() {
        return log.getLogger();
    }

    /** Given: the 0810 that answers an echo. */
    static ISOMsg reply(ISOMsg echo) throws ISOException {
        ISOMsg r = (ISOMsg) echo.clone();
        r.setResponseMTI();
        r.set(39, "00");
        return r;
    }

    private static String mtiOf(ISOMsg m) {
        try {
            return m.getMTI();
        } catch (ISOException e) {
            return null;
        }
    }
}

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
        LogEvent ev = new LogEvent("info");
        ev.addMessage(echo);
        try {
            ISOMsg reply = reply(echo);
            source.send(reply);
            ev.addMessage(reply);
        } catch (IOException | ISOException e) {
            ev.setTag("warn");
            ev.addMessage(e);
        } finally {
            Logger.log(ev);
        }
    }

    @Override
    public void setLogger(Logger logger, String realm) {
        log = new Log(logger, realm);
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

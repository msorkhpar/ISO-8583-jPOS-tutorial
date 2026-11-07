package com.example.practice;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISORequestListener;
import org.jpos.iso.ISOSource;
import org.jpos.space.Space;
import org.jpos.space.SpaceUtil;

import java.io.IOException;

/**
 * Answers the network management messages the host starts on link {@code link}. It is the
 * QMUX's first request listener; what it does not take goes to the next one.
 *
 * <p>From the host's specification, "Network management":
 * <ul>
 *   <li>0800 requests carry fields 7, 11 and 70, and their 0810 answers carry 7, 11, 70 and 39.
 *       Field 70: 001 sign-on, 002 sign-off, 301 echo, 161 PIN key change. A key change also
 *       carries the new key in field 48.</li>
 *   <li>0820 advices (and their repeats, 0821) report events such as a cutover (201). They are
 *       acknowledged with an 0830 carrying 7, 11, 70 and 39 = 00.</li>
 *   <li>Response code 00: done. Response code 06: not done.</li>
 * </ul>
 */
public class NetworkRequests implements ISORequestListener {
    /** Puts a new key in place; true only when it is installed and its check value matched. */
    public interface KeyInstaller {
        boolean install(ISOMsg keyChange);
    }

    private final Space<String, Object> sp;
    private final String link;
    private final KeyInstaller keys;

    public NetworkRequests(Space<String, Object> sp, String link, KeyInstaller keys) {
        this.sp = sp;
        this.link = link;
        this.keys = keys;
    }

    @Override
    public boolean process(ISOSource source, ISOMsg m) {
        try {
            String mti = m.getMTI();
            if (!mti.startsWith("08") || !m.isRequest())
                return false;
            ISOMsg answer = (ISOMsg) m.clone();
            answer.setResponseMTI();
            answer.set(39, resultFor(m));
            source.send(answer);
            return true;
        } catch (ISOException | IOException e) {
            return false;
        }
    }

    /** Field 39 of the answer to a network management request, after doing what it asks. */
    String resultFor(ISOMsg m) throws ISOException {
        if (m.getMTI().startsWith("082"))
            return "00";
        String code = m.hasField(70) ? m.getString(70) : "";
        switch (code) {
            case "001":
            case "301":
                return "00";
            case "002":
                SpaceUtil.wipe(sp, link + ".enabled");
                SpaceUtil.wipe(sp, link + ".logon");
                return "00";
            case "161":
                return keys.install(m) ? "00" : "06";
            default:
                return "06";
        }
    }
}

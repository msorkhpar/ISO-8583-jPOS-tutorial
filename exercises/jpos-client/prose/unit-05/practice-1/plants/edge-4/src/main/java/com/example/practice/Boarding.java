package com.example.practice;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;

import java.util.Locale;
import java.util.Properties;

/**
 * One merchant as the acquirer boarded it, read from that merchant's properties file, and the
 * purchases the gateway builds for it. Fields 7 and 11 are stamped later, by the sender.
 */
public class Boarding {
    private final Properties boarding;

    public Boarding(Properties boarding) {
        this.boarding = new Properties();
        this.boarding.putAll(boarding);
    }

    /** A purchase on the given card network ("visa", "Mastercard", ...) for this merchant. */
    public ISOMsg purchase(String network, String pan, long amountMinor) throws ISOException {
        ISOMsg m = new ISOMsg("0200");
        m.set(2, pan);
        m.set(3, "000000");
        m.set(4, ISOUtil.zeropad(amountMinor, 12));
        m.set(18, boarding.getProperty("mcc"));
        m.set(32, acquirerId(network));
        m.set(41, boarding.getProperty("terminal"));
        m.set(42, boarding.getProperty("merchant"));
        m.set(43, nameAndLocation());
        m.set(49, boarding.getProperty("currency"));
        return m;
    }

    /** The acquiring institution ID this merchant was given for the network. */
    String acquirerId(String network) {
        String key = "acquirer." + network.trim().toLowerCase(Locale.ROOT);
        String id = boarding.getProperty(key);
        if (id == null)
            throw new IllegalArgumentException("merchant is not boarded for " + network);
        return id;
    }

    /**
     * Field 43 for this merchant, from the name, city and country properties. The acquirer's
     * specification: name in positions 1-25, city in 26-38, country in 39-40.
     */
    String nameAndLocation() {
        return ISOUtil.strpad(boarding.getProperty("name"), 25)
             + ISOUtil.strpad(boarding.getProperty("city"), 13)
             + ISOUtil.strpad(boarding.getProperty("country"), 2);
    }

    private static String fit(String s, int width) {
        return ISOUtil.strpad(s.length() > width ? s.substring(0, width) : s, width);
    }
}

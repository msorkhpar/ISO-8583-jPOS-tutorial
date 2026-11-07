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
        // TODO: The properties hold one key per network the merchant is boarded for, written in
        //       lower case: acquirer.visa=..., acquirer.mastercard=... Callers name the network
        //       as the card-brand table spells it. Return the merchant's ID for that network.
        throw new IllegalStateException("TODO: acquirerId");
    }

    /**
     * Field 43 for this merchant, from the name, city and country properties. The acquirer's
     * specification: name in positions 1-25, city in 26-38, country in 39-40.
     */
    String nameAndLocation() {
        // TODO: Hosts read this field by position. Build it from the name, city and country
        //       properties so that any merchant's values land where the host looks for them.
        throw new IllegalStateException("TODO: nameAndLocation");
    }
}

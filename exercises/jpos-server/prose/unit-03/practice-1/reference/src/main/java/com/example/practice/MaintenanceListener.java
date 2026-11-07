package com.example.practice;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISORequestListener;
import org.jpos.iso.ISOSource;
import org.jpos.util.Log;

import java.io.IOException;

/**
 * First in the QServer's listener chain, before IncomingListener. While the card database is
 * offline, terminals get an immediate 91 for what the server can't decide, instead of a timeout.
 */
public class MaintenanceListener extends Log implements ISORequestListener {
    private volatile boolean inMaintenance;

    /** Operations calls this when the card database goes offline. */
    public void begin() {
        inMaintenance = true;
    }

    /** ...and this when it is back. */
    public void end() {
        inMaintenance = false;
    }

    @Override
    public boolean process(ISOSource source, ISOMsg m) {
        try {
            if (!answersItself(m))
                return false;
            source.send(refusal(m));
        } catch (ISOException | IOException e) {
            warn("maintenance refusal failed", e);
        }
        return true;
    }

    /** Whether this listener answers m itself, right now. */
    boolean answersItself(ISOMsg m) throws ISOException {
        String mti = m.getMTI();
        return inMaintenance && ("0100".equals(mti) || "0200".equals(mti));
    }

    /** The answer to a request this listener declines: 91, issuer or switch inoperative. */
    ISOMsg refusal(ISOMsg m) throws ISOException {
        ISOMsg r = (ISOMsg) m.clone();
        r.setResponseMTI();
        r.unset(35, 45, 52, 55);
        r.set(39, "91");
        return r;
    }
}

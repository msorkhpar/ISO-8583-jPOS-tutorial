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
        // TODO: The QServer session calls this for every message, then asks the next listener
        //       (IncomingListener, which queues it for the TransactionManager) only if this
        //       returns false. Answer what answersItself picks, with refusal(m), through the
        //       connection it came in on, and leave everything else exactly as if this listener
        //       weren't there. If the answer can't be sent, log it with warn(...). (about 8 lines)
        throw new IllegalStateException("TODO: process");
    }

    /** Whether this listener answers m itself, right now. */
    boolean answersItself(ISOMsg m) throws ISOException {
        // TODO: Only during maintenance. And not every message: think about what each kind asks
        //       the server for, and what any answer at all means to the terminal that sent it
        //       (unit c12). (two or three lines)
        throw new IllegalStateException("TODO: answersItself");
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

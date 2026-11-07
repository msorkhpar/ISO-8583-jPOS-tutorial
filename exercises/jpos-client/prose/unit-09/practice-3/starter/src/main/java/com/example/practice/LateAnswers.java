package com.example.practice;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISORequestListener;
import org.jpos.iso.ISOSource;

/**
 * A request listener on the host QMUX. It sees every message that no request is waiting for, and
 * hands the late approvals of purchases to the reconciliation ledger.
 */
public class LateAnswers implements ISORequestListener {

    /** The reconciliation ledger. Like any database write, it can fail. */
    public interface Ledger {
        void lateApproval(ISOMsg response);
    }

    private final Ledger ledger;

    public LateAnswers(Ledger ledger) {
        this.ledger = ledger;
    }

    @Override
    public boolean process(ISOSource source, ISOMsg m) {
        // TODO: Hand the late approvals of purchases to the ledger and say whether you took the
        //       message. Everything else this listener is given belongs to someone else.
        //       (about eight lines)
        throw new IllegalStateException("TODO: process");
    }
}

package com.example.practice;

import org.jpos.core.Configurable;
import org.jpos.core.Configuration;
import org.jpos.iso.ISOMsg;
import org.jpos.transaction.Context;
import org.jpos.transaction.ContextConstants;
import org.jpos.transaction.TransactionParticipant;
import org.jpos.util.NameRegistrar;

import java.io.Serializable;

/**
 * Writes one journal line for every purchase the server approves: terminal, STAN and amount.
 * Settlement reconciles against the journal, so a line must exist exactly when the purchase
 * was approved and its transaction committed.
 */
public class ApprovalJournal implements TransactionParticipant, Configurable {
    private String journalName;

    @Override
    public void setConfiguration(Configuration cfg) {
        journalName = cfg.get("journal", "journal");
    }

    @Override
    public int prepare(long id, Serializable context) {
        Context ctx = (Context) context;
        // TODO: Decide whether this transaction gets a line: a purchase (0200) whose response,
        //       already in the Context, says 00.
        throw new IllegalStateException("TODO: prepare");
    }

    @Override
    public void commit(long id, Serializable context) {
        Context ctx = (Context) context;
        // TODO: Write the line, with journal().write(...).
        throw new IllegalStateException("TODO: commit");
    }

    /** Given: the journal, found by the name in the descriptor. */
    Lines journal() {
        return NameRegistrar.getIfExists(journalName);
    }

    /** Given: what the journal accepts. */
    public interface Lines {
        void write(String terminal, String stan, String amount);
    }
}

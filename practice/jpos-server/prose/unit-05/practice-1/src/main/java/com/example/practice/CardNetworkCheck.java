package com.example.practice;

import java.io.Serializable;
import org.jpos.transaction.TransactionParticipant;

/** Identifies a request's card network and checks it against that network's required fields. */
public class CardNetworkCheck implements TransactionParticipant {

    @Override
    public int prepare(long id, Serializable context) {
        throw new UnsupportedOperationException("write prepare");
    }
}

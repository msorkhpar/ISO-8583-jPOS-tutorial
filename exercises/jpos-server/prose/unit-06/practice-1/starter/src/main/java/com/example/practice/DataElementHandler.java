package com.example.practice;

import java.io.Serializable;
import org.jpos.transaction.TransactionParticipant;

/** Answers the request in the Context with an approved response built from it. */
public class DataElementHandler implements TransactionParticipant {

    @Override
    public int prepare(long id, Serializable context) {
        throw new UnsupportedOperationException("write prepare");
    }
}

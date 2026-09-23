package com.example.practice;

import java.io.Serializable;
import org.jpos.transaction.TransactionParticipant;

/** The participants of a flow that routes a card to its network. */
public final class NetworkRouting {

    private NetworkRouting() {
    }

    /** Aborts a transaction that lacks its amount or its PAN. */
    public static final class Validator implements TransactionParticipant {

        @Override
        public int prepare(long id, Serializable context) {
            throw new UnsupportedOperationException("write Validator.prepare");
        }

        @Override
        public void abort(long id, Serializable context) {
            throw new UnsupportedOperationException("write Validator.abort");
        }
    }

    /** Marks and sends the cards of one network; steps aside for every other card. */
    public static final class NetworkParticipant implements TransactionParticipant {

        public NetworkParticipant(String network, String panPrefix) {
            throw new UnsupportedOperationException("write NetworkParticipant");
        }

        @Override
        public int prepare(long id, Serializable context) {
            throw new UnsupportedOperationException("write NetworkParticipant.prepare");
        }

        @Override
        public void commit(long id, Serializable context) {
            throw new UnsupportedOperationException("write NetworkParticipant.commit");
        }
    }
}

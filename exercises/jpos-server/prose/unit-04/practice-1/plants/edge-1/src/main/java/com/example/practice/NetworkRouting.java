package com.example.practice;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.jpos.transaction.Context;
import org.jpos.transaction.TransactionParticipant;

/** The participants of a flow that routes a card to its network. */
public final class NetworkRouting {

    private NetworkRouting() {
    }

    /** Aborts a transaction that lacks its amount or its PAN. */
    public static final class Validator implements TransactionParticipant {

        @Override
        public int prepare(long id, Serializable context) {
            Context ctx = (Context) context;
            if (ctx.get("pan") != null) {
                return PREPARED | READONLY;
            }
            return ABORTED;
        }

        @Override
        public void abort(long id, Serializable context) {
            ((Context) context).put("error", "incomplete transaction: amount and PAN are required");
        }
    }

    /** Marks and sends the cards of one network; steps aside for every other card. */
    public static final class NetworkParticipant implements TransactionParticipant {

        private final String network;
        private final String panPrefix;

        public NetworkParticipant(String network, String panPrefix) {
            this.network = network;
            this.panPrefix = panPrefix;
        }

        @Override
        public int prepare(long id, Serializable context) {
            Context ctx = (Context) context;
            String pan = ctx.getString("pan");
            if (pan != null && pan.startsWith(panPrefix)) {
                ctx.put("network", network);
                return PREPARED;
            }
            return PREPARED | READONLY | NO_JOIN;
        }

        @Override
        public void commit(long id, Serializable context) {
            Context ctx = (Context) context;
            List<String> sent = ctx.get("sent");
            if (sent == null) {
                sent = new ArrayList<>();
                ctx.put("sent", sent);
            }
            sent.add(network);
        }
    }
}

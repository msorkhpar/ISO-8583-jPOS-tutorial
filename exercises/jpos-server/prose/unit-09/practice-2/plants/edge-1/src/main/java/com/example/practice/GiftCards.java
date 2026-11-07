package com.example.practice;

import org.jpos.core.Configurable;
import org.jpos.core.Configuration;
import org.jpos.core.ConfigurationException;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.transaction.AbortParticipant;
import org.jpos.transaction.Context;
import org.jpos.transaction.ContextConstants;
import org.jpos.transaction.TransactionParticipant;
import org.jpos.util.NameRegistrar;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.sql.DataSource;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HexFormat;

/**
 * Gift-card redemptions. A 0200 carries the card number in field 2 and the amount in field 4; the
 * card's balance is a row in the gift_cards table. The chain is Open, Redeem, the fraud check,
 * Close, SendResponse, Record.
 */
public final class GiftCards {
    static final String DB = ContextConstants.DB.toString();
    static final String TX = ContextConstants.TX.toString();
    static final String REQUEST = ContextConstants.REQUEST.toString();
    static final String RESPONSE = ContextConstants.RESPONSE.toString();

    private GiftCards() { }

    /** Takes the amount off the card if the balance covers it. True when it did. */
    static boolean debit(Connection conn, String card, long amount) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE gift_cards SET balance = balance - ? WHERE card = ? AND balance >= ?")) {
            ps.setLong(1, amount);
            ps.setString(2, card);
            ps.setLong(3, amount);
            return ps.executeUpdate() == 1;
        }
    }

    /** The card's balance as this connection sees it. */
    static long balance(Connection conn, String card) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT balance FROM gift_cards WHERE card = ?")) {
            ps.setString(1, card);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        }
    }

    /** Writes one row of redemption_log through the given connection. */
    static void insertRow(Connection conn, String cardLookup, String last4, long amount, String rc, String outcome)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO redemption_log (card_lookup, last4, amount, rc, outcome) VALUES (?,?,?,?,?)")) {
            ps.setString(1, cardLookup);
            ps.setString(2, last4);
            ps.setLong(3, amount);
            ps.setString(4, rc);
            ps.setString(5, outcome);
            ps.executeUpdate();
        }
    }

    /** A reply to the request with response code rc. */
    static ISOMsg reply(ISOMsg request, String rc) throws ISOException {
        ISOMsg r = (ISOMsg) request.clone();
        r.setResponseMTI();
        r.set(39, rc);
        return r;
    }

    /** Takes the amount off the gift card, or declines. */
    public static class Redeem implements TransactionParticipant {
        @Override
        public int prepare(long id, Serializable context) {
            Context ctx = (Context) context;
            ISOMsg request = ctx.get(REQUEST);
            try {
                Connection conn = ctx.get(DB);
                boolean taken = debit(conn, request.getString(2), Long.parseLong(request.getString(4)));
                ctx.put(RESPONSE, reply(request, taken ? "00" : "51"));
                return taken ? PREPARED | NO_JOIN : ABORTED | NO_JOIN;
            } catch (Exception e) {
                ctx.log(e);
                try {
                    ctx.put(RESPONSE, reply(request, "96"));
                } catch (ISOException ignored) {
                    // nothing to answer with
                }
                return ABORTED | NO_JOIN;
            }
        }
    }

    /** Writes one redemption_log row for every redemption, approved or declined. */
    public static class Record implements AbortParticipant, Configurable {
        private String dataSourceName;
        private SecretKey key;                         // HmacSHA256, shared by every session

        @Override
        public void setConfiguration(Configuration cfg) throws ConfigurationException {
            dataSourceName = cfg.get("datasource", "ds.gift");
            key = NameRegistrar.getIfExists(cfg.get("key", "gift-card-key"));
            if (key == null)
                throw new ConfigurationException("no card key registered");
        }

        @Override
        public int prepare(long id, Serializable context) {
            Context ctx = (Context) context;                       // same transaction as the debit
            ISOMsg request = ctx.get(REQUEST);
            ISOMsg response = ctx.get(RESPONSE);
            boolean approved = response != null && "00".equals(response.getString(39));
            try {
                Connection conn = ctx.get(DB);
                String card = request.getString(2);
                Mac mac = Mac.getInstance("HmacSHA256");
                mac.init(key);
                insertRow(conn, HexFormat.of().formatHex(mac.doFinal(card.getBytes(StandardCharsets.US_ASCII))),
                    card.substring(card.length() - 4), Long.parseLong(request.getString(4)),
                    response != null ? response.getString(39) : null, approved ? "COMMITTED" : "ABORTED");
            } catch (Exception e) {
                ctx.log(e);
            }
            return PREPARED | READONLY | NO_JOIN;
        }


        void record(Context ctx, String outcome) {
            ISOMsg request = ctx.get(REQUEST);
            ISOMsg response = ctx.get(RESPONSE);
            try (Connection conn = NameRegistrar.<DataSource>get(dataSourceName).getConnection()) {
                conn.setAutoCommit(true);
                String card = request.getString(2);
                Mac mac = Mac.getInstance("HmacSHA256");
                mac.init(key);
                String lookup = HexFormat.of().formatHex(mac.doFinal(card.getBytes(StandardCharsets.US_ASCII)));
                insertRow(conn, lookup, card.substring(card.length() - 4), Long.parseLong(request.getString(4)),
                    response != null ? response.getString(39) : null, outcome);
            } catch (Exception e) {
                ctx.log(e);
            }
        }
    }

    // ---- given: one database transaction per TransactionManager transaction ----

    /** Given: borrows a connection, turns auto-commit off, and leaves it under DB and TX. */
    public static class Open implements TransactionParticipant, Configurable {
        private String dataSourceName;

        @Override
        public void setConfiguration(Configuration cfg) {
            dataSourceName = cfg.get("datasource", "ds.gift");
        }

        @Override
        public int prepare(long id, Serializable context) {
            Context ctx = (Context) context;
            try {
                DataSource ds = NameRegistrar.get(dataSourceName);
                Connection conn = ds.getConnection();
                conn.setAutoCommit(false);
                ctx.put(DB, conn);
                ctx.put(TX, conn);
                return PREPARED | NO_JOIN;
            } catch (Exception e) {
                ctx.log(e);
                try {
                    ctx.put(RESPONSE, reply(ctx.get(REQUEST), "96"));
                } catch (ISOException ignored) {
                    // nothing to answer with
                }
                return ABORTED | NO_JOIN;
            }
        }
    }

    /** Given: commits or rolls back Open's transaction, and always gives the connection back. */
    public static class Close implements AbortParticipant {
        @Override
        public int prepare(long id, Serializable context) {
            return ((Context) context).get(DB) != null ? PREPARED | READONLY : PREPARED | READONLY | NO_JOIN;
        }

        @Override
        public void commit(long id, Serializable context) {
            Context ctx = (Context) context;
            Connection conn = ctx.get(DB);
            try {
                conn.commit();
                ctx.remove(TX);
            } catch (Exception e) {
                ctx.log(e);
            } finally {
                close(ctx, conn);
            }
        }

        @Override
        public void abort(long id, Serializable context) {
            Context ctx = (Context) context;
            Connection conn = ctx.get(DB);
            try {
                conn.rollback();
            } catch (Exception e) {
                ctx.log(e);
            } finally {
                ctx.remove(TX);
                close(ctx, conn);
            }
        }

        private static void close(Context ctx, Connection conn) {
            try {
                conn.close();
            } catch (Exception e) {
                ctx.log(e);
            }
            ctx.remove(DB);
        }
    }
}

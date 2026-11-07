package com.example.practice;

import org.jpos.core.Configurable;
import org.jpos.core.Configuration;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.transaction.AbortParticipant;
import org.jpos.transaction.Context;
import org.jpos.transaction.ContextConstants;
import org.jpos.transaction.TransactionParticipant;
import org.jpos.util.NameRegistrar;

import javax.sql.DataSource;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Gift-card redemptions. A 0200 carries the card number in field 2 and the amount in field 4; the
 * card's balance is a row in the gift_cards table. The chain is Open, Redeem, the fraud check,
 * Close, SendResponse.
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
                return ABORTED | NO_JOIN;
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

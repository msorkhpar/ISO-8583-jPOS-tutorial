package com.example.practice;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOSource;
import org.jpos.q2.Q2;
import org.jpos.space.SpaceFactory;
import org.jpos.transaction.AbortParticipant;
import org.jpos.transaction.Context;
import org.jpos.transaction.ContextConstants;
import org.jpos.transaction.TransactionParticipant;
import org.jpos.util.NameRegistrar;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.sql.DataSource;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** Runs Record after SendResponse, behind the given Open, Redeem and Close, in a real TransactionManager. */
class RecordTest {

    @Test
    void anApprovedRedemptionIsRecorded() throws Exception {
        db.card("6011111111111117", 10000);
        assertEquals("00", Run.of(redemption("6011111111111117", "000000002550", "000071")).answer().getString(39));
        List<Object[]> rows = db.rows();
        assertEquals(1, rows.size(), "one row for one redemption");
        assertEquals("1117", rows.get(0)[1]);
        assertEquals(2550L, ((Number) rows.get(0)[2]).longValue());
        assertEquals("00", rows.get(0)[3]);
        assertEquals("COMMITTED", rows.get(0)[4]);
    }

    @Test
    void aDeclinedRedemptionIsRecordedToo() throws Exception {
        db.card("3530111333300000", 1000);
        assertEquals("51", Run.of(redemption("3530111333300000", "000000001500", "000072")).answer().getString(39));
        assertEquals("59", Run.of(redemption("3530111333300000", "000000000400", "000073"), "DECLINE").answer().getString(39));
        List<Object[]> rows = db.rows();
        assertEquals(2, rows.size(), "a declined redemption gets its row as well");
        assertEquals(List.of("51", "59"), List.of(rows.get(0)[3], rows.get(1)[3]));
        assertEquals(List.of("ABORTED", "ABORTED"), List.of(rows.get(0)[4], rows.get(1)[4]));
        assertEquals(1000, db.balance("3530111333300000"), "the declines took nothing");
    }

    @Test
    void theCardCanBeFoundOnlyWithTheKey() throws Exception {
        db.card("6011000990139424", 8000);
        Run.of(redemption("6011000990139424", "000000003000", "000074"));
        Run.of(redemption("6011000990139424", "000000001000", "000075"));
        List<Object[]> rows = db.rows();
        assertEquals(2, rows.size());
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(KEY);
        String expected = HexFormat.of().formatHex(mac.doFinal("6011000990139424".getBytes(StandardCharsets.US_ASCII)));
        assertEquals(expected, rows.get(0)[0], "support computes the lookup value with the server's key");
        assertEquals(rows.get(0)[0], rows.get(1)[0], "the same card gives the same lookup value");
    }

    @Test
    void everyConnectionGoesBack() throws Exception {
        db.card("3566002020360505", 50000);
        for (int i = 0; i < 6; i++)
            assertEquals("00", Run.of(redemption("3566002020360505", "000000001000", "00008" + i)).answer().getString(39),
                "redemption " + (i + 1) + " of 6");
        assertEquals(6, db.rows().size());
    }

    private static ISOMsg redemption(String card, String amount, String stan) throws Exception {
        ISOMsg m = new ISOMsg("0200");
        m.set(2, card);
        m.set(3, "000000");
        m.set(4, amount);
        m.set(11, stan);
        m.set(41, "29110042");
        return m;
    }

    // ---- test support: Q2 in this JVM, and a small database with transactions and row locks ----

    private static final String QUEUE = "txn.redemption-log";
    private static Q2 q2;
    private static FakeDb db;

    private static final SecretKey KEY = new SecretKeySpec(
        HexFormat.of().parseHex("8f1d2c3b4a5968778695a4b3c2d1e0f00112233445566778899aabbccddeeff0"), "HmacSHA256");

    @BeforeAll
    static void startQ2() throws Exception {
        NameRegistrar.register("practice-card-key", KEY);
        Path deploy = Files.createTempDirectory("deploy");
        Files.writeString(deploy.resolve("30_txnmgr.xml"),
            "<txnmgr name='practice-redemption-log' class='org.jpos.transaction.TransactionManager'>\n"
          + "  <property name='queue' value='" + QUEUE + "'/>\n"
          + "  <property name='sessions' value='2'/>\n"
          + "  <property name='debug' value='false'/>\n"
          + "  <participant class='com.example.practice.GiftCards$Open'>\n"
          + "    <property name='datasource' value='ds.gift'/>\n"
          + "  </participant>\n"
          + "  <participant class='com.example.practice.GiftCards$Redeem'/>\n"
          + "  <participant class='com.example.practice.RecordTest$Fraud'/>\n"
          + "  <participant class='com.example.practice.GiftCards$Close'/>\n"
          + "  <participant class='org.jpos.transaction.participant.SendResponse'/>\n"
          + "  <participant class='com.example.practice.GiftCards$Record'>\n"
          + "    <property name='datasource' value='ds.gift'/>\n"
          + "    <property name='key' value='practice-card-key'/>\n"
          + "  </participant>\n"
          + "  <participant class='com.example.practice.RecordTest$Finished'/>\n"
          + "</txnmgr>\n");
        q2 = new Q2(deploy.toString());
        q2.start();
        assertNotNull(NameRegistrar.get("practice-redemption-log", 30000), "the TransactionManager did not start");
    }

    @AfterAll
    static void stopQ2() {
        q2.shutdown(true);
        NameRegistrar.unregister("practice-card-key");
    }

    @BeforeEach
    void freshDatabase() {
        NameRegistrar.unregister("ds.gift");
        db = new FakeDb(3);
        NameRegistrar.register("ds.gift", db.dataSource());
    }

    /** One transaction, run to its end. */
    static final class Run {
        private static final AtomicInteger RUNS = new AtomicInteger();
        final List<ISOMsg> sent = new CopyOnWriteArrayList<>();
        final String id = "redemption-log.run." + RUNS.incrementAndGet();

        ISOMsg answer() {
            assertEquals(1, sent.size(), "the terminal gets one answer");
            return sent.get(0);
        }

        static Run of(ISOMsg request, String... flags) {
            Run run = new Run();
            Context ctx = new Context();
            ctx.put(ContextConstants.REQUEST.toString(), request);
            ctx.put(ContextConstants.SOURCE.toString(), new ISOSource() {
                @Override public void send(ISOMsg m) { run.sent.add(m); }
                @Override public boolean isConnected() { return true; }
            });
            ctx.put("RUN", run.id);
            for (String f : flags)
                ctx.put(f, Boolean.TRUE);
            SpaceFactory.getSpace().out(QUEUE, ctx);
            assertNotNull(SpaceFactory.getSpace().in(run.id, 20000), "the transaction did not finish");
            return run;
        }
    }

    /** Stands in for the fraud check: declines with 59 when the test says so. */
    public static class Fraud implements TransactionParticipant {
        @Override
        public int prepare(long id, Serializable context) {
            Context ctx = (Context) context;
            if (ctx.get("DECLINE") == null)
                return PREPARED | READONLY | NO_JOIN;
            try {
                ctx.put(ContextConstants.RESPONSE.toString(),
                    GiftCards.reply(ctx.get(ContextConstants.REQUEST.toString()), "59"));
            } catch (Exception e) {
                ctx.log(e);
            }
            return ABORTED | NO_JOIN;
        }
    }

    /** Last on both paths: tells the test the transaction is over. */
    public static class Finished implements AbortParticipant {
        @Override
        public int prepare(long id, Serializable context) {
            return PREPARED | READONLY;
        }

        @Override
        public void commit(long id, Serializable context) {
            SpaceFactory.getSpace().out(((Context) context).getString("RUN"), Boolean.TRUE);
        }

        @Override
        public void abort(long id, Serializable context) {
            SpaceFactory.getSpace().out(((Context) context).getString("RUN"), Boolean.FALSE);
        }
    }

    /**
     * The gift_cards and redemption_log tables behind a DataSource, with at most maxOpen connections
     * open at once. Connections start with auto-commit on. With it off,
     * changes stay private to the connection until commit, and an updated row stays locked until
     * commit, rollback or close; another update of that row waits (up to 5 s).
     */
    static final class FakeDb {
        final Map<String, Long> balances = new HashMap<>();
        final Set<String> broken = new HashSet<>();
        private final List<Object[]> rows = new ArrayList<>();
        private final Map<String, Conn> locks = new HashMap<>();
        private final int maxOpen;
        private int open;

        FakeDb(int maxOpen) {
            this.maxOpen = maxOpen;
        }

        synchronized void card(String card, long balance) {
            balances.put(card, balance);
        }

        /** The committed rows of redemption_log: card_lookup, last4, amount, rc, outcome. */
        synchronized List<Object[]> rows() {
            return new ArrayList<>(rows);
        }

        synchronized long balance(String card) {
            return balances.get(card);
        }

        DataSource dataSource() {
            return (DataSource) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { DataSource.class },
                (p, m, a) -> {
                    if (m.getName().equals("getConnection"))
                        return connect();
                    return basics(p, m.getName(), a, "DataSource");
                });
        }

        private synchronized Connection connect() throws SQLException {
            if (open >= maxOpen)
                throw new SQLException("no connection available");
            open++;
            return new Conn().proxy();
        }

        static Object basics(Object p, String name, Object[] a, String what) {
            switch (name) {
                case "toString": return what;
                case "hashCode": return System.identityHashCode(p);
                case "equals": return p == a[0];
                default: throw new UnsupportedOperationException(what + "." + name);
            }
        }

        final class Conn {
            boolean autoCommit = true, closed;
            final Map<String, Long> pending = new HashMap<>();
            final List<Object[]> pendingRows = new ArrayList<>();

            Connection proxy() {
                InvocationHandler h = (p, m, a) -> {
                    synchronized (FakeDb.this) {
                        switch (m.getName()) {
                            case "setAutoCommit": autoCommit = (Boolean) a[0]; return null;
                            case "getAutoCommit": return autoCommit;
                            case "commit": check(); balances.putAll(pending); pending.clear(); rows.addAll(pendingRows); pendingRows.clear(); release(); return null;
                            case "rollback": check(); pending.clear(); pendingRows.clear(); release(); return null;
                            case "close":
                                if (!closed) { closed = true; pending.clear(); pendingRows.clear(); release(); open--; }
                                return null;
                            case "isClosed": return closed;
                            case "prepareStatement": check(); return statement((String) a[0]);
                            default: return basics(p, m.getName(), a, "Connection");
                        }
                    }
                };
                return (Connection) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { Connection.class }, h);
            }

            private void check() throws SQLException {
                if (closed)
                    throw new SQLException("connection is closed");
            }

            private void release() {
                locks.values().removeIf(c -> c == this);
                FakeDb.this.notifyAll();
            }

            private long current(String card) {
                Long v = pending.containsKey(card) ? pending.get(card) : balances.get(card);
                return v == null ? -1 : v;
            }

            private PreparedStatement statement(String sql) {
                Map<Integer, Object> params = new HashMap<>();
                InvocationHandler h = (p, m, a) -> {
                    synchronized (FakeDb.this) {
                        String name = m.getName();
                        if (name.startsWith("set") && a != null && a.length == 2 && a[0] instanceof Integer) {
                            params.put((Integer) a[0], a[1]);
                            return null;
                        }
                        switch (name) {
                            case "executeUpdate": check(); return update(sql, params);
                            case "executeQuery": check(); return query(sql, params);
                            case "close": return null;
                            default: return basics(p, name, a, "PreparedStatement");
                        }
                    }
                };
                return (PreparedStatement) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { PreparedStatement.class }, h);
            }

            private int update(String sql, Map<Integer, Object> params) throws SQLException {
                if (sql.startsWith("INSERT INTO redemption_log")) {
                    Object[] row = { params.get(1), params.get(2), params.get(3), params.get(4), params.get(5) };
                    if (autoCommit)
                        rows.add(row);
                    else
                        pendingRows.add(row);
                    return 1;
                }
                if (!sql.startsWith("UPDATE gift_cards SET balance = balance - ?"))
                    throw new SQLException("unexpected statement: " + sql);
                long amount = ((Number) params.get(1)).longValue();
                String card = (String) params.get(2);
                if (broken.contains(card))
                    throw new SQLException("could not serialize access due to concurrent update");
                long deadline = System.currentTimeMillis() + 5000;
                while (locks.containsKey(card) && locks.get(card) != this) {
                    long left = deadline - System.currentTimeMillis();
                    if (left <= 0)
                        throw new SQLException("lock timeout on gift card row");
                    try {
                        FakeDb.this.wait(left);
                    } catch (InterruptedException e) {
                        throw new SQLException(e);
                    }
                }
                long now = current(card);
                if (now < amount)
                    return 0;
                if (autoCommit) {
                    balances.put(card, now - amount);
                } else {
                    pending.put(card, now - amount);
                    locks.put(card, this);
                }
                return 1;
            }

            private ResultSet query(String sql, Map<Integer, Object> params) throws SQLException {
                if (!sql.startsWith("SELECT balance FROM gift_cards"))
                    throw new SQLException("unexpected query: " + sql);
                long v = current((String) params.get(1));
                boolean[] before = { true };
                InvocationHandler h = (p, m, a) -> {
                    switch (m.getName()) {
                        case "next": { boolean r = before[0] && v >= 0; before[0] = false; return r; }
                        case "getLong": return v;
                        case "close": return null;
                        default: return basics(p, m.getName(), a, "ResultSet");
                    }
                };
                return (ResultSet) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { ResultSet.class }, h);
            }
        }
    }
}

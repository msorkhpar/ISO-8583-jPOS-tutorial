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

import javax.sql.DataSource;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** Runs Redeem between the given Open and Close, in a real TransactionManager deployed by Q2. */
class GiftCardsTest {

    @Test
    void aRedemptionTheBalanceCoversIsApproved() throws Exception {
        db.card("6011111111111117", 10000);
        Run run = Run.of(redemption("6011111111111117", "000000002550", "000071"));
        assertEquals("00", run.answer().getString(39));
        assertEquals(7450, db.balance("6011111111111117"));
    }

    @Test
    void aRedemptionOverTheBalanceIsDeclined() throws Exception {
        db.card("3530111333300000", 1000);
        Run run = Run.of(redemption("3530111333300000", "000000001500", "000072"));
        assertEquals("51", run.answer().getString(39));
        assertEquals(1000, db.balance("3530111333300000"));
    }

    @Test
    void aRedemptionTheFraudCheckDeclinesLeavesTheBalance() throws Exception {
        db.card("6011000990139424", 8000);
        Run declined = Run.of(redemption("6011000990139424", "000000003000", "000073"), "DECLINE");
        assertEquals("59", declined.answer().getString(39), "the fraud check declines this one");
        assertEquals(8000, db.balance("6011000990139424"), "a declined redemption takes nothing");
        Run whole = Run.of(redemption("6011000990139424", "000000008000", "000074"));
        assertEquals("00", whole.answer().getString(39), "the whole balance is still there");
    }

    @Test
    void twoRedemptionsAtOnceSpendTheBalanceOnce() throws Exception {
        db.card("3566002020360505", 5000);
        Hold.both = new CyclicBarrier(2);
        ISOMsg first = redemption("3566002020360505", "000000003000", "000081");
        ISOMsg second = redemption("3566002020360505", "000000003000", "000082");
        CompletableFuture<Run> a = CompletableFuture.supplyAsync(() -> Run.of(first, "HOLD"));
        CompletableFuture<Run> b = CompletableFuture.supplyAsync(() -> Run.of(second, "HOLD"));
        List<String> codes = new ArrayList<>(List.of(
            a.get(30, TimeUnit.SECONDS).answer().getString(39),
            b.get(30, TimeUnit.SECONDS).answer().getString(39)));
        codes.sort(null);
        assertEquals(List.of("00", "51"), codes, "50.00 pays for one redemption of 30.00, not two");
        assertEquals(2000, db.balance("3566002020360505"));
    }

    @Test
    void aDatabaseErrorIsAnswered96() throws Exception {
        db.card("6011000400000000", 9000);
        db.broken.add("6011000400000000");
        Run run = Run.of(redemption("6011000400000000", "000000001000", "000091"));
        assertEquals("96", run.answer().getString(39), "the terminal gets an answer it can act on");
        assertEquals(9000, db.balance("6011000400000000"));
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

    private static final String QUEUE = "txn.giftcards";
    private static Q2 q2;
    private static FakeDb db;

    @BeforeAll
    static void startQ2() throws Exception {
        Path deploy = Files.createTempDirectory("deploy");
        Files.writeString(deploy.resolve("30_txnmgr.xml"),
            "<txnmgr name='practice-giftcards' class='org.jpos.transaction.TransactionManager'>\n"
          + "  <property name='queue' value='" + QUEUE + "'/>\n"
          + "  <property name='sessions' value='2'/>\n"
          + "  <property name='debug' value='false'/>\n"
          + "  <participant class='com.example.practice.GiftCards$Open'>\n"
          + "    <property name='datasource' value='ds.gift'/>\n"
          + "  </participant>\n"
          + "  <participant class='com.example.practice.GiftCards$Redeem'/>\n"
          + "  <participant class='com.example.practice.GiftCardsTest$Fraud'/>\n"
          + "  <participant class='com.example.practice.GiftCardsTest$Hold'/>\n"
          + "  <participant class='com.example.practice.GiftCards$Close'/>\n"
          + "  <participant class='org.jpos.transaction.participant.SendResponse'/>\n"
          + "  <participant class='com.example.practice.GiftCardsTest$Finished'/>\n"
          + "</txnmgr>\n");
        q2 = new Q2(deploy.toString());
        q2.start();
        assertNotNull(NameRegistrar.get("practice-giftcards", 30000), "the TransactionManager did not start");
    }

    @AfterAll
    static void stopQ2() {
        q2.shutdown(true);
    }

    @BeforeEach
    void freshDatabase() {
        NameRegistrar.unregister("ds.gift");
        db = new FakeDb(8);
        NameRegistrar.register("ds.gift", db.dataSource());
    }

    /** One transaction, run to its end. */
    static final class Run {
        private static final AtomicInteger RUNS = new AtomicInteger();
        final List<ISOMsg> sent = new CopyOnWriteArrayList<>();
        final String id = "giftcards.run." + RUNS.incrementAndGet();

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

    /** Holds two flagged transactions here until both have arrived, or two seconds have passed. */
    public static class Hold implements TransactionParticipant {
        static volatile CyclicBarrier both = new CyclicBarrier(2);

        @Override
        public int prepare(long id, Serializable context) {
            if (((Context) context).get("HOLD") != null) {
                try {
                    both.await(2, TimeUnit.SECONDS);
                } catch (Exception e) {
                    // the other one is not coming
                }
            }
            return PREPARED | READONLY | NO_JOIN;
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
     * The gift_cards table behind a DataSource. Connections start with auto-commit on. With it off,
     * changes stay private to the connection until commit, and an updated row stays locked until
     * commit, rollback or close; another update of that row waits (up to 5 s).
     */
    static final class FakeDb {
        final Map<String, Long> balances = new HashMap<>();
        final Set<String> broken = new HashSet<>();
        private final Map<String, Conn> locks = new HashMap<>();
        private final int maxOpen;
        private int open;

        FakeDb(int maxOpen) {
            this.maxOpen = maxOpen;
        }

        synchronized void card(String card, long balance) {
            balances.put(card, balance);
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

            Connection proxy() {
                InvocationHandler h = (p, m, a) -> {
                    synchronized (FakeDb.this) {
                        switch (m.getName()) {
                            case "setAutoCommit": autoCommit = (Boolean) a[0]; return null;
                            case "getAutoCommit": return autoCommit;
                            case "commit": check(); balances.putAll(pending); pending.clear(); release(); return null;
                            case "rollback": check(); pending.clear(); release(); return null;
                            case "close":
                                if (!closed) { closed = true; pending.clear(); release(); open--; }
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

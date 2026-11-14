import com.example.practice.GiftCards;
import org.jpos.iso.ISOMsg;
import org.jpos.transaction.Context;
import org.jpos.transaction.ContextConstants;
import org.jpos.transaction.TransactionConstants;

import java.lang.reflect.Proxy;
import javax.crypto.spec.SecretKeySpec;
import javax.sql.DataSource;
import org.jpos.core.SimpleConfiguration;
import org.jpos.util.NameRegistrar;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Run executes this file. Change the calls in main and see what prints. Submit grades. */
public class TryIt {
    /** Held here, so the JDK's own exit notice stays off: a logger nothing holds can be dropped. */
    private static final Logger JDK_EXIT = Logger.getLogger("java.lang.Runtime");

    static final Map<String, Long> BALANCES = new HashMap<>();
    static final List<String> ROWS = new ArrayList<>();
    static GiftCards.Record record;

    public static void main(String[] args) throws Exception {
        // Turn the logger up, so the log lines of your code show under the printed lines.
        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s %5$s%n");
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        Logger root = Logger.getLogger("");
        root.setLevel(Level.ALL);
        root.addHandler(handler);
        JDK_EXIT.setLevel(Level.OFF);

        // A stand-in for the collaborator, like the one the tests use for the first main case.
        try {
            NameRegistrar.register("tryit.gift", (DataSource) Proxy.newProxyInstance(TryIt.class.getClassLoader(),
                new Class<?>[] { DataSource.class }, (p, m, a) -> m.getName().equals("getConnection") ? connect() : null));
            NameRegistrar.register("tryit.card-key", new SecretKeySpec(
                HexFormat.of().parseHex("3c71a09e5f2b8d4416e0c7a95b3f2d8e71c64a0b9d2e5f8317a6c0e4b92d7f15"), "HmacSHA256"));
            SimpleConfiguration cfg = new SimpleConfiguration();
            cfg.put("datasource", "tryit.gift");
            cfg.put("key", "tryit.card-key");
            record = new GiftCards.Record();
            record.setConfiguration(cfg);
            BALANCES.put("6037990000004411", 6000L);
            BALANCES.put("6037990000008826", 2500L);
            System.out.println("redeem 45.00: " + redeem("6037990000004411", "000000004500", "000321", false));
            System.out.println("redeem 30.00: " + redeem("6037990000004411", "000000003000", "000322", false));
            System.out.println("redeem 10.00, fraud check declines: " + redeem("6037990000008826", "000000001000", "000323", true));
            System.out.println("redeem 20.00: " + redeem("6037990000008826", "000000002000", "000324", false));
            ROWS.forEach(row -> System.out.println("row: " + row));
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        } finally {
            NameRegistrar.unregister("tryit.gift");
            NameRegistrar.unregister("tryit.card-key");
        }
    }

    /** Open's connection, Redeem, the fraud check's verdict, then Close and Record on the path the chain takes. */
    private static String redeem(String card, String amount, String stan, boolean fraud) throws Exception {
        ISOMsg m = new ISOMsg("0200");
        m.set(2, card);
        m.set(3, "000000");
        m.set(4, amount);
        m.set(11, stan);
        m.set(41, "60400117");
        Context ctx = new Context();
        Connection conn = connect();
        conn.setAutoCommit(false);
        ctx.put(ContextConstants.REQUEST.toString(), m);
        ctx.put(ContextConstants.DB.toString(), conn);
        ctx.put(ContextConstants.TX.toString(), conn);
        boolean prepared = (new GiftCards.Redeem().prepare(1L, ctx) & TransactionConstants.PREPARED) != 0;
        if (prepared && fraud) {
            ISOMsg declined = (ISOMsg) m.clone();
            declined.setResponseMTI();
            declined.set(39, "59");
            ctx.put(ContextConstants.RESPONSE.toString(), declined);
        }
        record.prepare(1L, ctx);
        if (prepared && !fraud) {
            new GiftCards.Close().commit(1L, ctx);
            record.commit(1L, ctx);
        } else {
            new GiftCards.Close().abort(1L, ctx);
            record.abort(1L, ctx);
        }
        ISOMsg r = ctx.get(ContextConstants.RESPONSE.toString());
        return "field 39=" + (r == null ? null : r.getString(39)) + ", balance now " + BALANCES.get(card);
    }

    /** A connection to a gift_cards table kept in BALANCES and a redemption_log kept in ROWS: updates stay private until commit. */
    private static Connection connect() {
        Map<String, Long> pending = new HashMap<>();
        boolean[] auto = { true };
        return (Connection) Proxy.newProxyInstance(TryIt.class.getClassLoader(), new Class<?>[] { Connection.class },
            (p, m, a) -> switch (m.getName()) {
                case "setAutoCommit" -> { auto[0] = (Boolean) a[0]; yield null; }
                case "commit" -> { BALANCES.putAll(pending); pending.clear(); yield null; }
                case "rollback", "close" -> { pending.clear(); yield null; }
                case "prepareStatement" -> statement(pending, auto);
                case "hashCode" -> System.identityHashCode(p);
                case "equals" -> p == a[0];
                case "isClosed", "getAutoCommit" -> false;
                default -> "fake gift_cards connection";
            });
    }

    private static PreparedStatement statement(Map<String, Long> pending, boolean[] auto) {
        Map<Integer, Object> params = new HashMap<>();
        return (PreparedStatement) Proxy.newProxyInstance(TryIt.class.getClassLoader(), new Class<?>[] { PreparedStatement.class },
            (p, m, a) -> {
                if (m.getName().startsWith("set")) params.put((Integer) a[0], a[1]);
                if (!m.getName().equals("executeUpdate")) return null;
                if (params.size() == 5) {
                    ROWS.add(new java.util.TreeMap<>(params).values().toString());
                    return 1;
                }
                String card = (String) params.get(2);
                long amount = (Long) params.get(1), now = pending.getOrDefault(card, BALANCES.getOrDefault(card, 0L));
                if (now < amount) return 0;
                (auto[0] ? BALANCES : pending).put(card, now - amount);
                return 1;
            });
    }
}

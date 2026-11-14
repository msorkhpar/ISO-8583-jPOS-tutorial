import com.example.practice.CardExpiry;
import org.jpos.core.SimpleConfiguration;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;
import org.jpos.transaction.Context;
import org.jpos.transaction.ContextConstants;
import org.jpos.transaction.TransactionConstants;
import org.jpos.util.ProtectedLogListener;
import org.jpos.util.SimpleLogListener;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Run executes this file. Change the calls in main and see what prints. Submit grades. */
public class TryIt {
    /** Held here, so the JDK's own exit notice stays off: a logger nothing holds can be dropped. */
    private static final Logger JDK_EXIT = Logger.getLogger("java.lang.Runtime");

    public static void main(String[] args) throws Exception {
        // Turn the logger up, so the log lines of your code show under the printed lines.
        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s %5$s%n");
        // Only this handler prints, and it leaves out the JDK's own detail.
        Logger root = Logger.getLogger("");
        for (java.util.logging.Handler before : root.getHandlers()) {
            root.removeHandler(before);
        }
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        handler.setFilter(entry -> !String.valueOf(entry.getLoggerName()).matches("(java|javax|jdk|sun|com\\.sun)\\..*"));
        root.setLevel(Level.ALL);
        root.addHandler(handler);
        JDK_EXIT.setLevel(Level.OFF);

        // A stand-in for the collaborator, like the one the tests use for the first main case.
        try {
            ProtectedLogListener protect = new ProtectedLogListener();
            SimpleConfiguration p = new SimpleConfiguration();
            p.put("protect", "2 35 45");
            p.put("wipe", "52 55");
            protect.setConfiguration(p);
            org.jpos.util.Logger q2 = new org.jpos.util.Logger();
            q2.setName("tryit-q2");
            q2.addListener(protect);
            q2.addListener(new SimpleLogListener(System.out));
            CardExpiry expiry = new CardExpiry();
            expiry.setLogger(q2, "card-expiry");
            SimpleConfiguration cfg = new SimpleConfiguration();
            cfg.put("this-month", "2027-02");
            expiry.setConfiguration(cfg);
            System.out.println("expiry 2701: " + run(expiry, 4402L, "000732", "2701"));
            System.out.println("expiry 2713: " + run(expiry, 4403L, "000733", "2713"));
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }

    private static String run(CardExpiry expiry, long id, String stan, String field14) throws Exception {
        ISOMsg m = new ISOMsg("0200");
        m.set(2, "4917610000000000");
        m.set(3, "000000");
        m.set(4, "000000002210");
        m.set(11, stan);
        m.set(14, field14);
        m.set(35, "4917610000000000=27121010000000000000");
        m.set(41, "60400108");
        m.set(52, ISOUtil.hex2byte("19A4C7720E5B3F80"));
        Context ctx = new Context();
        ctx.put(ContextConstants.REQUEST.toString(), m);
        int vote = expiry.prepare(id, ctx);
        ISOMsg r = ctx.get(ContextConstants.RESPONSE.toString());
        return ((vote & TransactionConstants.PREPARED) != 0 ? "prepared" : "aborted")
                + (r == null ? "" : ", field 39=" + r.getString(39));
    }
}

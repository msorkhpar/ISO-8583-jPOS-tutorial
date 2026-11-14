import com.example.practice.VelocityLimit;
import org.jpos.core.SimpleConfiguration;
import org.jpos.iso.ISOMsg;
import org.jpos.transaction.Context;
import org.jpos.transaction.ContextConstants;
import org.jpos.transaction.TransactionConstants;
import org.jpos.util.NameRegistrar;

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
            VelocityLimit.DailyCounts counts = new VelocityLimit.DailyCounts(3);
            NameRegistrar.register("tryit-counts", counts);
            VelocityLimit limit = new VelocityLimit();
            SimpleConfiguration cfg = new SimpleConfiguration();
            cfg.put("counts", "tryit-counts");
            limit.setConfiguration(cfg);
            String card = "4111111111111111";
            System.out.println("purchase 1 declined by authorizer: " + run(limit, card, "000401", true));
            for (int i = 2; i <= 5; i++)
                System.out.println("purchase " + i + ": " + run(limit, card, "00040" + i, false));
            System.out.println("count for the card: " + counts.count(card));
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        } finally {
            NameRegistrar.unregister("tryit-counts");
        }
    }

    private static String run(VelocityLimit limit, String card, String stan, boolean declined) throws Exception {
        ISOMsg m = new ISOMsg("0200");
        m.set(2, card);
        m.set(3, "000000");
        m.set(4, "000000001275");
        m.set(11, stan);
        m.set(41, "60400017");
        Context ctx = new Context();
        ctx.put(ContextConstants.REQUEST.toString(), m);
        int result = limit.prepare(1L, ctx);
        if ((result & TransactionConstants.PREPARED) == 0)
            return "refused, field 39=" + ((ISOMsg) ctx.get(ContextConstants.RESPONSE.toString())).getString(39);
        if (declined) {
            limit.abort(1L, ctx);
            return "prepared, then aborted";
        }
        return "prepared, goes on to the authorizer";
    }
}

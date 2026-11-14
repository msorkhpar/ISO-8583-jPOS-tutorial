import com.example.practice.ReversalRecords;
import org.jpos.core.SimpleConfiguration;
import org.jpos.iso.ISOMsg;
import org.jpos.space.Space;
import org.jpos.space.SpaceFactory;
import org.jpos.transaction.Context;
import org.jpos.transaction.ContextConstants;
import org.jpos.transaction.TransactionConstants;

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
            ReversalRecords.Outbound outbound = new ReversalRecords.Outbound();
            SimpleConfiguration cfg = new SimpleConfiguration();
            cfg.put("acquirer-id", "55500011122");
            outbound.setConfiguration(cfg);
            ISOMsg a = purchase("FUEL0031", "000219", "631114000219");
            ISOMsg b = purchase("FUEL0032", "000219", "631114000219");
            System.out.println("FUEL0031 purchase sent upstream as: " + send(outbound, a));
            System.out.println("FUEL0032 purchase sent upstream as: " + send(outbound, b));
            System.out.println("FUEL0032 reversal answered: " + reverse(b, "0420"));
            System.out.println("FUEL0032 repeat answered: " + reverse(b, "0421"));
            System.out.println("FUEL0033 reversal of nothing answered: " + reverse(purchase("FUEL0033", "000219", "631114000219"), "0420"));
            Space<String, Object> sp = SpaceFactory.getSpace("tspace:switch-records");
            Object queued;
            while ((queued = sp.inp("reversal.fleet")) != null)
                System.out.println("queued for fleet: " + ((ISOMsg) queued).getString(0) + " 90=" + ((ISOMsg) queued).getString(90));
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }

    private static String send(ReversalRecords.Outbound outbound, ISOMsg purchase) {
        Context ctx = new Context();
        ctx.put(ContextConstants.REQUEST.toString(), purchase);
        ctx.put(ContextConstants.DESTINATION.toString(), "fleet");
        if ((outbound.prepare(1L, ctx) & TransactionConstants.PREPARED) == 0)
            return "Outbound failed, " + ctx.getResult().failure().getMessage();
        ISOMsg out = ctx.get("OUTBOUND");
        return "STAN " + out.getString(11) + ", field 7 " + out.getString(7);
    }

    private static String reverse(ISOMsg purchase, String mti) throws Exception {
        ISOMsg r = (ISOMsg) purchase.clone(2, 3, 4, 37, 41, 49);
        r.setMTI(mti);
        r.set(7, "1114142530");
        r.set(11, "000220");
        Context ctx = new Context();
        ctx.put(ContextConstants.REQUEST.toString(), r);
        new ReversalRecords.ReverseFromTerminal().prepare(1L, ctx);
        ISOMsg answer = ctx.get(ContextConstants.RESPONSE.toString());
        return answer == null ? "no answer" : answer.getMTI() + " field 39=" + answer.getString(39);
    }

    private static ISOMsg purchase(String terminal, String stan, String rrn) throws Exception {
        ISOMsg m = new ISOMsg("0200");
        m.set(2, "7083000000098765");
        m.set(3, "000000");
        m.set(4, "000000005525");
        m.set(7, "1114142200");
        m.set(11, stan);
        m.set(37, rrn);
        m.set(41, terminal);
        m.set(49, "978");
        return m;
    }
}

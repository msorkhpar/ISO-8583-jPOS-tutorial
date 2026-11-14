import com.example.practice.AfterNetwork;
import org.jpos.core.SimpleConfiguration;
import org.jpos.iso.ISOMsg;
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
            AfterNetwork after = new AfterNetwork();
            SimpleConfiguration cfg = new SimpleConfiguration();
            cfg.put("saf", "tryit.saf");
            after.setConfiguration(cfg);
            System.out.println("network approved: " + run(after, "000731", "00"));
            System.out.println("network declined: " + run(after, "000732", "51"));
            System.out.println("network silent: " + run(after, "000733", null));
            ISOMsg reversal;
            while ((reversal = (ISOMsg) SpaceFactory.getSpace().inp("tryit.saf")) != null)
                System.out.println("on the saf queue: " + reversal.getMTI() + " field 90=" + reversal.getString(90));
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }

    private static String run(AfterNetwork after, String stan, String networkAnswer) throws Exception {
        ISOMsg m = new ISOMsg("0200");
        m.set(2, "5105105105105100");
        m.set(3, "000000");
        m.set(4, "000000005210");
        m.set(7, "1114093015");
        m.set(11, stan);
        m.set(41, "60400045");
        m.set(49, "978");
        Context ctx = new Context();
        ctx.put(ContextConstants.REQUEST.toString(), m);
        if (networkAnswer != null) {
            ISOMsg r = (ISOMsg) m.clone();
            r.setResponseMTI();
            r.set(39, networkAnswer);
            ctx.put(ContextConstants.RESPONSE.toString(), r);
        }
        int result = after.prepare(1L, ctx);
        return (result & TransactionConstants.PREPARED) != 0 ? "prepared" : "aborted";
    }
}

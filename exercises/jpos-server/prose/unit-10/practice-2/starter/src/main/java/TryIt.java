import com.example.practice.ReleaseHold;
import org.jpos.core.SimpleConfiguration;
import org.jpos.iso.ISOMsg;
import org.jpos.transaction.Context;
import org.jpos.transaction.ContextConstants;
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
            ReleaseHold.HoldTable holds = new ReleaseHold.HoldTable();
            NameRegistrar.register("tryit.holds", holds);
            SimpleConfiguration cfg = new SimpleConfiguration();
            cfg.put("holds", "tryit.holds");
            ReleaseHold release = new ReleaseHold();
            release.setConfiguration(cfg);
            holds.open("ACC-7310", 60000);
            holds.place("HTL00215", "008841", "1113194500", "ACC-7310", 32000);
            System.out.println("available with the hold: " + holds.available("ACC-7310"));
            System.out.println("0400 from another hotel: " + send(release, "0400", "HTL00299", "008841"));
            System.out.println("available: " + holds.available("ACC-7310"));
            System.out.println("0400 from the hotel: " + send(release, "0400", "HTL00215", "008841"));
            System.out.println("0401 repeat: " + send(release, "0401", "HTL00215", "008841"));
            System.out.println("available: " + holds.available("ACC-7310"));
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        } finally {
            NameRegistrar.unregister("tryit.holds");
        }
    }

    private static String send(ReleaseHold release, String mti, String terminal, String originalStan) throws Exception {
        ISOMsg m = new ISOMsg(mti);
        m.set(2, "5425233430109903");
        m.set(3, "000000");
        m.set(4, "000000032000");
        m.set(7, "1114081000");
        m.set(11, "008850");
        m.set(41, terminal);
        m.set(49, "978");
        m.set(90, "0100" + originalStan + "1113194500" + "00000000000" + "00000000000");
        Context ctx = new Context();
        ctx.put(ContextConstants.REQUEST.toString(), m);
        release.prepare(1L, ctx);
        ISOMsg r = ctx.get(ContextConstants.RESPONSE.toString());
        return r.getMTI() + " field 39=" + r.getString(39);
    }
}

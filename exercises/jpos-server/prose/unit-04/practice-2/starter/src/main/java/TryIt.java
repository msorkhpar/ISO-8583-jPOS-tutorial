import com.example.practice.ScoreFraud;
import org.jpos.core.SimpleConfiguration;
import org.jpos.iso.ISOMsg;
import org.jpos.transaction.Context;
import org.jpos.transaction.ContextConstants;
import org.jpos.transaction.TransactionConstants;
import org.jpos.util.NameRegistrar;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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
        ExecutorService service = Executors.newSingleThreadExecutor();
        try {
            ScoreFraud.Scorer scorer = (request, listener, handBack) -> service.submit(() -> {
                String stan = request.getString(11);
                if (stan.endsWith("3")) listener.unavailable(handBack);
                else listener.scored(stan.endsWith("1") ? 815 : 175, handBack);
            });
            NameRegistrar.register("tryit-scorer", scorer);
            ScoreFraud score = new ScoreFraud();
            SimpleConfiguration cfg = new SimpleConfiguration();
            cfg.put("scorer", "tryit-scorer");
            score.setConfiguration(cfg);
            Context[] contexts = new Context[3];
            for (int i = 0; i < 3; i++) {
                contexts[i] = context("00072" + (i + 1));
                int result = score.prepare(i, contexts[i]);
                System.out.println("purchase " + (i + 1) + " paused: " + ((result & TransactionConstants.PAUSE) != 0));
            }
            service.shutdown();
            service.awaitTermination(5, TimeUnit.SECONDS);
            for (int i = 0; i < 3; i++)
                System.out.println("purchase " + (i + 1) + " score: " + contexts[i].get("SCORE"));
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        } finally {
            service.shutdownNow();
            NameRegistrar.unregister("tryit-scorer");
        }
    }

    private static Context context(String stan) throws Exception {
        ISOMsg m = new ISOMsg("0200");
        m.set(2, "5454545454545454");
        m.set(3, "000000");
        m.set(4, "000000004260");
        m.set(11, stan);
        m.set(41, "60400022");
        Context ctx = new Context();
        ctx.put(ContextConstants.REQUEST.toString(), m);
        return ctx;
    }
}

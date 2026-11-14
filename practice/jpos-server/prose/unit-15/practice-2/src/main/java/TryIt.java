import com.example.practice.QuoteRate;
import org.jpos.core.SimpleConfiguration;
import org.jpos.iso.ISOMsg;
import org.jpos.transaction.Context;
import org.jpos.transaction.ContextConstants;
import org.jpos.util.NameRegistrar;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        try {
            NameRegistrar.register("tryit-fx", (QuoteRate.FxService) (currency, amount, listener) -> {
                if (amount == 2100) listener.quoted(1172300);
                if (amount == 2300) {
                    service.schedule(() -> listener.failed(new IllegalStateException("rate feed down")), 50, TimeUnit.MILLISECONDS);
                    service.schedule(() -> listener.quoted(1169900), 500, TimeUnit.MILLISECONDS);
                }
            });
            SimpleConfiguration cfg = new SimpleConfiguration();
            cfg.put("service", "tryit-fx");
            cfg.put("timeout", "300");
            QuoteRate quote = new QuoteRate();
            quote.setConfiguration(cfg);
            Watched instant = start(quote, "000000002100");
            Watched silent = start(quote, "000000002200");
            Watched failedThenLate = start(quote, "000000002300");
            Thread.sleep(700);
            System.out.println("instant quote: " + instant);
            System.out.println("no answer: " + silent);
            System.out.println("failure, then a late quote: " + failedThenLate);
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        } finally {
            service.shutdownNow();
            NameRegistrar.unregister("tryit-fx");
        }
    }

    /** A Context that counts how often the transaction was resumed. */
    private static final class Watched extends Context {
        final AtomicInteger resumes = new AtomicInteger();

        @Override
        public synchronized void resume() {
            resumes.incrementAndGet();
            super.resume();
        }

        @Override
        public String toString() {
            return "resumed " + resumes.get() + " time(s), RATE=" + get(QuoteRate.RATE);
        }
    }

    private static Watched start(QuoteRate quote, String amount) throws Exception {
        ISOMsg m = new ISOMsg("0200");
        m.set(2, "4761739001010119");
        m.set(3, "000000");
        m.set(4, amount);
        m.set(11, "003377");
        m.set(41, "60400155");
        m.set(49, "978");
        Watched ctx = new Watched();
        ctx.put(ContextConstants.REQUEST.toString(), m);
        quote.prepare(1L, ctx);
        return ctx;
    }
}

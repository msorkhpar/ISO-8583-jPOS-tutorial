import com.example.practice.InFlightChecks;
import org.jpos.iso.ISOMsg;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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
            for (boolean shares : new boolean[] { false, true }) {
                String name = shares ? "CardStatus keeping the request in a field" : "CardStatus keeping it local";
                try {
                    InFlightChecks.checkAll(() -> new Simulated(shares));
                    System.out.println(name + ": all checks pass");
                } catch (AssertionError caught) {
                    System.out.println(name + ": caught, " + caught.getMessage());
                }
            }
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }

    /** CardStatus and SendResponse simulated in memory, two transactions at a time. */
    private static final class Simulated implements InFlightChecks.Chain {
        private final boolean shares;
        private final AtomicBoolean holdNext = new AtomicBoolean();
        private volatile CountDownLatch held = new CountDownLatch(1), release = new CountDownLatch(1);
        private volatile ISOMsg shared;

        Simulated(boolean shares) {
            this.shares = shares;
        }

        private ISOMsg process(ISOMsg request) throws Exception {
            shared = request;
            if (holdNext.getAndSet(false)) {
                held.countDown();
                release.await(5, TimeUnit.SECONDS);
            }
            ISOMsg answer = (ISOMsg) (shares ? shared : request).clone();
            answer.setResponseMTI();
            answer.set(39, "00");
            return answer;
        }

        @Override public ISOMsg send(ISOMsg request) throws Exception { return process(request); }

        @Override
        public InFlightChecks.Pending start(ISOMsg request) {
            CompletableFuture<ISOMsg> f = CompletableFuture.supplyAsync(() -> {
                try { return process(request); } catch (Exception e) { throw new IllegalStateException(e); }
            });
            return () -> f.get(10, TimeUnit.SECONDS);
        }

        @Override
        public void holdNextLookup() {
            held = new CountDownLatch(1);
            release = new CountDownLatch(1);
            holdNext.set(true);
        }

        @Override public void awaitHeldLookup() throws Exception { held.await(5, TimeUnit.SECONDS); }
        @Override public void releaseLookup() { release.countDown(); }
    }
}

import com.example.practice.ReversalForwarder;
import org.jpos.iso.ISOMsg;
import org.jpos.space.TSpace;

import java.util.function.Consumer;
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
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        Logger root = Logger.getLogger("");
        root.setLevel(Level.ALL);
        root.addHandler(handler);
        JDK_EXIT.setLevel(Level.OFF);

        // A stand-in for the collaborator, like the one the tests use for the first main case.
        TSpace<String, Object> sp = new TSpace<>();
        ReversalForwarder channelA = new ReversalForwarder(sp, "reversal.visa", host("channel A"));
        ReversalForwarder channelB = new ReversalForwarder(sp, "reversal.visa", host("channel B"));
        try {
            for (String stan : new String[] { "002201", "002202", "002203" }) {
                long start = System.nanoTime();
                sp.out("reversal.visa", reversal(stan));
                System.out.println("queued " + stan + " in " + (System.nanoTime() - start) / 1_000_000 + " ms");
            }
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        } finally {
            channelA.close();
            channelB.close();
        }
        System.out.println("still queued: " + sp.rdp("reversal.visa"));
    }

    /** A host link that takes 300 ms to answer each reversal. */
    private static Consumer<ISOMsg> host(String channel) {
        return m -> {
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println(channel + " sent " + m.getString(0) + " STAN " + m.getString(11));
        };
    }

    private static ISOMsg reversal(String stan) throws Exception {
        ISOMsg m = new ISOMsg("0420");
        m.set(4, "000000004875");
        m.set(11, stan);
        m.set(41, "60400126");
        return m;
    }
}

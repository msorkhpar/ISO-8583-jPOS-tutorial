import com.example.practice.CardCheckListener;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOSource;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
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
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        Logger root = Logger.getLogger("");
        root.setLevel(Level.ALL);
        root.addHandler(handler);
        JDK_EXIT.setLevel(Level.OFF);

        // A stand-in for the collaborator, like the one the tests use for the first main case.
        try {
            CardCheckListener listener = new CardCheckListener(pan -> {
                if (pan.startsWith("4000")) Thread.sleep(1500);
                return pan.startsWith("6011");
            });
            BlockingQueue<String> till = new LinkedBlockingQueue<>();
            ISOSource connection = new ISOSource() {
                @Override public void send(ISOMsg r) { till.add(r.getString(11) + " 39=" + r.getString(39)); }
                @Override public boolean isConnected() { return true; }
            };
            long t0 = System.nanoTime();
            System.out.println("slow lookup taken: " + listener.process(connection, purchase("4000056655665556", "001301")));
            System.out.println("process returned at once: " + ((System.nanoTime() - t0) / 1_000_000 < 500));
            listener.process(connection, purchase("6011000990139424", "001302"));
            System.out.println("first answer: " + till.poll(5, TimeUnit.SECONDS));
            System.out.println("second answer: " + till.poll(5, TimeUnit.SECONDS));
            ISOMsg echo = new ISOMsg("0800");
            echo.set(11, "001303");
            System.out.println("0800 taken: " + listener.process(connection, echo));
            System.exit(0); // the listener keeps its lookup threads and offers no way to stop them
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }

    private static ISOMsg purchase(String pan, String stan) throws Exception {
        ISOMsg m = new ISOMsg("0200");
        m.set(2, pan);
        m.set(3, "000000");
        m.set(4, "000000002215");
        m.set(11, stan);
        m.set(41, "POS00916");
        return m;
    }
}

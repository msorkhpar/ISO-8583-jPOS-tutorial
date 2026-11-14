import com.example.practice.HoldingHost;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOSource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
            HoldingHost host = new HoldingHost(Set.of("002002", "002003"));
            Client till1 = new Client();
            Client till2 = new Client();
            host.process(till1, purchase("002001"));
            host.process(till1, purchase("002002"));
            host.process(till2, purchase("002003"));
            System.out.println("till 1 before release: " + till1.got);
            System.out.println("till 2 before release: " + till2.got);
            till2.connected = false;
            System.out.println("released to a client: " + host.release());
            System.out.println("till 1 after release: " + till1.got);
            System.out.println("released again: " + host.release());
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }

    private static ISOMsg purchase(String stan) throws Exception {
        ISOMsg m = new ISOMsg("0200");
        m.set(2, "4000056655665556");
        m.set(4, "000000002720");
        m.set(11, stan);
        m.set(41, "27180011");
        return m;
    }

    /** A client connection that keeps the STANs it was sent; a closed one refuses, as a socket would. */
    static final class Client implements ISOSource {
        final List<String> got = new ArrayList<>();
        volatile boolean connected = true;

        @Override public synchronized void send(ISOMsg m) throws IOException {
            if (!connected) throw new IOException("connection closed");
            got.add(m.getString(11));
        }
        @Override public boolean isConnected() { return connected; }
    }
}

import com.example.practice.NetworkRequests;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOSource;
import org.jpos.space.TSpace;
import java.util.Date;
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
            TSpace<String, Object> sp = new TSpace<>();
            Date ready = new Date();
            sp.put("west-channel.ready", ready);
            sp.put("west-mux.logon", ready);
            sp.put("west-mux.enabled", ready);
            NetworkRequests listener = new NetworkRequests(sp, "west-mux", m -> false);
            Link link = new Link();
            System.out.println("echo taken: " + listener.process(link, fromHost("0800", "301", "000801")));
            ISOMsg key = fromHost("0800", "161", "000802");
            key.set(48, "0123456789ABCDEFFEDCBA9876543210" + "D5D44F");
            System.out.println("key change (check fails) taken: " + listener.process(link, key));
            System.out.println("cutover advice taken: " + listener.process(link, fromHost("0821", "201", "000803")));
            System.out.println("unknown code taken: " + listener.process(link, fromHost("0800", "999", "000804")));
            System.out.println("sign-off taken: " + listener.process(link, fromHost("0800", "002", "000805")));
            System.out.println("link still enabled: " + (sp.rdp("west-mux.enabled") != null));
            ISOMsg purchase = new ISOMsg("0200");
            purchase.set(11, "000806");
            System.out.println("0200 taken: " + listener.process(link, purchase));
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }

    private static ISOMsg fromHost(String mti, String code, String stan) throws Exception {
        ISOMsg m = new ISOMsg(mti);
        m.set(7, "1114051500");
        m.set(11, stan);
        m.set(70, code);
        return m;
    }

    /** The link back to the host: prints each answer the listener sends. */
    static final class Link implements ISOSource {
        @Override public void send(ISOMsg m) throws ISOException {
            System.out.println("  answer: " + m.getMTI() + " 11=" + m.getString(11) + " 70=" + m.getString(70)
                    + " 39=" + m.getString(39) + (m.hasField(48) ? " 48=" + m.getString(48) : ""));
        }
        @Override public boolean isConnected() { return true; }
    }
}

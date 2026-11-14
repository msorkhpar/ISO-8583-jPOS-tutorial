import com.example.practice.SheetHost;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOSource;
import java.util.List;
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
            SheetHost host = new SheetHost(List.of(
                "T01;0200;4000056655665556;;;05",
                "T02;0200;;100.00;199.99;51",
                "T03;0200;;7.50;7.50;--",
                "T04;0200;;;;00"));
            System.out.println("listed card: " + answer(host, purchase("4000056655665556", "000000015000", "001101")));
            System.out.println("150.00: " + answer(host, purchase("5200828282828210", "000000015000", "001102")));
            System.out.println("199.99: " + answer(host, purchase("5200828282828210", "000000019999", "001103")));
            System.out.println("200.00: " + answer(host, purchase("5200828282828210", "000000020000", "001104")));
            System.out.println("7.50: " + answer(host, purchase("5200828282828210", "000000000750", "001105")));
            try {
                new SheetHost(List.of("T05;0200;;20;30.00;00"));
                System.out.println("row with amount 20: loaded");
            } catch (IllegalArgumentException e) {
                System.out.println("row with amount 20: refused, " + e.getMessage());
            }
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }

    private static ISOMsg purchase(String pan, String amount, String stan) throws Exception {
        ISOMsg m = new ISOMsg("0200");
        m.set(2, pan);
        m.set(3, "000000");
        m.set(4, amount);
        m.set(11, stan);
        m.set(41, "27180009");
        return m;
    }

    /** The rc the host answered with, or "none" when it sent nothing. */
    private static String answer(SheetHost host, ISOMsg request) {
        String[] rc = { "none" };
        host.process(new ISOSource() {
            @Override public void send(ISOMsg r) { rc[0] = r.getString(39); }
            @Override public boolean isConnected() { return true; }
        }, request);
        return rc[0];
    }
}

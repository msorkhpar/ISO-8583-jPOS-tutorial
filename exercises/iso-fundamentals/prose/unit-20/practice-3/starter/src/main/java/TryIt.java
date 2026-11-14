import com.example.practice.IssuerLedger;
import org.jpos.iso.ISOMsg;
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
            IssuerLedger ledger = new IssuerLedger();
            System.out.println("purchase 2750: " + ledger.handle(tx("0200", "010001", "1103091500", "55003", "40000012", "730701010001", 2750)));
            System.out.println("advice 910: " + ledger.handle(tx("0220", "010002", "1103091600", "55003", "40000012", "730701010002", 910)));
            System.out.println("its repeat, new STAN: " + ledger.handle(tx("0221", "010009", "1103091900", "55003", "40000012", "730701010002", 910)));
            System.out.println("reversal of the purchase: " + ledger.handle(tx("0420", "010010", "1103092000", "55003", "40000012", "730701010001", 2750)));
            System.out.println("the reversal again: " + ledger.handle(tx("0421", "010011", "1103092100", "55003", "40000012", "730701010001", 2750)));
            System.out.println("balance: " + ledger.balance());
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }

    private static ISOMsg tx(String mti, String stan, String de7, String acquirer, String terminal,
                             String rrn, long amount) throws Exception {
        ISOMsg m = new ISOMsg(mti);
        m.set(2, "4111111111111111");
        m.set(4, String.format("%012d", amount));
        m.set(7, de7);
        m.set(11, stan);
        m.set(32, acquirer);
        m.set(37, rrn);
        m.set(41, terminal);
        return m;
    }
}

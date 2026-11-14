import com.example.practice.Account;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jpos.iso.ISOMsg;

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
            Account account = new Account(50_000);
            ISOMsg approved = account.authorize(message("0100", 9_000, "081501"));
            System.out.println("0110: 39=" + approved.getString(39) + " 38=" + approved.getString(38) + ", available " + account.available());

            ISOMsg advice = message("0120", 6_500, "081502");
            advice.set(38, "S40021");
            ISOMsg ack = account.standIn(advice);
            System.out.println("stand-in ack: " + ack.getMTI() + " 39=" + ack.getString(39) + ", available " + account.available());

            account.clear(new Account.ClearingRecord(approved.getString(38), 7_820));
            account.clear(new Account.ClearingRecord("S40021", 6_500));
            System.out.println("after clearing: ledger " + account.ledger() + ", available " + account.available());
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }

    private static ISOMsg message(String mti, long amount, String stan) throws Exception {
        ISOMsg m = new ISOMsg(mti);
        m.set(2, "4000056655665556");
        m.set(3, "000000");
        m.set(4, String.format("%012d", amount));
        m.set(11, stan);
        m.set(41, "TERM0700");
        return m;
    }
}

import com.example.practice.RequiredFields;
import org.jpos.core.SimpleConfiguration;
import org.jpos.iso.ISOMsg;
import org.jpos.transaction.Context;
import org.jpos.transaction.ContextConstants;
import org.jpos.transaction.TransactionConstants;
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
            System.out.println("0200 with amount: " + prepare("0200", "000610", "000000002480"));
            System.out.println("0200 without amount: " + prepare("0200", "000611", null));
            System.out.println("0100 without amount: " + prepare("0100", "000612", null));
            System.out.println("0800 echo: " + prepare("0800", "000613", null));
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }

    /** Runs the participant's prepare on one request and says what it decided. */
    private static String prepare(String mti, String stan, String amount) throws Exception {
        ISOMsg m = new ISOMsg(mti);
        if (!mti.startsWith("08")) m.set(3, "000000");
        if (amount != null) m.set(4, amount);
        if (mti.startsWith("08")) m.set(70, "301");
        m.set(7, "1114113000");
        m.set(11, stan);
        m.set(41, "27180050");
        RequiredFields participant = new RequiredFields();
        participant.setConfiguration(new SimpleConfiguration());
        Context ctx = new Context();
        ctx.put(ContextConstants.REQUEST.toString(), m);
        int result = participant.prepare(1L, ctx);
        String verdict = (result & TransactionConstants.PREPARED) != 0 ? "PREPARED" : "ABORTED";
        ISOMsg r = ctx.get(ContextConstants.RESPONSE.toString());
        return verdict + (r == null ? ", no response" : ", response " + r.getMTI() + " 39=" + r.getString(39)
                + " 11=" + r.getString(11) + " 41=" + r.getString(41));
    }
}

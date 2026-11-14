import com.example.practice.CashWithdrawal;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.packager.GenericPackager;
import org.jpos.transaction.Context;
import org.jpos.transaction.ContextConstants;

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
            System.out.println("200.00 euro: " + rc("000000020000", "978"));
            System.out.println("75.20 euro: " + rc("000000007520", "978"));
            System.out.println("250,000 won: " + rc("000000250000", "410"));
            System.out.println("120.000 dinars: " + rc("000000120000", "414"));
            System.out.println("350.000 dinars: " + rc("000000350000", "414"));
            System.out.println("50.00 Swiss francs: " + rc("000000005000", "756"));
            System.out.println("no currency: " + rc("000000020000", null));
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }

    private static String rc(String amount, String currency) throws Exception {
        ISOMsg m = new ISOMsg("0200");
        m.set(3, "011000");
        m.set(4, amount);
        m.set(7, "1114103000");
        m.set(11, "006128");
        m.set(41, "60400058");
        if (currency != null) m.set(49, currency);
        GenericPackager p = new GenericPackager("jar:packager/iso87ascii.xml");
        m.setPackager(p);
        byte[] wire = m.pack();
        ISOMsg in = new ISOMsg();
        in.setPackager(p);
        in.unpack(wire);
        Context ctx = new Context();
        ctx.put(ContextConstants.REQUEST.toString(), in);
        new CashWithdrawal().prepare(1L, ctx);
        ISOMsg response = ctx.get(ContextConstants.RESPONSE.toString());
        return response == null ? "no response" : "field 39=" + response.getString(39);
    }
}

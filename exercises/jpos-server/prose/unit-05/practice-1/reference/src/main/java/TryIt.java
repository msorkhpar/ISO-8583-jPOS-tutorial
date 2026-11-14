import com.example.practice.AcceptedCurrencies;
import org.jpos.core.ConfigurationException;
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
            AcceptedCurrencies visa = configured("978, 840 826", null);
            AcceptedCurrencies mastercard = configured("978", "57");
            System.out.println("visa in 826: " + check(visa, "826", "000501"));
            System.out.println("visa in 036: " + check(visa, "036", "000502"));
            System.out.println("mastercard in 978: " + check(mastercard, "978", "000503"));
            System.out.println("mastercard in 840: " + check(mastercard, "840", "000504"));
            SimpleConfiguration misspelt = new SimpleConfiguration();
            misspelt.put("currencys", "978");
            try {
                new AcceptedCurrencies().setConfiguration(misspelt);
                System.out.println("misspelt property: deployed");
            } catch (ConfigurationException e) {
                System.out.println("misspelt property: refused, " + e.getMessage());
            }
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }

    private static AcceptedCurrencies configured(String currencies, String rc) throws Exception {
        SimpleConfiguration cfg = new SimpleConfiguration();
        cfg.put("currencies", currencies);
        if (rc != null)
            cfg.put("rc", rc);
        AcceptedCurrencies participant = new AcceptedCurrencies();
        participant.setConfiguration(cfg);
        return participant;
    }

    private static String check(AcceptedCurrencies participant, String currency, String stan) throws Exception {
        ISOMsg m = new ISOMsg("0200");
        m.set(2, "4024007103939509");
        m.set(3, "000000");
        m.set(4, "000000002940");
        m.set(11, stan);
        m.set(41, "60400031");
        m.set(49, currency);
        Context ctx = new Context();
        ctx.put(ContextConstants.REQUEST.toString(), m);
        if ((participant.prepare(1L, ctx) & TransactionConstants.PREPARED) != 0)
            return "goes on";
        ISOMsg r = ctx.get(ContextConstants.RESPONSE.toString());
        return "declined, " + r.getMTI() + " field 39=" + r.getString(39) + " field 11=" + r.getString(11);
    }
}

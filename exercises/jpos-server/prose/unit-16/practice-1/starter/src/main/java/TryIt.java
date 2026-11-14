import com.example.practice.ForwardLeg;
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
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        Logger root = Logger.getLogger("");
        root.setLevel(Level.ALL);
        root.addHandler(handler);
        JDK_EXIT.setLevel(Level.OFF);

        // A stand-in for the collaborator, like the one the tests use for the first main case.
        try {
            SimpleConfiguration cfg = new SimpleConfiguration();
            cfg.put("space", "tspace:tryit");
            cfg.put("terminal-id", "SW000007");
            cfg.put("acquirer-id", "98765432109");
            ForwardLeg.Outbound outbound = new ForwardLeg.Outbound();
            outbound.setConfiguration(cfg);
            System.out.println("fleet purchase: " + forward(outbound, "fleet", "PMP00019", "000431", "000000004275"));
            System.out.println("visa purchase: " + forward(outbound, "visa", "PMP00019", "000432", "000000001990"));
            System.out.println("fleet inquiry: " + forward(outbound, "fleet", "PMP00020", "000431", null));
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }

    /** Outbound, a network that approves at once, then Reply: what each side sees. */
    private static String forward(ForwardLeg.Outbound outbound, String link, String terminal, String stan,
                                  String amount) throws Exception {
        ISOMsg m = new ISOMsg("0200");
        m.set(2, "7083000000041234");
        m.set(3, amount == null ? "310000" : "000000");
        if (amount != null) m.set(4, amount);
        m.set(7, "1114140500");
        m.set(11, stan);
        m.set(41, terminal);
        m.set(48, "PUMP07");
        m.set(49, "978");
        Context ctx = new Context();
        ctx.put(ContextConstants.REQUEST.toString(), m);
        ctx.put(ContextConstants.DESTINATION.toString(), link);
        if ((outbound.prepare(1L, ctx) & TransactionConstants.PREPARED) == 0)
            return "Outbound failed, " + ctx.getResult().failure().getMessage();
        ISOMsg out = ctx.get(ForwardLeg.OUTBOUND);
        ISOMsg answer = (ISOMsg) out.clone();
        answer.setResponseMTI();
        answer.set(39, "00");
        answer.set(38, "K4M81Z");
        if (amount == null) answer.set(54, "1001978C000000032500");
        answer.set(63, "NETWORK-PRIVATE");
        ctx.put(ForwardLeg.HOST_RESPONSE, answer);
        if ((new ForwardLeg.Reply().prepare(1L, ctx) & TransactionConstants.PREPARED) == 0)
            return "Reply failed, " + ctx.getResult().failure().getMessage();
        ISOMsg r = ctx.get(ContextConstants.RESPONSE.toString());
        return "network saw 11=" + out.getString(11) + " 41=" + out.getString(41) + " 32=" + out.getString(32)
                + " 48=" + out.getString(48) + "; terminal got 11=" + r.getString(11) + " 41=" + r.getString(41)
                + " 38=" + r.getString(38) + " 54=" + r.getString(54) + " 63=" + r.getString(63);
    }
}

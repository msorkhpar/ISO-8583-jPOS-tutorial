import com.example.practice.CoreAnswer;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.channel.ASCIIChannel;
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
            String v87 = "jar:packager/iso87ascii.xml", v93 = "jar:packager/iso93ascii.xml";
            System.out.println("87, short approval: " + answer(v87, new CoreAnswer.Decision("00", "A41C07", "ENJOY")));
            System.out.println("87, long message: " + answer(v87, new CoreAnswer.Decision("05", null, "DO NOT HONOUR - CONTACT ISSUER NOW")));
            System.out.println("87, long code: " + answer(v87, new CoreAnswer.Decision("00", "A41C07ZZ9", "ENJOY")));
            System.out.println("93, long message: " + answer(v93, new CoreAnswer.Decision("05", null, "DO NOT HONOUR - CONTACT ISSUER NOW")));
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }

    private static String answer(String layout, CoreAnswer.Decision decision) throws Exception {
        ISOMsg m = new ISOMsg("0200");
        m.set(3, "000000");
        m.set(4, "000000006480");
        m.set(7, "1114110500");
        m.set(11, "003316");
        m.set(41, "60400071");
        m.set(49, "978");
        Context ctx = new Context();
        ctx.put(ContextConstants.REQUEST.toString(), m);
        ctx.put(ContextConstants.SOURCE.toString(), new ASCIIChannel(new GenericPackager(layout)));
        ctx.put("CORE", decision);
        new CoreAnswer().prepare(1L, ctx);
        ISOMsg r = ctx.get(ContextConstants.RESPONSE.toString());
        return "39=" + r.getString(39) + " 38=" + r.getString(38) + " 44=" + r.getString(44);
    }
}

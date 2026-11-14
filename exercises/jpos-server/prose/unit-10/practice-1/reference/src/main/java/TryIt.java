import com.example.practice.AnswerFailures;
import org.jpos.iso.ISOMsg;
import org.jpos.rc.CMF;
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
            System.out.println("PIN tries exceeded: " + abort(CMF.MAX_PIN_TRIES_EXCEEDED, null));
            System.out.println("missing field: " + abort(CMF.MISSING_FIELD, null));
            System.out.println("restricted card (not in the table): " + abort(CMF.RESTRICTED_CARD, null));
            System.out.println("nothing reported: " + abort(null, null));
            System.out.println("approval built, then journal failed: " + abort(null, "00"));
            System.out.println("partial approval built, then journal failed: " + abort(null, "10"));
            System.out.println("over the limit, 61 built: " + abort(null, "61"));
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }

    /** One aborting withdrawal, with a reported cause and an answer already built, if any. */
    private static String abort(CMF cause, String builtRc) throws Exception {
        ISOMsg m = new ISOMsg("0200");
        m.set(2, "4012888888881881");
        m.set(3, "010000");
        m.set(4, "000000008000");
        m.set(7, "1114130000");
        m.set(11, "004502");
        m.set(41, "ATM00117");
        m.set(49, "978");
        Context ctx = new Context();
        ctx.put(ContextConstants.REQUEST.toString(), m);
        if (cause != null)
            ctx.getResult().fail(cause, "TryIt", "reported by a participant");
        if (builtRc != null) {
            ISOMsg built = (ISOMsg) m.clone();
            built.setResponseMTI();
            built.set(39, builtRc);
            ctx.put(ContextConstants.RESPONSE.toString(), built);
        }
        new AnswerFailures().prepareForAbort(1L, ctx);
        ISOMsg r = ctx.get(ContextConstants.RESPONSE.toString());
        return "field 39=" + r.getString(39);
    }
}

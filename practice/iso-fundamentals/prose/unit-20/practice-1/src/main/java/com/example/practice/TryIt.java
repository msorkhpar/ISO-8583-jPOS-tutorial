package com.example.practice;

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

        // A switch with an empty journal and queue, and a terminal that prints what it is told.
        try {
            TimeoutHandler.Db db = new TimeoutHandler.Db();
            TimeoutHandler h = new TimeoutHandler(db, (stan, outcome) -> System.out.println("terminal: " + stan + " " + outcome));
            ISOMsg purchase = purchase("004211");

            h.onNoAnswer(purchase, "004212", "1102193045");
            System.out.println("journal after timeout: " + db.journal("004211"));
            System.out.println("queued: " + db.queue().size());
            if (!db.queue().isEmpty()) {
                ISOMsg rev = db.queue().get(0);
                System.out.println("reversal: " + rev.getMTI() + " stan " + rev.getString(11) + " field 90 " + rev.getString(90));
            }

            ISOMsg late = (ISOMsg) purchase.clone();
            late.setResponseMTI();
            late.set(39, "00");
            h.onLateAnswer(late);
            System.out.println("journal after late approval: " + db.journal("004211"));
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }

    private static ISOMsg purchase(String stan) throws Exception {
        ISOMsg m = new ISOMsg("0200");
        m.set(2, "4111111111111111");
        m.set(3, "000000");
        m.set(4, "000000002750");
        m.set(7, "1102193015");
        m.set(11, stan);
        m.set(32, "4400");
        m.set(41, "29110007");
        m.set(49, "978");
        return m;
    }
}

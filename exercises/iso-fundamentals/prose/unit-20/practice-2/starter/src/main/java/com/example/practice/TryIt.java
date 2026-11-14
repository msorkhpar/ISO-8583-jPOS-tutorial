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

        // The network's limits: 20 s base, 10 min cap, 4 attempts, 1 hour window. Times are in ms.
        try {
            long s = 1000;
            LinkSaf saf = new LinkSaf(20 * s, 10 * 60 * s, 4, 60 * 60 * s);
            saf.store(advice("0220", "007301"), 0);
            saf.store(advice("0420", "007302"), 0);

            ISOMsg first = saf.due(0);
            System.out.println("due at 0: " + first.getMTI() + " stan " + first.getString(11));
            saf.failed(1 * s);
            System.out.println("next due at: " + saf.nextDueAt() / s + " s");
            System.out.println("due at 5 s: " + saf.due(5 * s));
            saf.linkUp(6 * s);
            ISOMsg again = saf.due(6 * s);
            System.out.println("after link up: " + again.getMTI() + " stan " + again.getString(11));
            for (int i = 0; i < 3; i++) {
                saf.failed(saf.nextDueAt());
            }
            System.out.println("parked: " + saf.parked().size() + ", pending: " + saf.size());
            System.out.println("waits: " + saf.waitAfter(1) / s + " " + saf.waitAfter(2) / s + " " + saf.waitAfter(6) / s + " s");
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }

    private static ISOMsg advice(String mti, String stan) throws Exception {
        ISOMsg m = new ISOMsg(mti);
        m.set(4, "000000004500");
        m.set(11, stan);
        m.set(41, "29110007");
        return m;
    }
}

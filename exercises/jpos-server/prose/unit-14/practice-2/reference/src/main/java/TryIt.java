import com.example.practice.PinService;
import org.jpos.util.NameRegistrar;

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
        PinService pins = new PinService();
        try {
            pins.setName("tryit-pins");
            pins.init();
            pins.start();
            System.out.println("started with no HSM: " + pins.getStateAsString());
            NameRegistrar.register("hsm", (PinService.Hsm) block -> "driver-1:" + block.toLowerCase());
            pins.start();
            System.out.println("started again with the HSM up: " + pins.getStateAsString());
            System.out.println("translated: " + pins.translate("7A31C0E9B4D2F658"));
            NameRegistrar.unregister("hsm");
            NameRegistrar.register("hsm", (PinService.Hsm) block -> "driver-2:" + block.toLowerCase());
            System.out.println("after the HSM redeploy: " + pins.translate("7A31C0E9B4D2F658"));
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        } finally {
            pins.stop();
            NameRegistrar.unregister("hsm");
        }
    }
}

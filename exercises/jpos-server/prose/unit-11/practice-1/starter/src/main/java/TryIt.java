import com.example.practice.HoldFundsChecks;
import org.jpos.iso.ISOMsg;

import java.util.HashMap;
import java.util.Map;
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
            for (String flaw : new String[] { "none", "keeps-declined", "refund-on-refusal", "silent-refusal" }) {
                try {
                    HoldFundsChecks.checkAll(() -> new Simulated(flaw));
                    System.out.println("flaw " + flaw + ": all checks pass");
                } catch (AssertionError caught) {
                    System.out.println("flaw " + flaw + ": caught, " + caught.getMessage());
                }
            }
        } catch (RuntimeException stopped) {
            // A method you have not finished, or a bug, stops here with its message, not a build error.
            System.out.println("Run stopped: " + stopped);
        }
    }

    /** A chain of HoldFunds, the issuer and SendResponse, simulated in memory, with one flaw or none. */
    private static final class Simulated implements HoldFundsChecks.Chain {
        private final Map<String, Long> balances = new HashMap<>();
        private final String flaw;
        private String issuer = "00";

        Simulated(String flaw) {
            this.flaw = flaw;
        }

        @Override public void open(String card, long balance) { balances.put(card, balance); }
        @Override public long balance(String card) { return balances.getOrDefault(card, 0L); }
        @Override public void issuerAnswers(String responseCode) { issuer = responseCode; }

        @Override
        public ISOMsg send(ISOMsg request) throws Exception {
            String card = request.getString(2);
            long amount = Long.parseLong(request.getString(4));
            String rc = issuer;
            if (balance(card) < amount) {
                if (flaw.equals("refund-on-refusal")) balances.merge(card, amount, Long::sum);
                if (flaw.equals("silent-refusal")) return null;
                rc = "51";
            } else {
                balances.merge(card, -amount, Long::sum);
                if (!rc.equals("00") && !flaw.equals("keeps-declined")) balances.merge(card, amount, Long::sum);
            }
            ISOMsg answer = (ISOMsg) request.clone();
            answer.setResponseMTI();
            answer.set(39, rc);
            return answer;
        }
    }
}

/**
 * Read an ISO-8583 Message Type Indicator, one digit at a time.
 *
 * <p>The MTI is four numeric digits and each digit answers a different
 * question: which version of the standard, which class of message, which
 * function it performs, and where it came from. Nothing here needs a
 * library — the whole of it is in the four tables below.
 */
public final class Mti {

    /** How many digits an MTI has, always. */
    public static final int DIGITS = 4;

    private Mti() {
    }

    /**
     * Return the four parts of {@code mti}, in the order the standard reads them:
     * version, message class, message function, message origin.
     */
    public static String[] parts(String mti) {
        if (mti == null || mti.length() != DIGITS) {
            throw new IllegalArgumentException("an MTI is exactly " + DIGITS + " digits");
        }
        for (int at = 0; at < DIGITS; at++) {
            char digit = mti.charAt(at);
            if (digit < '0' || digit > '9') {
                throw new IllegalArgumentException("an MTI is exactly " + DIGITS + " digits");
            }
        }
        return new String[] {
            version(mti.charAt(0)),
            messageClass(mti.charAt(1)),
            messageFunction(mti.charAt(2)),
            messageOrigin(mti.charAt(3)),
        };
    }

    /** The first digit: which edition of the standard the message is written against. */
    public static String version(char digit) {
        switch (digit) {
            case '0': return "ISO 8583-1:1987";
            case '1': return "ISO 8583-2:1993";
            case '2': return "ISO 8583-3:2003";
            default: return "Reserved";
        }
    }

    /** The second digit: what the message is for. */
    public static String messageClass(char digit) {
        switch (digit) {
            case '1': return "Authorization";
            case '2': return "Financial";
            case '3': return "File actions";
            case '4': return "Reversal and chargeback";
            case '5': return "Reconciliation";
            case '6': return "Administrative";
            case '7': return "Fee collection";
            case '8': return "Network management";
            default: return "Reserved";
        }
    }

    /** The third digit: where in the exchange this message sits. */
    public static String messageFunction(char digit) {
        switch (digit) {
            case '0': return "Request";
            case '1': return "Response";
            case '2': return "Advice";
            case '3': return "Advice response";
            default: return "Reserved";
        }
    }

    /** The fourth digit: which side of the link sent it. */
    public static String messageOrigin(char digit) {
        switch (digit) {
            case '0': return "Acquirer";
            case '1': return "Acquirer repeat";
            case '2': return "Issuer";
            case '3': return "Issuer repeat";
            case '4': return "Other";
            case '5': return "Other repeat";
            default: return "Reserved";
        }
    }

    /** Print one MTI's four parts, so Run has something to show. */
    public static void main(String[] args) {
        String mti = args.length > 0 ? args[0] : "0100";
        String[] parts = parts(mti);
        System.out.println("MTI " + mti);
        System.out.println("  version:  " + parts[0]);
        System.out.println("  class:    " + parts[1]);
        System.out.println("  function: " + parts[2]);
        System.out.println("  origin:   " + parts[3]);
    }
}

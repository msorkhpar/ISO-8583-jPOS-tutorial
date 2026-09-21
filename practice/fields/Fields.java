/**
 * Walk an ISO-8583 message and print the data elements it carries.
 *
 * <p>Nothing grades this file. Run it, read what it prints, then change it:
 * add a field to the sample message, or teach {@link #unpack} a data element
 * it does not know yet. That is the whole exercise.
 *
 * <p>The sample below is a made-up authorization request. Its account number
 * is one of the standard test values that no bank ever issues.
 */
public final class Fields {

    /** A made-up authorization request: MTI, primary bitmap, then the fields in order. */
    public static final String SAMPLE =
        "0100"
        + "7000000000000000"
        + "16" + "4111111111111111"
        + "000000"
        + "000000010000";

    private Fields() {
    }

    /** Print every part of {@code message}, in the order it is written. */
    public static void unpack(String message) {
        int at = 0;
        System.out.println("MTI            " + message.substring(at, at += 4));
        System.out.println("primary bitmap " + message.substring(at, at += 16));

        // Data element 2 is variable length: two digits of length, then that many.
        int length = Integer.parseInt(message.substring(at, at += 2));
        System.out.println("DE 2  PAN      " + message.substring(at, at += length)
            + "  (LLVAR, " + length + " digits)");

        // Data elements 3 and 4 are fixed length, so nothing announces them.
        System.out.println("DE 3  proc code " + message.substring(at, at += 6) + "  (n 6)");
        System.out.println("DE 4  amount    " + message.substring(at, at += 12) + "  (n 12)");

        if (at != message.length()) {
            System.out.println("and " + (message.length() - at) + " character(s) nobody read");
        }
    }

    public static void main(String[] args) {
        unpack(args.length > 0 ? args[0] : SAMPLE);
    }
}

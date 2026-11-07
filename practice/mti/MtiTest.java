/**
 * The grader for {@link Mti}. Plain Java, no test framework and no jar to fetch:
 * it prints one line per check and leaves with a non-zero status if any failed.
 */
public final class MtiTest {

    private static int failures;

    private MtiTest() {
    }

    public static void main(String[] args) {
        String[] authorizationRequest = Mti.parts("0100");
        equal("0100 version", "ISO 8583:1987", authorizationRequest[0]);
        equal("0100 class", "Authorization", authorizationRequest[1]);
        equal("0100 function", "Request", authorizationRequest[2]);
        equal("0100 origin", "Acquirer", authorizationRequest[3]);

        String[] networkResponse = Mti.parts("0810");
        equal("0810 class", "Network management", networkResponse[1]);
        equal("0810 function", "Request response", networkResponse[2]);
        equal("0810 origin", "Acquirer", networkResponse[3]);

        String[] reversalAdvice = Mti.parts("1422");
        equal("1422 version", "ISO 8583:1993", reversalAdvice[0]);
        equal("1422 class", "Reversal and chargeback", reversalAdvice[1]);
        equal("1422 function", "Advice", reversalAdvice[2]);
        equal("1422 origin", "Issuer", reversalAdvice[3]);

        String[] instruction = Mti.parts("2160");
        equal("2160 version", "ISO 8583-1:2003", instruction[0]);
        equal("2160 class", "Authorization", instruction[1]);
        equal("2160 function", "Instruction", instruction[2]);

        String[] notification = Mti.parts("1140");
        equal("1140 function", "Notification", notification[2]);

        equal("3100 version", "Reserved for ISO use", Mti.parts("3100")[0]);
        equal("8100 version", "Reserved for national use", Mti.parts("8100")[0]);
        equal("9100 version", "Reserved for private use", Mti.parts("9100")[0]);
        equal("0170 function", "Instruction acknowledgement", Mti.parts("0170")[2]);
        equal("0106 origin", "Reserved for ISO use", Mti.parts("0106")[3]);

        refuses("three digits", "010");
        refuses("a letter", "01O0");

        if (failures > 0) {
            System.out.println(failures + " check(s) failed");
            System.exit(1);
        }
        System.out.println("every check passed");
    }

    private static void equal(String what, String wanted, String found) {
        if (wanted.equals(found)) {
            System.out.println("ok   " + what);
            return;
        }
        failures++;
        System.out.println("FAIL " + what + ": wanted " + wanted + ", found " + found);
    }

    private static void refuses(String what, String mti) {
        try {
            Mti.parts(mti);
        } catch (IllegalArgumentException refused) {
            System.out.println("ok   refuses " + what);
            return;
        }
        failures++;
        System.out.println("FAIL " + what + " was accepted as an MTI");
    }
}

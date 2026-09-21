/**
 * The grader for {@link Bitmap}. Plain Java, no test framework and no jar to
 * fetch: it prints one line per check and leaves with a non-zero status if any
 * failed.
 */
public final class BitmapTest {

    /** The data element the first bit is reserved for, which is not one. */
    private static final int SECONDARY_FLAG = 1;

    private static int failures;

    private BitmapTest() {
    }

    public static void main(String[] args) {
        // Bits 2, 3 and 4 are the first byte's second, third and fourth: 0111 0000.
        equal("fields 2, 3, 4", "7000000000000000", Bitmap.primary(2, 3, 4));
        // Bit 11 is the second byte's third: 0010 0000.
        equal("fields 2, 3, 4, 11", "7020000000000000", Bitmap.primary(2, 3, 4, 11));
        // Bit 2 alone: 0100 0000.
        equal("field 2", "4000000000000000", Bitmap.primary(2));
        // Bit 64 is the last bit of the last byte.
        equal("field 64", "0000000000000001", Bitmap.primary(64));
        equal("no fields at all", "0000000000000000", Bitmap.primary());

        refuses("bit 1, which is the secondary bitmap flag", SECONDARY_FLAG);
        refuses("a data element past the primary bitmap", Bitmap.FIELDS + 1);

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

    private static void refuses(String what, int field) {
        try {
            Bitmap.primary(field);
        } catch (IllegalArgumentException refused) {
            System.out.println("ok   refuses " + what);
            return;
        }
        failures++;
        System.out.println("FAIL " + what + " was accepted");
    }
}

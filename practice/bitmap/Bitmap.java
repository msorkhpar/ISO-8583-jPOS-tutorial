/**
 * Build the primary bitmap of an ISO-8583 message.
 *
 * <p>The primary bitmap is 8 bytes — 64 bits — written as 16 hexadecimal
 * characters. Bit <em>n</em> is set when data element <em>n</em> is present,
 * and the bits are counted from 1, most significant bit first. Bit 1 is not a
 * data element at all: it says a secondary bitmap follows.
 */
public final class Bitmap {

    /** How many data elements the primary bitmap can speak for. */
    public static final int FIELDS = 64;

    /** The data element the first bit is reserved for: the secondary bitmap flag. */
    public static final int SECONDARY_FLAG = 1;

    private Bitmap() {
    }

    /**
     * Return the primary bitmap for {@code fields}, as 16 uppercase hex characters.
     *
     * <p>Each entry is a data element number between 2 and 64.
     */
    public static String primary(int... fields) {
        byte[] bits = new byte[FIELDS / 8];
        for (int field : fields) {
            if (field <= SECONDARY_FLAG || field > FIELDS) {
                throw new IllegalArgumentException(
                    "the primary bitmap speaks for data elements 2 to " + FIELDS);
            }
            bits[(field - 1) / 8] |= (byte) (0x80 >> (field % 8));
        }
        StringBuilder written = new StringBuilder(bits.length * 2);
        for (byte each : bits) {
            written.append(String.format("%02X", each));
        }
        return written.toString();
    }

    /** Print the bitmap of a small authorization request, so Run has something to show. */
    public static void main(String[] args) {
        System.out.println("fields 2, 3, 4      -> " + primary(2, 3, 4));
        System.out.println("fields 2, 3, 4, 11  -> " + primary(2, 3, 4, 11));
        System.out.println("field 64            -> " + primary(64));
    }
}

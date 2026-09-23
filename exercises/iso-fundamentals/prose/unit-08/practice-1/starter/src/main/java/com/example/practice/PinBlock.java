package com.example.practice;

/** An ISO 9564 PIN block: the padded PIN field XOR the PAN field. */
public final class PinBlock {

    private PinBlock() {
    }

    /** The PIN field: 0, the length as one hex digit, the PIN, F padding to sixteen digits. */
    public static String pinField(String pin) {
        throw new UnsupportedOperationException("write pinField");
    }

    /** The PAN field: 0000 and the twelve rightmost PAN digits before the check digit. */
    public static String panField(String pan) {
        throw new UnsupportedOperationException("write panField");
    }

    /** The PIN block: the PIN field XOR the PAN field, sixteen upper-case hex digits. */
    public static String format0(String pin, String pan) {
        throw new UnsupportedOperationException("write format0");
    }
}

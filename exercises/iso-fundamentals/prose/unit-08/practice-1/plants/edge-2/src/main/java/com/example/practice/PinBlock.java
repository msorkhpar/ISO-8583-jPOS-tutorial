package com.example.practice;

import org.jpos.iso.ISOUtil;

/** An ISO 9564 PIN block: the padded PIN field XOR the PAN field. */
public final class PinBlock {

    private PinBlock() {
    }

    /** The PIN field: 0, the length as one hex digit, the PIN, F padding to sixteen digits. */
    public static String pinField(String pin) {
        if (pin == null || pin.isEmpty()) {
            throw new IllegalArgumentException("a PIN is four to twelve digits");
        }
        StringBuilder field = new StringBuilder("0");
        field.append(Integer.toHexString(pin.length()).toUpperCase()).append(pin);
        while (field.length() < 16) {
            field.append('F');
        }
        return field.toString();
    }

    /** The PAN field: 0000 and the twelve rightmost PAN digits before the check digit. */
    public static String panField(String pan) {
        int end = pan.length() - 1;
        return "0000" + pan.substring(end - 12, end);
    }

    /** The PIN block: the PIN field XOR the PAN field, sixteen upper-case hex digits. */
    public static String format0(String pin, String pan) {
        byte[] block = ISOUtil.xor(ISOUtil.hex2byte(pinField(pin)), ISOUtil.hex2byte(panField(pan)));
        return ISOUtil.hexString(block);
    }
}

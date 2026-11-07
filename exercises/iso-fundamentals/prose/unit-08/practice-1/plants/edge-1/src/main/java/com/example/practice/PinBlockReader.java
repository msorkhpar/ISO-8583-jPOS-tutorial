package com.example.practice;

import org.jpos.iso.ISOUtil;

/**
 * The certification lab's test issuer reads PIN blocks here. Its test security module has already
 * decrypted field 52; {@link #pin} gets the clear block and the card number of the message, and
 * returns the PIN the cardholder typed.
 */
public class PinBlockReader {

    /** The PIN in a clear PIN block, or IllegalArgumentException when the block is not one for this card. */
    public static String pin(String clearBlock, String pan) {
        if (clearBlock == null || clearBlock.length() != 16 || !ISOUtil.isNumeric(clearBlock, 16))
            throw new IllegalArgumentException("a clear PIN block is 16 hex digits");
        return pinFrom(pinField(clearBlock.toUpperCase(), pan));
    }

    /** The PIN field inside the block: undo whatever the block's format mixed into it. */
    static String pinField(String block, String pan) {
        char format = block.charAt(0);
        if (format == '1')
            return block;
        if (format != '0' && format != '3')
            throw new IllegalArgumentException("format " + format + " is not used in the lab");
        if (pan == null || pan.length() < 13 || pan.length() > 19 || !ISOUtil.isNumeric(pan, 10))
            throw new IllegalArgumentException("a card number is 13 to 19 digits");
        String panField = "0000" + pan.substring(pan.length() - 12);
        return ISOUtil.hexString(ISOUtil.xor(ISOUtil.hex2byte(block), ISOUtil.hex2byte(panField)));
    }

    /** The PIN, once the field is laid out as its format requires. */
    static String pinFrom(String field) {
        int length = Character.digit(field.charAt(1), 16);
        if (length < 4 || length > 12)
            throw new IllegalArgumentException("a PIN is 4 to 12 digits");
        String pin = field.substring(2, 2 + length);
        if (!ISOUtil.isNumeric(pin, 10))
            throw new IllegalArgumentException("a PIN is digits only");
        String fill = field.charAt(0) == '0' ? "F" : field.charAt(0) == '3' ? "ABCDEF" : "0123456789ABCDEF";
        for (char c : field.substring(2 + length).toCharArray())
            if (fill.indexOf(c) < 0)
                throw new IllegalArgumentException("fill " + c + " is not allowed in format " + field.charAt(0));
        return pin;
    }
}

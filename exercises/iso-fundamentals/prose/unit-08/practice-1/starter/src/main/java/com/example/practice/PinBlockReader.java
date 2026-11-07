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
        // TODO: The block's first digit names its format. The lab's devices send formats 0, 1
        //       and 3; refuse any other with an IllegalArgumentException, and refuse a card
        //       number that a format needs but can't use. Return the PIN field the device built.
        throw new IllegalStateException("TODO: pinField");
    }

    /** The PIN, once the field is laid out as its format requires. */
    static String pinFrom(String field) {
        // TODO: Return the PIN the cardholder typed. A field that is not laid out exactly as its
        //       format requires is the sign of a wrong key or a wrong card: refuse it with an
        //       IllegalArgumentException rather than return a PIN.
        throw new IllegalStateException("TODO: pinFrom");
    }
}

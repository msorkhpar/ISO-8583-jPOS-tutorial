package com.example.practice;

import org.jpos.iso.ISOUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PinBlockReaderTest {
    private static final String VISA = "4111111111111111";
    private static final String MASTERCARD = "5555555555554444";

    /** A clear block as a device builds it: the PIN field XOR the given 16-digit mask. */
    private static String xor(String pinField, String mask) {
        return ISOUtil.hexString(ISOUtil.xor(ISOUtil.hex2byte(pinField), ISOUtil.hex2byte(mask)));
    }

    @Test
    void readsTheFormatZeroBlocksOfACard() {
        assertEquals("7391", PinBlockReader.pin(xor("047391FFFFFFFFFF", "0000111111111111"), VISA));
        assertEquals("503176284", PinBlockReader.pin(xor("09503176284FFFFF", "0000111111111111"), VISA));
        assertEquals("7391", PinBlockReader.pin(xor("047391FFFFFFFFFF", "0000111111111111").toLowerCase(), VISA),
                "a block in lower-case hex is the same block");
    }

    @Test
    void refusesAFormatTheLabDoesNotUse() {
        assertThrows(IllegalArgumentException.class,
                () -> PinBlockReader.pin(xor("247391FFFFFFFFFF", "0000111111111111"), VISA));
        assertThrows(IllegalArgumentException.class,
                () -> PinBlockReader.pin(xor("547391FFFFFFFFFF", "0000111111111111"), VISA));
        assertThrows(IllegalArgumentException.class,
                () -> PinBlockReader.pin(xor("047391FFFFFFFFFF", "0000111111111111"), "411111111111"),
                "a format 0 block needs a whole card number");
    }

    @Test
    void readsACardWhoseLastDigitsDiffer() {
        // 0000 and the 12 digits before the check digit of 5555555555554444
        assertEquals("7391", PinBlockReader.pin(xor("047391FFFFFFFFFF", "0000555555555444"), MASTERCARD));
    }

    @Test
    void readsATwelveDigitPin() {
        assertEquals("820417395162", PinBlockReader.pin(xor("0C820417395162FF", "0000111111111111"), VISA));
        assertEquals("8204173951", PinBlockReader.pin(xor("0A8204173951FFFF", "0000111111111111"), VISA));
    }

    @Test
    void refusesABlockReadAgainstAnotherCard() {
        String builtForVisa = xor("047301FFFFFFFFFF", "0000111111111111");
        assertThrows(IllegalArgumentException.class, () -> PinBlockReader.pin(builtForVisa, MASTERCARD));
    }

    @Test
    void readsFormatThreeWithItsRandomFill() {
        assertEquals("50613", PinBlockReader.pin(xor("3550613BEADCAFEB", "0000111111111111"), VISA));
        assertThrows(IllegalArgumentException.class,
                () -> PinBlockReader.pin(xor("355061397ADCAFEB", "0000111111111111"), VISA),
                "format 3 fill is A to F only");
    }

    @Test
    void readsFormatOneWithoutTheCardNumber() {
        assertEquals("7391", PinBlockReader.pin("1473913A9C0E7B52", MASTERCARD));
        assertEquals("7391", PinBlockReader.pin("1473913A9C0E7B52", null), "a device that doesn't know the card");
    }
}

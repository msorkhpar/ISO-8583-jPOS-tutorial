package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PinBlockTest {

    // ISO 9564 format 0 worked example: PIN 1234 with PAN 43219876543210987.
    private static final String PAN = "43219876543210987";

    @Test
    void buildsThePinFieldThePanFieldAndTheirXor() {
        assertEquals("041234FFFFFFFFFF", PinBlock.pinField("1234"));
        assertEquals("0000987654321098", PinBlock.panField(PAN));
        assertEquals("0412AC89ABCDEF67", PinBlock.format0("1234", PAN));
        // The check digit, the last 5, is left out of the PAN field.
        assertEquals("0000345678901234", PinBlock.panField("5123456789012345"));
        assertEquals("041200A9876FEDCB", PinBlock.format0("1234", "5123456789012345"));
            }

    @Test
    void aTwelveDigitPinHasLengthC() {
        assertEquals("0C123456789012FF", PinBlock.pinField("123456789012"));
        assertEquals("0A1234567890FFFF", PinBlock.pinField("1234567890"));
        assertEquals("0C12AC202CA20267", PinBlock.format0("123456789012", PAN));
    }

    @Test
    void refusesAPinOutsideFourToTwelveDigits() {
        assertThrows(IllegalArgumentException.class, () -> PinBlock.pinField("123"));
        assertThrows(IllegalArgumentException.class, () -> PinBlock.pinField("1234567890123"));
        assertThrows(IllegalArgumentException.class, () -> PinBlock.pinField("12a4"));
        assertThrows(IllegalArgumentException.class, () -> PinBlock.format0("123", PAN));
    }
}

package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class FieldEncoderTest {

    @Test
    void writesEachFieldAsItsFormatSays() {
        assertEquals("ABC123      ", FieldEncoder.encode("an12", "ABC123"));
        assertEquals("05", FieldEncoder.encode("an2", "05"));
        assertEquals("TERM01  ", FieldEncoder.encode("ans8", "TERM01"));
        assertEquals("164111111111111111", FieldEncoder.encode("n..19", "4111111111111111"));
        assertEquals("214111111111111111=2512", FieldEncoder.encode("z..37", "4111111111111111=2512"));
        assertEquals("10ACME Store", FieldEncoder.encode("ans..40", "ACME Store"));
        assertEquals("008VISA1234", FieldEncoder.encode("ans..999", "VISA1234"));
        assertEquals("00", FieldEncoder.encode("ans..99", ""));
        assertThrows(IllegalArgumentException.class, () -> FieldEncoder.encode("n..19", "4111 1111"));
        assertThrows(IllegalArgumentException.class, () -> FieldEncoder.encode("a3", "AB1"));
        assertThrows(IllegalArgumentException.class, () -> FieldEncoder.encode("an12", "ABC-123"));
    }

    @Test
    void numericFieldsArePaddedOnTheLeftWithZeros() {
        assertEquals("000000012345", FieldEncoder.encode("n12", "12345"));
        assertEquals("000000", FieldEncoder.encode("n6", "0"));
    }

    @Test
    void refusesAValueTooLongInsteadOfCuttingIt() {
        assertThrows(IllegalArgumentException.class, () -> FieldEncoder.encode("n12", "1000000000000"));
        assertThrows(IllegalArgumentException.class, () -> FieldEncoder.encode("an2", "005"));
        assertThrows(IllegalArgumentException.class,
            () -> FieldEncoder.encode("ans..40", "A MERCHANT NAME THAT RUNS ON PAST FORTY CHARS"));
    }
}

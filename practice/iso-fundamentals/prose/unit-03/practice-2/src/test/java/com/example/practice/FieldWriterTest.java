package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class FieldWriterTest {

    @Test
    void writesFieldsTheReceiverCanSplit() {
        String wire = FieldWriter.write("n6", "004417")
                + FieldWriter.write("ans8", "POS-0042")
                + FieldWriter.write("n..11", "98765432109")
                + FieldWriter.write("a3", "EUR");
        assertEquals("004417POS-00421198765432109EUR", wire);
    }

    @Test
    void numbersKeepTheirValue() {
        assertEquals("000000007500", FieldWriter.write("n12", "7500"));
        assertEquals("000009", FieldWriter.write("n6", "9"));
        assertEquals("0000", FieldWriter.write("n4", ""));
    }

    @Test
    void tooLongIsRefusedNotCut() {
        assertThrows(IllegalArgumentException.class, () -> FieldWriter.write("n6", "1234567"));
        assertThrows(IllegalArgumentException.class, () -> FieldWriter.write("an2", "A12"));
        assertThrows(IllegalArgumentException.class,
                () -> FieldWriter.write("ans..25", "THE SUNRISE CAFE AND BAKERY"));
    }

    @Test
    void variableFieldCarriesOnlyItsValue() {
        assertEquals("12SUNRISE CAFE", FieldWriter.write("ans..25", "SUNRISE CAFE"));
        assertEquals("0512345", FieldWriter.write("n..6", "12345"));
        assertEquals("00", FieldWriter.write("n..11", ""));
    }

    @Test
    void threeDotsMeanAThreeDigitLength() {
        assertEquals("008REF=AB12", FieldWriter.write("ans...999", "REF=AB12"));
        assertEquals("000", FieldWriter.write("ans...999", ""));
        assertEquals("01112345678901", FieldWriter.write("n...11", "12345678901"));
    }

    @Test
    void charactersOutsideTheTypeAreRefused() {
        assertEquals("AB 12-X ", FieldWriter.write("ans8", "AB 12-X"));
        assertThrows(IllegalArgumentException.class, () -> FieldWriter.write("n12", "75.00"));
        assertThrows(IllegalArgumentException.class, () -> FieldWriter.write("an12", "AB-12"));
        assertThrows(IllegalArgumentException.class, () -> FieldWriter.write("a3", "AB1"));
        assertThrows(IllegalArgumentException.class, () -> FieldWriter.write("n..19", "4111 1111"));
    }
}

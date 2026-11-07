package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;

import org.junit.jupiter.api.Test;

class Field48ReaderTest {

    @Test
    void readsTheCategoryCodeAndTheWantedSubelements() {
        String field48 = "T" + "1003ABC" + "420512345" + "6102XY";
        assertEquals("T", Field48Reader.categoryCode(field48));
        assertEquals(Map.of("42", "12345", "61", "XY"), Field48Reader.wanted(field48, "42", "61", "99"));
        assertNull(Field48Reader.value(field48, "99"));
    }

    @Test
    void aTagsDigitsInsideAnotherSubelementAreData() {
        String field48 = "R" + "1006104207" + "4203777";
        assertEquals("777", Field48Reader.value(field48, "42"));
        assertEquals("104207", Field48Reader.value(field48, "10"));
    }

    @Test
    void aFieldWithoutCategoryCodeStartsWithATag() {
        String field48 = "42049876" + "7701N";
        assertNull(Field48Reader.categoryCode(field48));
        assertEquals("9876", Field48Reader.value(field48, "42"));
        assertEquals("N", Field48Reader.value(field48, "77"));
    }

    @Test
    void aSubelementNobodyToldYouAboutIsSteppedOver() {
        String field48 = "C" + "2705X1Y2Z" + "6103123";
        assertEquals("C", Field48Reader.categoryCode(field48));
        assertEquals("123", Field48Reader.value(field48, "61"));
    }
}

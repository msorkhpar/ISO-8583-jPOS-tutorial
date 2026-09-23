package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class BitmapReaderTest {

    @Test
    void listsTheFieldsBothBitmapsFlag() {
        // A primary bitmap alone: 0111 0000, 0010 0000 -> 2, 3, 4 and 11.
        assertEquals(List.of(2, 3, 4, 11), BitmapReader.present("7020000000000000"));
        assertEquals(List.of(2, 3, 4, 11), BitmapReader.present("7020000000000000".toLowerCase()));
        // Bit 1 set, so a secondary follows: its bit 2 is 66, its bit 64 is 128.
        assertEquals(List.of(2, 3, 4, 66, 128),
            BitmapReader.present("F000000000000000" + "4000000000000001"));
        assertEquals(List.of(), BitmapReader.present("0000000000000000"));
        assertThrows(IllegalArgumentException.class, () -> BitmapReader.present("70200000000000G0"));
    }

    @Test
    void fieldSixtyFiveIsTheSecondarysFirstBit() {
        assertEquals(List.of(2, 65), BitmapReader.present("C000000000000000" + "8000000000000000"));
    }

    @Test
    void refusesALengthThatDisagreesWithBitOne() {
        // Bit 1 set, but no secondary bitmap follows.
        assertThrows(IllegalArgumentException.class, () -> BitmapReader.present("F000000000000000"));
        // Bit 1 clear, yet sixteen more digits follow.
        assertThrows(IllegalArgumentException.class,
            () -> BitmapReader.present("7000000000000000" + "8000000000000000"));
    }
}

package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.jpos.iso.ISOMsg;
import org.junit.jupiter.api.Test;

class BitmapCheckTest {

    private static ISOMsg carrying(int... fields) throws Exception {
        ISOMsg message = new ISOMsg();
        message.setMTI("0100");
        for (int field : fields) {
            message.set(field, "1");
        }
        return message;
    }

    @Test
    void listsEveryFieldTheBitmapAndTheMessageDisagreeOn() throws Exception {
        // The bitmap claims 2, 3, 4 and 11; the message carries 2, 3, 4 and 7.
        assertEquals(List.of(7, 11), BitmapCheck.mismatches("7020000000000000", carrying(2, 3, 4, 7)));
        assertEquals(List.of(), BitmapCheck.mismatches("7020000000000000", carrying(2, 3, 4, 11)));
    }

    @Test
    void neverReportsBitOne() throws Exception {
        // Bit 1 set (a secondary bitmap follows), and fields 2, 3 and 4.
        assertEquals(List.of(), BitmapCheck.mismatches("F000000000000000", carrying(2, 3, 4)));
    }

    @Test
    void refusesABitmapThatIsNotSixteenHexDigits() throws Exception {
        ISOMsg message = carrying(2);
        assertThrows(IllegalArgumentException.class, () -> BitmapCheck.mismatches("4000", message));
        assertThrows(IllegalArgumentException.class,
            () -> BitmapCheck.mismatches("40000000000000000", message));
    }
}

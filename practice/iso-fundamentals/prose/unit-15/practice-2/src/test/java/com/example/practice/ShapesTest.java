package com.example.practice;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShapesTest {

    /** A purchase as the host received it, with the values that differ from one purchase to the next. */
    private static ISOMsg purchase(String pan, String amount, String stan, String time, String terminal)
            throws Exception {
        ISOMsg m = new ISOMsg("0200");
        m.set(2, pan);
        m.set(3, "000000");
        m.set(4, amount);
        m.set(7, "1212" + time);
        m.set(11, stan);
        m.set(22, "051");
        m.set(37, "634612" + stan);
        m.set(41, terminal);
        m.set(42, "000000004470021");
        m.set(43, "HARBOUR BOOKS            LEITH        GB");
        m.set(49, "826");
        return m;
    }

    private static List<ISOMsg> approved() throws Exception {
        return List.of(
                purchase("4111111111111111", "000000001299", "000214", "081503", "29110061"),
                purchase("5555555555554444", "000000064000", "000215", "082247", "29110062"),
                purchase("4111111111111111", "000000000450", "000377", "090012", "29110061"));
    }

    @Test
    void reportsTheFieldsWhoseShapeNoApprovedMessageHas() throws Exception {
        ISOMsg rejected = purchase("5555555555554444", "000000002210", "000378", "091530", "29110063");
        rejected.unset(49);                         // the currency went missing
        rejected.set(12, "0915");                   // the local time lost its seconds
        assertEquals(List.of(12, 49), Shapes.unseen(approved(), rejected));
    }

    @Test
    void aRejectedMessageShapedLikeTheApprovedOnesGivesNothing() throws Exception {
        ISOMsg rejected = purchase("5555555555554444", "000000002210", "000378", "091530", "29110063");
        assertEquals(List.of(), Shapes.unseen(approved(), rejected), "different values, same shapes");
    }

    @Test
    void aLetterInAFieldOfDigitsIsReported() throws Exception {
        ISOMsg rejected = purchase("4111111111111111", "0000000022O0", "000378", "091530", "29110063");
        assertEquals(List.of(4), Shapes.unseen(approved(), rejected), "letter O typed for a zero");
    }

    @Test
    void aCharacterOutsideAsciiIsReported() throws Exception {
        ISOMsg rejected = purchase("4111111111111111", "000000002210", "000378", "091530", "29110063");
        rejected.set(43, "BRASSERIE L\u00c9ON           LEITH        GB");
        assertEquals(List.of(43), Shapes.unseen(approved(), rejected));
    }

    @Test
    void aBinaryFieldIsComparedByItsLength() throws Exception {
        List<ISOMsg> approved = approved();
        approved.get(0).set(52, ISOUtil.hex2byte("0412659876543210"));
        approved.get(1).set(52, ISOUtil.hex2byte("3047158829610354"));
        approved.get(2).set(52, ISOUtil.hex2byte("8821904736251049"));
        ISOMsg rejected = purchase("4111111111111111", "000000002210", "000378", "091530", "29110063");
        rejected.set(52, ISOUtil.hex2byte("A3F09C1BE7D24F65"));
        assertEquals(List.of(), Shapes.unseen(approved, rejected), "another PIN block of the same length");
        rejected.set(52, ISOUtil.hex2byte("A3F09C1BE7D24F"));
        assertEquals(List.of(52), Shapes.unseen(approved, rejected), "a PIN block one byte short");
    }

    @Test
    void aShapeOneApprovedMessageHasIsNotReported() throws Exception {
        List<ISOMsg> approved = approved();
        approved.get(2).set(2, "4111111111111111003");   // a 19-digit card was approved too
        ISOMsg rejected = purchase("6011000990139424004", "000000002210", "000378", "091530", "29110063");
        assertEquals(List.of(), Shapes.unseen(approved, rejected));
    }

    @Test
    void aFieldSomeApprovedMessagesLackIsNotReportedWhenItIsMissing() throws Exception {
        List<ISOMsg> approved = approved();
        approved.get(1).set(14, "2812");
        approved.get(2).set(14, "2907");                 // the first approved purchase carried no expiry
        ISOMsg rejected = purchase("4111111111111111", "000000002210", "000378", "091530", "29110063");
        assertEquals(List.of(), Shapes.unseen(approved, rejected));
        rejected.set(14, "29071");
        assertEquals(List.of(14), Shapes.unseen(approved, rejected), "an expiry of five digits");
    }
}

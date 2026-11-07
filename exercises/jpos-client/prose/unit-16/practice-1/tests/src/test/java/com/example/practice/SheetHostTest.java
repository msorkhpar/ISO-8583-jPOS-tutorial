package com.example.practice;

import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOSource;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SheetHostTest {

    static final class Client implements ISOSource {
        final List<ISOMsg> sent = new ArrayList<>();
        @Override public void send(ISOMsg m) { sent.add(m); }
        @Override public boolean isConnected() { return true; }
    }

    private static ISOMsg purchase(String pan, String amount, String stan) throws Exception {
        ISOMsg m = new ISOMsg("0200");
        m.set(2, pan);
        m.set(3, "000000");
        m.set(4, amount);
        m.set(11, stan);
        m.set(41, "29110002");
        return m;
    }

    /** The rc the host answered with, "none" when it sent nothing. */
    private static String answer(SheetHost host, ISOMsg request) {
        Client client = new Client();
        host.process(client, request);
        if (client.sent.isEmpty()) return "none";
        assertEquals(1, client.sent.size());
        ISOMsg r = client.sent.get(0);
        assertEquals("0210", r.getString(0));
        return r.getString(39);
    }

    @Test
    void aCardRowAnswersThatCard() throws Exception {
        SheetHost host = new SheetHost(List.of(
            "C03;0200;5555555555554444;;;54",
            "C09;0200;4111111111111111;;;00"));
        assertEquals("54", answer(host, purchase("5555555555554444", "000000004200", "000811")));
        assertEquals("00", answer(host, purchase("4111111111111111", "000000004200", "000812")));
    }

    @Test
    void anAmountInsideARangeGetsThatRowsRc() throws Exception {
        SheetHost host = new SheetHost(List.of(
            "C20;0100;;;;12",
            "C24;0200;;9.00;10.00;13",
            "C21;0200;;10.00;20.00;00",
            "C22;0200;;30.00;50.00;61",
            "C23;0200;;;5.00;14"));
        assertEquals("14", answer(host, purchase("4111111111111111", "000000000400", "000820")));
        assertEquals("13", answer(host, purchase("4111111111111111", "000000000950", "000824")));
        assertEquals("00", answer(host, purchase("4111111111111111", "000000001500", "000821")));
        assertEquals("61", answer(host, purchase("4111111111111111", "000000004000", "000822")));
        assertEquals("none", answer(host, purchase("4111111111111111", "000000002500", "000823")));
    }

    @Test
    void aSilentRowAnswersNothingButHandlesTheRequest() throws Exception {
        SheetHost host = new SheetHost(List.of("C31;0200;;12.00;13.00;--"));
        Client client = new Client();
        assertTrue(host.process(client, purchase("4111111111111111", "000000001234", "000831")));
        assertTrue(client.sent.isEmpty());
        assertFalse(host.process(client, purchase("4111111111111111", "000000009900", "000832")));
    }

    @Test
    void theFirstRowThatAppliesAnswers() throws Exception {
        SheetHost host = new SheetHost(List.of(
            "C05;0200;5555555555554444;;;54",
            "C30;0200;;;;00"));
        assertEquals("54", answer(host, purchase("5555555555554444", "000000003300", "000841")));
        assertEquals("00", answer(host, purchase("4111111111111111", "000000003300", "000842")));

        SheetHost catchAllFirst = new SheetHost(List.of(
            "C30;0200;;;;00",
            "C05;0200;5555555555554444;;;54"));
        assertEquals("00", answer(catchAllFirst, purchase("5555555555554444", "000000003300", "000843")));
    }

    @Test
    void bothEndsOfARangeBelongToIt() throws Exception {
        SheetHost host = new SheetHost(List.of(
            "C42;0200;;41.20;41.20;62",
            "C40;0200;;30.00;50.00;61",
            "C41;0200;;;;00"));
        assertEquals("62", answer(host, purchase("4111111111111111", "000000004120", "000850")));
        assertEquals("00", answer(host, purchase("4111111111111111", "000000002999", "000851")));
        assertEquals("61", answer(host, purchase("4111111111111111", "000000003000", "000852")));
        assertEquals("61", answer(host, purchase("4111111111111111", "000000005000", "000853")));
        assertEquals("00", answer(host, purchase("4111111111111111", "000000005001", "000854")));
    }

    @Test
    void amountsWithCentsAreReadExactly() throws Exception {
        SheetHost host = new SheetHost(List.of(
            "C50;0200;;19.99;64.10;61",
            "C51;0200;;;;00"));
        assertEquals("00", answer(host, purchase("4111111111111111", "000000001998", "000861")));
        assertEquals("61", answer(host, purchase("4111111111111111", "000000001999", "000862")));
        assertEquals("61", answer(host, purchase("4111111111111111", "000000006410", "000863")));
        assertEquals("00", answer(host, purchase("4111111111111111", "000000006411", "000864")));
    }

    @Test
    void aBlankUpperBoundHasNoLimit() throws Exception {
        SheetHost host = new SheetHost(List.of(
            "C60;0200;;250.01;;51",
            "C61;0200;;;;00"));
        assertEquals("00", answer(host, purchase("4111111111111111", "000000025000", "000871")));
        assertEquals("51", answer(host, purchase("4111111111111111", "000000025001", "000872")));
        assertEquals("51", answer(host, purchase("4111111111111111", "000099999999", "000873")));
    }

    @Test
    void aRowThatCannotBeReadRefusesTheSheet() {
        for (String bad : List.of(
                "C70;0200;;25O.00;;51",           // a letter O
                "C71;0200;;250;;51",              // no cents: units or minor units?
                "C72;0200;;64.10;19.99;51",       // a range nothing can fall in
                "C73;0200;;;;5",                  // a one-digit rc
                "C74;0200;;;25O.00;51",           // the same, in the upper bound
                "C75;0200;;10.00;250;51",
                "C76;0200;;1,000.00;;51",         // a thousands separator
                "C77;0200;;100.00;20.00;51",      // empty, though "100.00" < "20.00" as text
                "C78;0200;;250.5;;51",            // one decimal
                "C79;0200;;-1.00;;51",
                "C80;0200;;;;",                   // no rc at all
                "C81;0200;;;;-5"))
            assertThrows(IllegalArgumentException.class,
                () -> new SheetHost(List.of("C01;0200;4111111111111111;;;00", bad)), bad);
    }
}

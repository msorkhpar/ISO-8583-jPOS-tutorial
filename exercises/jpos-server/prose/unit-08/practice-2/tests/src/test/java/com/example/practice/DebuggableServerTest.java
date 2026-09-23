package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.List;
import org.jpos.iso.ISOMsg;
import org.jpos.util.LogEvent;
import org.jpos.util.Logger;
import org.junit.jupiter.api.Test;

class DebuggableServerTest {

    private final List<LogEvent> seen = new ArrayList<>();

    private Logger capturing() {
        Logger logger = new Logger();
        logger.addListener(event -> {
            seen.add(event);
            return event;
        });
        return logger;
    }

    private static ISOMsg purchase(boolean withAmount) throws Exception {
        ISOMsg msg = new ISOMsg();
        msg.setMTI("0200");
        if (withAmount) {
            msg.set(4, "000000010000");
        }
        return msg;
    }

    private List<String> tags() {
        List<String> tags = new ArrayList<>();
        seen.forEach(event -> tags.add(event.getTag()));
        return tags;
    }

    @Test
    void withDebugOnADebugEventPrecedesTheProcessingEvent() throws Exception {
        DebuggableServer server = new DebuggableServer(capturing());
        server.processTransaction(purchase(true));
        assertEquals(List.of("transaction.process"), tags());
        seen.clear();
        server.setDebugMode(true);
        server.processTransaction(purchase(true));
        assertEquals(List.of("transaction.debug", "transaction.process"), tags());
        assertEquals(List.of("MTI: 0200", "Amount: 000000010000"), seen.get(0).getPayLoad());
        assertEquals(List.of("Processing transaction: 0200"), seen.get(1).getPayLoad());
    }

    @Test
    void switchingDebugOffStopsTheDebugEvents() throws Exception {
        DebuggableServer server = new DebuggableServer(capturing());
        server.setDebugMode(true);
        server.setDebugMode(false);
        server.processTransaction(purchase(true));
        assertEquals(List.of("transaction.process"), tags());
    }

    @Test
    void aMissingAmountIsSaidNotPrintedAsNull() throws Exception {
        DebuggableServer server = new DebuggableServer(capturing());
        server.setDebugMode(true);
        server.processTransaction(purchase(false));
        assertEquals(List.of("MTI: 0200", "Amount: none"), seen.get(0).getPayLoad());
        for (Object line : seen.get(0).getPayLoad()) {
            assertFalse(String.valueOf(line).contains("null"));
        }
    }
}

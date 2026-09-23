package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.util.LogEvent;
import org.jpos.util.Logger;
import org.junit.jupiter.api.Test;

class TransactionProcessorTest {

    private final List<LogEvent> seen = new ArrayList<>();

    private Logger capturing() {
        Logger logger = new Logger();
        logger.addListener(event -> {
            seen.add(event);
            return event;
        });
        return logger;
    }

    @Test
    void aGoodMessageLogsOneEventSayingItWasProcessed() throws Exception {
        ISOMsg msg = new ISOMsg();
        msg.setMTI("0200");
        assertTrue(new TransactionProcessor(capturing()).process(msg));
        assertEquals(1, seen.size());
        assertEquals("transaction.process", seen.get(0).getTag());
        assertEquals(
            List.of("Processing transaction: 0200", "Transaction processed successfully"),
            seen.get(0).getPayLoad());
    }

    @Test
    void aMessageThatFailsStillLogsExactlyOneEvent() {
        assertFalse(new TransactionProcessor(capturing()).process(new ISOMsg()));
        assertEquals(1, seen.size());
        assertEquals("transaction.process", seen.get(0).getTag());
        List<Object> payload = seen.get(0).getPayLoad();
        assertEquals(1, payload.size());
        assertInstanceOf(ISOException.class, payload.get(0));
    }
}

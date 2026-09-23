package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.jpos.iso.ISOMsg;
import org.jpos.util.LogEvent;
import org.jpos.util.Logger;
import org.junit.jupiter.api.Test;

class TransactionLoggerTest {

    // A standard test card number: no bank issues it.
    private static final String PAN = "4111111111111111";

    private final List<LogEvent> seen = new ArrayList<>();

    private Logger capturing() {
        Logger logger = new Logger();
        logger.addListener(event -> {
            seen.add(event);
            return event;
        });
        return logger;
    }

    private static ISOMsg purchase(boolean withPan) throws Exception {
        ISOMsg msg = new ISOMsg();
        msg.setMTI("0200");
        if (withPan) {
            msg.set(2, PAN);
        }
        msg.set(4, "000000010000");
        return msg;
    }

    private List<String> lines() {
        List<String> lines = new ArrayList<>();
        for (Object message : seen.get(0).getPayLoad()) {
            lines.add(String.valueOf(message));
        }
        return lines;
    }

    @Test
    void logsTheMtiAndTheAmountInOneTransactionEvent() throws Exception {
        new TransactionLogger(capturing()).logTransaction(purchase(true));
        assertEquals(1, seen.size());
        assertEquals("Transaction", seen.get(0).getTag());
        assertEquals("MTI: 0200", lines().get(0));
        assertTrue(lines().contains("Amount: 000000010000"));
    }

    @Test
    void thePanIsMaskedToItsFirstSixAndLastFourDigits() throws Exception {
        new TransactionLogger(capturing()).logTransaction(purchase(true));
        assertTrue(lines().contains("PAN: 411111******1111"));
        for (String line : lines()) {
            assertFalse(line.contains(PAN));
        }
    }

    @Test
    void aMessageWithoutAPanLogsNoPanLine() throws Exception {
        new TransactionLogger(capturing()).logTransaction(purchase(false));
        for (String line : lines()) {
            assertFalse(line.startsWith("PAN"));
        }
    }
}

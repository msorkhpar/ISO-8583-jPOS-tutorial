package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.jpos.util.LogEvent;
import org.jpos.util.Logger;
import org.junit.jupiter.api.Test;

class ClientLoggerTest {

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
    void oneCallDeliversOneEventWithEveryMessageInOrder() {
        new ClientLogger(capturing(), "client").log("transaction", "approved", "stan 000001");
        assertEquals(1, seen.size());
        assertEquals("transaction", seen.get(0).getTag());
        assertEquals("client", seen.get(0).getRealm());
        assertEquals(List.of("approved", "stan 000001"), seen.get(0).getPayLoad());
    }

    @Test
    void blankMessagesAreLeftOut() {
        new ClientLogger(capturing(), "client").log("transaction", "approved", " ", null, "stan 000001");
        assertEquals(1, seen.size());
        assertEquals(List.of("approved", "stan 000001"), seen.get(0).getPayLoad());
    }

    @Test
    void aCallWithNothingToSayLogsNothing() {
        new ClientLogger(capturing(), "client").log("transaction", " ", null);
        assertEquals(0, seen.size());
    }
}

package com.example.practice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;
import org.jpos.iso.ISOMsg;
import org.jpos.util.LogEvent;
import org.jpos.util.LogSource;
import org.jpos.util.Logger;
import org.jpos.util.SimpleLogSource;
import org.junit.jupiter.api.Test;

class TransactionRecorderTest {

    private static ISOMsg message(String mti, String stan) throws Exception {
        ISOMsg m = new ISOMsg();
        m.setMTI(mti);
        m.set(11, stan);
        return m;
    }

    @Test
    void recordsTheMessagesOfTransactionEventsOnly() throws Exception {
        Logger logger = new Logger();
        TransactionRecorder recorder = new TransactionRecorder();
        logger.addListener(recorder);
        LogSource source = new SimpleLogSource(logger, "switch");
        ISOMsg first = message("0100", "000001");
        ISOMsg echo = message("0800", "000002");
        ISOMsg second = message("0110", "000001");
        Logger.log(new LogEvent(source, "TX", first));
        Logger.log(new LogEvent(source, "info", echo));
        Logger.log(new LogEvent(source, "TX", second));
        List<ISOMsg> kept = recorder.recorded();
        assertEquals(2, kept.size());
        assertSame(first, kept.get(0));
        assertSame(second, kept.get(1));
    }

    @Test
    void handsEveryEventOnToTheNextListener() throws Exception {
        Logger logger = new Logger();
        TransactionRecorder recorder = new TransactionRecorder();
        logger.addListener(recorder);
        List<String> after = new ArrayList<>();
        logger.addListener(event -> {
            after.add(event.getTag());
            return event;
        });
        LogSource source = new SimpleLogSource(logger, "switch");
        Logger.log(new LogEvent(source, "TX", message("0100", "000001")));
        Logger.log(new LogEvent(source, "info", "channel connected"));
        assertEquals(List.of("TX", "info"), after);
        assertEquals(1, recorder.recorded().size());
    }

    @Test
    void skipsPayloadThatIsNotAMessage() throws Exception {
        Logger logger = new Logger();
        TransactionRecorder recorder = new TransactionRecorder();
        logger.addListener(recorder);
        LogSource source = new SimpleLogSource(logger, "switch");
        ISOMsg request = message("0100", "000001");
        LogEvent event = new LogEvent(source, "TX", "received from acquirer");
        event.addMessage(request);
        Logger.log(event);
        assertEquals(List.of(request), recorder.recorded());
    }
}

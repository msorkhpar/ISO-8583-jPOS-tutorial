package com.example.practice;

import java.util.ArrayList;
import java.util.List;
import org.jpos.util.LogEvent;
import org.jpos.util.LogSource;
import org.jpos.util.Logger;
import org.jpos.util.SimpleLogSource;

/** A client's own logger: one event per call, delivered to the logger it was built with. */
public final class ClientLogger {

    private final LogSource source;

    public ClientLogger(Logger logger, String realm) {
        this.source = new SimpleLogSource(logger, realm);
    }

    /** Log one event tagged `tag`, carrying the messages that say something. */
    public void log(String tag, String... messages) {
        List<String> kept = new ArrayList<>();
        for (String message : messages) {
            if (message != null && !message.isBlank()) {
                kept.add(message);
            }
        }
        LogEvent event = new LogEvent(source, tag);
        kept.forEach(event::addMessage);
        Logger.log(event);
    }
}

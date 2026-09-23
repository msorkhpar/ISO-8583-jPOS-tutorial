package com.example.practice;

import org.jpos.util.Logger;

/** A client's own logger: one event per call, delivered to the logger it was built with. */
public final class ClientLogger {

    public ClientLogger(Logger logger, String realm) {
    }

    /** Log one event tagged `tag`, carrying the messages that say something. */
    public void log(String tag, String... messages) {
        throw new UnsupportedOperationException("write log");
    }
}

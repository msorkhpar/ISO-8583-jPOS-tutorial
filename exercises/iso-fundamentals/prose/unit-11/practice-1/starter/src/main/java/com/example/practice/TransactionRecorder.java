package com.example.practice;

import java.util.List;
import org.jpos.iso.ISOMsg;
import org.jpos.util.LogEvent;
import org.jpos.util.LogListener;

/** A transaction log: keeps the messages of the events tagged TX. */
public final class TransactionRecorder implements LogListener {

    @Override
    public LogEvent log(LogEvent event) {
        throw new UnsupportedOperationException("write log");
    }

    /** The recorded messages, in arrival order. */
    public List<ISOMsg> recorded() {
        throw new UnsupportedOperationException("write recorded");
    }
}

package com.example.practice;

import java.util.ArrayList;
import java.util.List;
import org.jpos.iso.ISOMsg;
import org.jpos.util.LogEvent;
import org.jpos.util.LogListener;

/** A transaction log: keeps the messages of the events tagged TX. */
public final class TransactionRecorder implements LogListener {

    private final List<ISOMsg> recorded = new ArrayList<>();

    @Override
    public LogEvent log(LogEvent event) {
        if ("TX".equals(event.getTag())) {
            for (Object item : event.getPayLoad()) {
                if (item instanceof ISOMsg) {
                    recorded.add((ISOMsg) item);
                }
            }
        }
        return event;
    }

    /** The recorded messages, in arrival order. */
    public List<ISOMsg> recorded() {
        return List.copyOf(recorded);
    }
}

package com.example.practice;

import org.jpos.iso.ISOMsg;
import org.jpos.space.LocalSpace;
import org.jpos.space.SpaceListener;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Hands every reversal queued for a link to that link's sender. Participants queue the reversals
 * (an ISOMsg each) on the queue this forwarder listens to.
 */
public class ReversalForwarder implements SpaceListener<String, Object> {
    private final LocalSpace<String, Object> sp;
    private final Consumer<ISOMsg> sender;            // talks to the host: may take seconds
    private final ExecutorService worker = Executors.newSingleThreadExecutor();

    public ReversalForwarder(LocalSpace<String, Object> sp, String queue, Consumer<ISOMsg> sender) {
        this.sp = sp;
        this.sender = sender;
        sp.addListener(queue, this);
    }

    @Override
    public void notify(String queue, Object value) {
        Object taken = sp.inp(queue);
        if (taken instanceof ISOMsg)
            worker.execute(() -> sender.accept((ISOMsg) taken));
    }

    /** Stops the forwarder once everything it took has been handed to the sender. */
    public void close() throws InterruptedException {
        worker.shutdown();
        worker.awaitTermination(30, TimeUnit.SECONDS);
    }
}

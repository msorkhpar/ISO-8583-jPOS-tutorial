package com.example.practice;

import java.util.function.Supplier;
import org.jpos.iso.ISOChannel;

/** Keeps channels connected between uses and lends them out one piece of work at a time. */
public final class ChannelPool implements AutoCloseable {

    /** One piece of work done with a borrowed channel. */
    public interface ChannelWork<T> {
        T apply(ISOChannel channel) throws Exception;
    }

    public ChannelPool(Supplier<ISOChannel> newChannel, int maxChannels) {
    }

    public <T> T withChannel(ChannelWork<T> work) throws Exception {
        throw new UnsupportedOperationException("write withChannel");
    }

    public int active() {
        throw new UnsupportedOperationException("write active");
    }

    public int idle() {
        throw new UnsupportedOperationException("write idle");
    }

    @Override
    public void close() {
        throw new UnsupportedOperationException("write close");
    }
}

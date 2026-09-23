package com.example.practice;

import java.util.function.Supplier;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.jpos.iso.ISOChannel;

/** Keeps channels connected between uses and lends them out one piece of work at a time. */
public final class ChannelPool implements AutoCloseable {

    /** One piece of work done with a borrowed channel. */
    public interface ChannelWork<T> {
        T apply(ISOChannel channel) throws Exception;
    }

    private final GenericObjectPool<ISOChannel> pool;

    public ChannelPool(Supplier<ISOChannel> newChannel, int maxChannels) {
        pool = new GenericObjectPool<>(new Channels(newChannel));
        pool.setMaxTotal(maxChannels);
    }

    public <T> T withChannel(ChannelWork<T> work) throws Exception {
        ISOChannel channel = pool.borrowObject();
        try {
            return work.apply(channel);
        } finally {
            pool.returnObject(channel);
        }
    }

    public int active() {
        return pool.getNumActive();
    }

    public int idle() {
        return pool.getNumIdle();
    }

    @Override
    public void close() {
        pool.close();
    }

    private static final class Channels extends BasePooledObjectFactory<ISOChannel> {
        private final Supplier<ISOChannel> newChannel;

        Channels(Supplier<ISOChannel> newChannel) {
            this.newChannel = newChannel;
        }

        @Override
        public ISOChannel create() throws Exception {
            ISOChannel channel = newChannel.get();
            channel.connect();
            return channel;
        }

        @Override
        public PooledObject<ISOChannel> wrap(ISOChannel channel) {
            return new DefaultPooledObject<>(channel);
        }

        @Override
        public boolean validateObject(PooledObject<ISOChannel> p) {
            return p.getObject().isConnected();
        }

        @Override
        public void destroyObject(PooledObject<ISOChannel> p) throws Exception {
            p.getObject().disconnect();
        }
    }
}

package com.example.practice;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.space.Space;

import java.util.concurrent.TimeoutException;

/**
 * What the test host received. The host puts every message it receives on one Space queue, in
 * the order they arrive; tests read them from here.
 */
public class HostLog {
    private final Space<String, Object> sp;
    private final String queue;

    public HostLog(Space<String, Object> sp, String queue) {
        this.sp = sp;
        this.queue = queue;
    }

    /**
     * The next message of the given type the host receives, taken from the log. Messages of other
     * types that arrive meanwhile are dropped from the log.
     */
    public ISOMsg awaitReceived(String mti, long timeoutMillis)
            throws TimeoutException, InterruptedException, ISOException {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        for (long left = timeoutMillis; left > 0; left = deadline - System.currentTimeMillis()) {
            ISOMsg m = (ISOMsg) sp.in(queue, left);
            if (m == null)
                break;
            if (mti.equals(m.getMTI()))
                return m;
        }
        throw new TimeoutException("the host received no " + mti + " within " + timeoutMillis + " ms");
    }
}

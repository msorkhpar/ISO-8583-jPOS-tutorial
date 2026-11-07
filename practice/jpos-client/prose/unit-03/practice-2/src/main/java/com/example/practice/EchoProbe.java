package com.example.practice;

import org.jpos.iso.ISOChannel;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;

import java.io.IOException;
import java.util.function.LongSupplier;

/** A diagnostic tool: sends one echo over a connected channel and says what became of it. */
public class EchoProbe {
    public enum Result { UP, NO_ANSWER, LINK_DOWN }

    private final ISOChannel channel;
    private final LongSupplier clock;                  // milliseconds; System::currentTimeMillis in production

    public EchoProbe(ISOChannel channel, LongSupplier clock) {
        this.channel = channel;
        this.clock = clock;
    }

    /** Sends an echo with this STAN and reports within budgetMillis whether the host answered it. */
    public Result check(String stan, long budgetMillis) throws IOException, ISOException {
        // TODO: send echo(stan) on the channel and decide which Result it ends with. The operator
        //       runs this against a live link, and must get an answer within budgetMillis
        //       (as measured by clock), whatever the host does meanwhile. (about 12 lines)
        throw new IllegalStateException("TODO: check");
    }

    /** True when m is the host's answer to our echo with this STAN. */
    boolean isAnswerTo(ISOMsg m, String stan) throws ISOException {
        // TODO: one line
        throw new IllegalStateException("TODO: isAnswerTo");
    }

    static ISOMsg echo(String stan) throws ISOException {
        ISOMsg m = new ISOMsg("0800");
        m.set(11, stan);
        m.set(70, "301");
        return m;
    }
}

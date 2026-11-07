package com.example.practice;

import org.jpos.iso.ISOChannel;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;

import java.io.IOException;
import java.net.SocketTimeoutException;
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
        long deadline = clock.getAsLong() + budgetMillis;
        try {
            channel.send(echo(stan));
            while (clock.getAsLong() < deadline) {
                if (isAnswerTo(channel.receive(), stan))
                    return Result.UP;
            }
            return Result.NO_ANSWER;
        } catch (SocketTimeoutException e) {
            return Result.NO_ANSWER;
        } catch (IOException e) {
            return Result.LINK_DOWN;
        }
    }

    /** True when m is the host's answer to our echo with this STAN. */
    boolean isAnswerTo(ISOMsg m, String stan) throws ISOException {
        return "0810".equals(m.getMTI());
    }

    static ISOMsg echo(String stan) throws ISOException {
        ISOMsg m = new ISOMsg("0800");
        m.set(11, stan);
        m.set(70, "301");
        return m;
    }
}

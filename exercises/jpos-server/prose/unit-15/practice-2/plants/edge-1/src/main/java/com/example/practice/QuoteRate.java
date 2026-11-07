package com.example.practice;

import org.jpos.core.Configurable;
import org.jpos.core.Configuration;
import org.jpos.iso.ISOMsg;
import org.jpos.transaction.Context;
import org.jpos.transaction.TransactionParticipant;
import org.jpos.util.NameRegistrar;

import java.io.Serializable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.jpos.transaction.ContextConstants.REQUEST;

/**
 * Asks the exchange-rate service for a quote in the card's currency (field 49) and pauses the
 * transaction until the quote, a failure or this participant's own timeout arrives. A participant
 * after it decides; without a rate, no conversion is offered.
 */
public class QuoteRate implements TransactionParticipant, Configurable {

    /** The exchange-rate service. It has no timeout of its own and answers on its own thread. */
    public interface FxService {
        void quote(String currency, long amount, Listener listener);

        interface Listener {
            void quoted(long rateMicros);

            void failed(Exception e);
        }
    }

    public static final String RATE = "RATE";
    private static final ScheduledExecutorService TIMER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "quote-timeout");
        t.setDaemon(true);
        return t;
    });
    private String serviceName;
    private long timeout;

    @Override
    public void setConfiguration(Configuration cfg) {
        serviceName = cfg.get("service", "fx");
        timeout = cfg.getLong("timeout", 3000);
    }

    @Override
    public int prepare(long id, Serializable context) {
        Context ctx = (Context) context;
        ISOMsg request = ctx.get(REQUEST.toString());
        FxService fx = NameRegistrar.getIfExists(serviceName);
        if (fx == null || request == null || !request.hasField(49))
            return PREPARED | NO_JOIN | READONLY;                 // no quote, no offer
        Pending pending = new Pending(ctx);
        TIMER.schedule(pending::timedOut, timeout, TimeUnit.MILLISECONDS);
        fx.quote(request.getString(49), Long.parseLong(request.getString(4)), pending);
        return PREPARED | PAUSE | NO_JOIN;
    }

    /** One per transaction: the quote, a failure and the timeout of this transaction all arrive here. */
    static final class Pending implements FxService.Listener {
        private final Context ctx;

        Pending(Context ctx) {
            this.ctx = ctx;
        }

        /** The service quoted a rate, in millionths. */
        @Override
        public void quoted(long rateMicros) {
            ctx.put(RATE, rateMicros);
            ctx.resume();
        }

        /** The service could not quote. */
        @Override
        public void failed(Exception e) {
            ctx.log("no quote: " + e);
            ctx.resume();
        }

        /** The timeout of this participant ran out. */
        void timedOut() {
            ctx.log("no quote in time");
            ctx.resume();
        }
    }
}

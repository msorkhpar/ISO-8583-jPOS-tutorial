package com.example.practice;

import org.jpos.core.Configurable;
import org.jpos.core.Configuration;
import org.jpos.core.ConfigurationException;
import org.jpos.iso.ISOMsg;
import org.jpos.transaction.Context;
import org.jpos.transaction.ContextConstants;
import org.jpos.transaction.TransactionParticipant;
import org.jpos.util.NameRegistrar;

import java.io.Serializable;

/**
 * Asks the fraud-scoring service about each purchase and leaves the score in the Context under
 * SCORE, for the participant after it, which decides.
 */
public class ScoreFraud implements TransactionParticipant, Configurable {
    static final String SCORE = "SCORE";
    private Scorer scorer;
    private final Answers answers = new Answers();

    @Override
    public void setConfiguration(Configuration cfg) throws ConfigurationException {
        scorer = NameRegistrar.getIfExists(cfg.get("scorer", "scorer"));
        if (scorer == null)
            throw new ConfigurationException("no scorer registered");
    }

    @Override
    public int prepare(long id, Serializable context) {
        Context ctx = (Context) context;
        ISOMsg request = ctx.get(ContextConstants.REQUEST.toString());
        scorer.score(request, answers, ctx);
        return PREPARED | PAUSE | NO_JOIN;
    }

    /** What the scorer calls back, on its own thread. One instance serves every purchase. */
    final class Answers implements Scorer.Listener {
        @Override
        public void scored(int score, Object handBack) {
            Context ctx = (Context) handBack;
            ctx.put(SCORE, score);
            ctx.resume();
        }

        @Override
        public void unavailable(Object handBack) {
            // no score, nothing to record
        }
    }

    /** Given: the fraud-scoring service. It answers later, on a thread of its own. */
    public interface Scorer {
        /** Scores the request and later calls the listener once, with handBack. */
        void score(ISOMsg request, Listener listener, Object handBack);

        interface Listener {
            /** The score, from 0 (fine) to 999 (fraud). */
            void scored(int score, Object handBack);

            /** The service could not score this purchase. */
            void unavailable(Object handBack);
        }
    }
}

package com.example.practice;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;

/**
 * What the acquirer's switch does once a purchase it sent upstream has ended: it records the
 * outcome in its journal, queues whatever the host must still hear about, and tells the terminal.
 */
public class TimeoutHandler {

    /** What the journal says about a purchase, by its STAN. */
    public enum Status { APPROVED, DECLINED, REVERSING, LATE_APPROVAL }

    /** What the terminal is told. */
    public enum Outcome { APPROVED, DECLINED, FAILED }

    public interface Terminal {
        void tell(String stan, Outcome outcome);
    }

    /** The process died here. Everything committed before it is on disk; nothing else is. */
    public static final class Crash extends Error {
        public Crash() { super("process died"); }
    }

    /**
     * The switch's database: the journal and the store-and-forward queue, written in transactions.
     * A test can make the process die after a given number of steps; every commit is a step.
     */
    public static final class Db {
        private final Map<String, Status> journal = new LinkedHashMap<>();
        private final List<ISOMsg> queue = new ArrayList<>();
        private int steps;
        private int dieAfter = Integer.MAX_VALUE;

        public void dieAfterSteps(int n) { dieAfter = n; }

        /** Counts one step, and dies if this was the last one allowed. */
        public void step() {
            if (++steps >= dieAfter)
                throw new Crash();
        }

        public Tx begin() { return new Tx(); }

        public Status journal(String stan) { return journal.get(stan); }

        public List<ISOMsg> queue() { return List.copyOf(queue); }

        /** Changes that become real together, at commit, or not at all. */
        public final class Tx {
            private final List<Runnable> changes = new ArrayList<>();

            public void journal(String stan, Status status) { changes.add(() -> journal.put(stan, status)); }

            public void enqueue(ISOMsg m) { changes.add(() -> queue.add(m)); }

            /** Takes out the queued message whose field 90 names the given original STAN. */
            public void dequeue(String originalStan) {
                changes.add(() -> queue.removeIf(m -> m.getString(90).substring(4, 10).equals(originalStan)));
            }

            public void commit() {
                changes.forEach(Runnable::run);
                changes.clear();
                step();
            }
        }
    }

    private final Db db;
    private final Terminal terminal;

    public TimeoutHandler(Db db, Terminal terminal) {
        this.db = db;
        this.terminal = terminal;
    }

    /** The host answered in time. */
    public void onAnswer(ISOMsg purchase, ISOMsg response) {
        boolean approved = "00".equals(response.getString(39));
        Db.Tx tx = db.begin();
        tx.journal(purchase.getString(11), approved ? Status.APPROVED : Status.DECLINED);
        tx.commit();
        terminal.tell(purchase.getString(11), approved ? Outcome.APPROVED : Outcome.DECLINED);
    }

    /**
     * The host did not answer in time. The purchase may have been approved, so the host must get its
     * reversal (built by {@link #reversalOf}), and the terminal must hear that the purchase failed.
     */
    void onNoAnswer(ISOMsg purchase, String reversalStan, String now) throws ISOException {
        terminal.tell(purchase.getString(11), Outcome.FAILED);
        Db.Tx tx = db.begin();
        tx.enqueue(reversalOf(purchase, reversalStan, now));
        tx.journal(purchase.getString(11), Status.REVERSING);
        tx.commit();
    }

    /**
     * The host's answer came after {@link #onNoAnswer} had already run for this purchase.
     * An approval is recorded as {@link Status#LATE_APPROVAL}.
     */
    void onLateAnswer(ISOMsg response) {
        if (!"00".equals(response.getString(39)))
            return;
        Db.Tx tx = db.begin();
        tx.journal(response.getString(11), Status.LATE_APPROVAL);
        tx.commit();
    }

    /** A new 0420 with its own STAN and time that names the purchase in field 90. */
    static ISOMsg reversalOf(ISOMsg purchase, String stan, String now) throws ISOException {
        ISOMsg rev = (ISOMsg) purchase.clone(new int[] { 2, 3, 4, 32, 33, 37, 41, 42, 49 });
        rev.setMTI("0420");
        rev.set(7, now);
        rev.set(11, stan);
        rev.set(90, purchase.getMTI()
                + ISOUtil.zeropad(purchase.getString(11), 6)
                + ISOUtil.zeropad(purchase.getString(7), 10)
                + ISOUtil.zeropad(purchase.hasField(32) ? purchase.getString(32) : "", 11)
                + ISOUtil.zeropad(purchase.hasField(33) ? purchase.getString(33) : "", 11));
        return rev;
    }
}

package com.example.practice;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;

/**
 * The store-and-forward queue of one outbound link. A single sender asks {@link #due} what to send,
 * sends it, and reports back with {@link #acknowledged} or {@link #failed}. Times are milliseconds
 * on the caller's clock.
 */
public class LinkSaf {

    static final class Entry {
        final ISOMsg stored;          // as it was stored: never changed
        final long storedAt;
        int attempts;                 // attempts that went out and were not answered
        long firstTriedAt = -1;       // when the first of them failed, -1 before that
        long dueAt;                   // the earliest time the next attempt may go out

        Entry(ISOMsg stored, long storedAt) {
            this.stored = stored;
            this.storedAt = storedAt;
            this.dueAt = storedAt;
        }
    }

    private final Deque<Entry> pending = new ArrayDeque<>();
    private final List<ISOMsg> parked = new ArrayList<>();
    private final long base;
    private final long cap;
    private final int limit;
    private final long window;

    /**
     * @param baseMillis the wait after the first failure
     * @param capMillis the longest wait between two attempts
     * @param limit the most attempts the network allows for one advice
     * @param windowMillis how long after its first attempt an advice may still be tried
     */
    public LinkSaf(long baseMillis, long capMillis, int limit, long windowMillis) {
        this.base = baseMillis;
        this.cap = capMillis;
        this.limit = limit;
        this.window = windowMillis;
    }

    /** Queues an advice behind the others, due from {@code now}. The queue keeps its own copy. */
    public void store(ISOMsg advice, long now) {
        pending.addLast(new Entry((ISOMsg) advice.clone(), now));
    }

    /**
     * What the sender should send at {@code now}, or null when nothing may go out yet. The result is
     * a copy the sender may change; it is a repeat if an earlier attempt of the same advice failed.
     */
    ISOMsg due(long now) throws ISOException {
        Entry head = pending.peekFirst();
        if (head == null || head.dueAt > now)
            return null;
        ISOMsg m = (ISOMsg) head.stored.clone();
        if (head.attempts > 0)
            m.setRetransmissionMTI();
        return m;
    }

    /** The host answered what {@link #due} returned. */
    public void acknowledged() {
        pending.removeFirst();
    }

    /**
     * What {@link #due} returned went out at {@code now} and got no answer. Either the advice gets
     * another attempt later, or the network's limits are reached and it is parked for operations.
     */
    void failed(long now) {
        Entry head = pending.peekFirst();
        head.attempts++;
        if (head.firstTriedAt < 0)
            head.firstTriedAt = now;
        long next = now + waitAfter(head.attempts);
        if (head.attempts >= limit || next - head.firstTriedAt >= window) {
            parked.add(pending.removeFirst().stored);
            return;
        }
        head.dueAt = next;
    }

    /** The link has just come back up. */
    void linkUp(long now) {
        Entry head = pending.peekFirst();
        if (head != null && head.dueAt > now)
            head.dueAt = now;
    }

    /** How long to wait after the given number of failed attempts of one advice. */
    long waitAfter(int failures) {
        long w = base;
        for (int i = 1; i < failures && w < cap; i++)
            w *= 2;
        return Math.min(w, cap);
    }

    /** When the head may next go out, or -1 if the queue is empty. */
    public long nextDueAt() {
        Entry head = pending.peekFirst();
        return head == null ? -1 : head.dueAt;
    }

    public int size() {
        return pending.size();
    }

    /** Advices that reached the network's limits, as they were stored. */
    public List<ISOMsg> parked() {
        return List.copyOf(parked);
    }
}

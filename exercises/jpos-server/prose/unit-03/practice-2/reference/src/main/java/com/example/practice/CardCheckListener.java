package com.example.practice;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISORequestListener;
import org.jpos.iso.ISOSource;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Answers purchases itself after checking the card against the blocked-card table. */
public class CardCheckListener implements ISORequestListener {

    /** The blocked-card table. Under load a lookup takes seconds. */
    public interface CardTable {
        boolean isBlocked(String pan) throws Exception;
    }

    private final CardTable cards;
    private final ExecutorService workers;

    public CardCheckListener(CardTable cards) {
        this.cards = cards;
        this.workers = newWorkers();
    }

    @Override
    public boolean process(ISOSource source, ISOMsg m) {
        if (!isPurchase(m))
            return false;
        workers.execute(() -> answer(source, m));
        return true;
    }

    /** The threads that run the lookups, created once for the listener. */
    ExecutorService newWorkers() {
        return Executors.newFixedThreadPool(16);
    }

    /** Looks the card up and answers the purchase through the connection given. */
    void answer(ISOSource source, ISOMsg m) {
        try {
            ISOMsg r = (ISOMsg) m.clone();
            r.setResponseMTI();
            r.set(39, cards.isBlocked(m.getString(2)) ? "62" : "00");
            source.send(r);
        } catch (Exception e) {
            // no answer can be built or sent; the terminal times out and reverses
        }
    }

    static boolean isPurchase(ISOMsg m) {
        try {
            return "0200".equals(m.getMTI());
        } catch (ISOException e) {
            return false;
        }
    }
}

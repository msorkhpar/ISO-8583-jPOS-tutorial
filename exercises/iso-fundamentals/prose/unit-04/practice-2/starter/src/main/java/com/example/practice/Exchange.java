package com.example.practice;

import org.jpos.iso.ISOMsg;

/** What a response says about the request it answers. */
public final class Exchange {

    /** What the other side decided. */
    public enum Outcome { APPROVED, DECLINED }

    private Exchange() {
    }

    /** The outcome of {@code request}, as {@code response} reports it. */
    public static Outcome outcome(ISOMsg request, ISOMsg response) throws Exception {
        throw new UnsupportedOperationException("write outcome");
    }
}

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
        String asked = request.getMTI();
        if (asked == null || asked.length() != 4 || asked.charAt(2) != '0') {
            throw new IllegalArgumentException("not a request: " + asked);
        }
        String answered = response.getMTI();
        if (answered == null || answered.length() != 4 || answered.charAt(2) != '1') {
            throw new IllegalArgumentException(response.getMTI() + " does not answer " + asked);
        }
        String stan = request.getString(11);
        if (stan == null || !stan.equals(response.getString(11))) {
            throw new IllegalArgumentException("the response answers another STAN");
        }
        String code = response.getString(39);
        if (code == null) {
            throw new IllegalArgumentException("the response has no response code");
        }
        return "00".equals(code) ? Outcome.APPROVED : Outcome.DECLINED;
    }
}

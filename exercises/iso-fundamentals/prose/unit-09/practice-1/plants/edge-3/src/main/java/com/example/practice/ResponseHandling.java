package com.example.practice;

import java.io.IOException;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;

/** What a transaction's response, or its failure, calls for. */
public final class ResponseHandling {

    /** What the issuer decided. */
    public enum Outcome { APPROVED, DECLINED, REFER }

    /** What to do about a failure. */
    public enum Action { RETRY, LOG_FOR_FIX, ALERT_SECURITY }

    private ResponseHandling() {
    }

    /** The issuer's decision, read from field 39. */
    public static Outcome outcomeOf(ISOMsg response) {
        String code = response.getString(39);
        if ("00".equals(code) || "10".equals(code) || "85".equals(code)) {
            return Outcome.APPROVED;
        }
        if ("01".equals(code) || "02".equals(code)) {
            return Outcome.REFER;
        }
        return Outcome.DECLINED;
    }

    /** How to handle a failure: a network, a format or a security error. */
    public static Action actionFor(Exception failure) {
        if (failure.getClass() == IOException.class) {
            return Action.RETRY;
        }
        if (failure instanceof ISOException) {
            return Action.LOG_FOR_FIX;
        }
        if (failure instanceof SecurityException) {
            return Action.ALERT_SECURITY;
        }
        throw new IllegalArgumentException("not a network, format or security error", failure);
    }
}

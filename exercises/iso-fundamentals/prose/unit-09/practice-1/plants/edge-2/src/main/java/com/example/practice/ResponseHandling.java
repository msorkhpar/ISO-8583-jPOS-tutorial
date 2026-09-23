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
        switch (response.getString(39)) {
            case "00": case "10": case "85":
                return Outcome.APPROVED;
            case "01": case "02":
                return Outcome.REFER;
            default:
                return Outcome.DECLINED;
        }
    }

    /** How to handle a failure: a network, a format or a security error. */
    public static Action actionFor(Exception failure) {
        if (failure instanceof IOException) {
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

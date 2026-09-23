package com.example.practice;

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
        throw new UnsupportedOperationException("write outcomeOf");
    }

    /** How to handle a failure: a network, a format or a security error. */
    public static Action actionFor(Exception failure) {
        throw new UnsupportedOperationException("write actionFor");
    }
}

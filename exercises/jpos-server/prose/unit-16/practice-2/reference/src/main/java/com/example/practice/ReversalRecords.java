package com.example.practice;

import org.jpos.core.Configurable;
import org.jpos.core.Configuration;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;
import org.jpos.rc.CMF;
import org.jpos.space.Space;
import org.jpos.space.SpaceFactory;
import org.jpos.space.SpaceUtil;
import org.jpos.transaction.Context;
import org.jpos.transaction.ContextConstants;
import org.jpos.transaction.TransactionParticipant;
import org.jpos.util.Caller;

import java.io.Serializable;

/**
 * The switch's memory of what it sent upstream. This acquirer's terminals name the purchase they
 * reverse by their terminal ID (41) and the retrieval reference number (37) they gave it; their
 * reversals carry no field 90.
 */
public class ReversalRecords {
    /** What was sent upstream, and on which link. */
    record Sent(String link, ISOMsg message) implements Serializable { }

    static final Space<String, Object> SPACE = SpaceFactory.getSpace("tspace:switch-records");
    static final long LEASE = 60_000L;
    private static final int[] ECHOED = { 0, 2, 3, 4, 7, 11, 12, 13, 32, 37, 41, 42, 49 };

    /** Before QueryHost: the copy the network receives, numbered by the switch, remembered. */
    public static class Outbound implements TransactionParticipant, Configurable {
        private String acquirerId;

        @Override
        public void setConfiguration(Configuration cfg) {
            acquirerId = cfg.get("acquirer-id", "12345678901");
        }

        @Override
        public int prepare(long id, Serializable context) {
            Context ctx = (Context) context;
            ISOMsg request = ctx.get(ContextConstants.REQUEST.toString());
            String link = ctx.getString(ContextConstants.DESTINATION.toString());
            try {
                ISOMsg out = (ISOMsg) request.clone();
                out.set(11, nextStan(link));
                out.set(32, acquirerId);
                remember(request, link, out);
                ctx.put("OUTBOUND", out);
                return PREPARED | NO_JOIN;
            } catch (Exception e) {
                return ctx.getResult().fail(CMF.SYSTEM_ERROR, Caller.info(), "%s", e).FAIL();
            }
        }
    }

    /** After QueryHost: the network's approval, as the terminal's reply. */
    public static class Reply implements TransactionParticipant {
        @Override
        public int prepare(long id, Serializable context) {
            Context ctx = (Context) context;
            ISOMsg answer = ctx.get("HOST_RESPONSE");
            if (answer == null || !"00".equals(answer.getString(39)))
                return FAIL;
            try {
                ctx.put(ContextConstants.RESPONSE.toString(),
                    replyTo(ctx.get(ContextConstants.REQUEST.toString()), answer.getString(38)));
                return PREPARED | NO_JOIN;
            } catch (ISOException e) {
                return ctx.getResult().fail(CMF.SYSTEM_ERROR, Caller.info(), "%s", e).FAIL();
            }
        }
    }

    /** A terminal's reversal (0420, 0421): reverses upstream what was sent, and answers 00. */
    public static class ReverseFromTerminal implements TransactionParticipant {
        @Override
        public int prepare(long id, Serializable context) {
            Context ctx = (Context) context;
            ISOMsg reversal = ctx.get(ContextConstants.REQUEST.toString());
            try {
                reverse(reversal.getString(41), reversal.getString(37));
                ctx.put(ContextConstants.RESPONSE.toString(), replyTo(reversal, null));
            } catch (Exception e) {
                ctx.log(e);
            }
            return PREPARED | NO_JOIN;
        }
    }

    /** Before the request leaves: keeps what {@code link}'s network will see, for a later reversal. */
    static void remember(ISOMsg request, String link, ISOMsg outbound) throws ISOException {
        ISOMsg kept = (ISOMsg) outbound.clone();
        kept.unset(35, 45, 52, 55);
        SPACE.put("sent." + request.getString(41) + "." + request.getString(37), new Sent(link, kept), LEASE);
    }

    /**
     * Queues, on "reversal." + link, one reversal of what was sent upstream for the terminal's
     * purchase. Returns false when there is nothing to reverse.
     */
    static boolean reverse(String terminal, String rrn) throws ISOException {
        Sent sent = (Sent) SPACE.inp("sent." + terminal + "." + rrn);
        if (sent == null)
            return false;
        SPACE.out("reversal." + sent.link(), reversalOf(sent.message(), nextStan(sent.link())));
        return true;
    }

    /** A reversal of {@code original}: a new message on the link, pointing back through field 90. */
    static ISOMsg reversalOf(ISOMsg original, String stan) throws ISOException {
        ISOMsg rev = (ISOMsg) original.clone(2, 3, 4, 7, 32, 37, 41, 49);
        rev.setMTI("0420");
        rev.set(11, stan);
        rev.set(90, original.getMTI()
            + ISOUtil.zeropad(original.getString(11), 6)
            + ISOUtil.zeropad(original.getString(7), 10)
            + ISOUtil.zeropad(original.hasField(32) ? original.getString(32) : "", 11)
            + ISOUtil.zeropad("", 11));
        return rev;
    }

    /** One STAN sequence per upstream link. */
    static String nextStan(String link) {
        long n = SpaceUtil.nextLong(SPACE, "stan." + link);
        return ISOUtil.zeropad(((n - 1) % 999999) + 1, 6);
    }

    /** An answer to the terminal's own request: approval code {@code authCode}, or none. */
    static ISOMsg replyTo(ISOMsg request, String authCode) throws ISOException {
        ISOMsg r = (ISOMsg) request.clone(ECHOED);
        r.setResponseMTI();
        r.set(39, "00");
        if (authCode != null)
            r.set(38, authCode);
        return r;
    }
}

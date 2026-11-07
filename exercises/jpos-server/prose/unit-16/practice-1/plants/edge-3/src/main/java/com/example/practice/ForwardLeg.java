package com.example.practice;

import org.jpos.core.Configurable;
import org.jpos.core.Configuration;
import org.jpos.core.ConfigurationException;
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
import java.util.Map;

/**
 * The forwarding leg of the switch. {@link Outbound} stands before QueryHost and {@link Reply} after
 * it, in the group that forwards a terminal's request to the network named by DESTINATION.
 */
public class ForwardLeg {
    public static final String OUTBOUND = "OUTBOUND";
    public static final String HOST_RESPONSE = "HOST_RESPONSE";

    /** What a reply to a terminal echoes from the terminal's request (unit s2). */
    static final int[] ECHOED = { 0, 2, 3, 4, 7, 11, 12, 13, 32, 37, 41, 42, 49 };

    /** The network's declines, as causes; AnswerFailures turns a cause into the terminal's code. */
    static final Map<String, CMF> DECLINES = Map.of(
        "05", CMF.DO_NOT_HONOUR, "51", CMF.NOT_SUFFICIENT_FUNDS, "54", CMF.EXPIRED);

    /** Before QueryHost: puts under OUTBOUND the message the network receives. */
    public static class Outbound implements TransactionParticipant, Configurable {
        private Space<String, Object> space;
        private String terminalId;
        private String acquirerId;

        @Override
        public void setConfiguration(Configuration cfg) throws ConfigurationException {
            space = SpaceFactory.getSpace(cfg.get("space", "tspace:default"));
            terminalId = cfg.get("terminal-id");
            acquirerId = cfg.get("acquirer-id");
        }

        @Override
        public int prepare(long id, Serializable context) {
            Context ctx = (Context) context;
            ISOMsg request = ctx.get(ContextConstants.REQUEST.toString());
            String link = ctx.getString(ContextConstants.DESTINATION.toString());
            try {
                ctx.put(OUTBOUND, outbound(request, link, space, terminalId, acquirerId));
                return PREPARED | NO_JOIN;
            } catch (Exception e) {
                return ctx.getResult().fail(CMF.SYSTEM_ERROR, Caller.info(), "%s", e).FAIL();
            }
        }
    }

    /** After QueryHost: an approval becomes the terminal's reply; anything else is reported. */
    public static class Reply implements TransactionParticipant {
        @Override
        public int prepare(long id, Serializable context) {
            Context ctx = (Context) context;
            ISOMsg answer = ctx.get(HOST_RESPONSE);
            if (answer == null)
                return FAIL;                                  // QueryHost reported HOST_UNREACHABLE
            String code = answer.getString(39);
            if (!"00".equals(code))
                return ctx.getResult().fail(DECLINES.getOrDefault(code, CMF.SYSTEM_ERROR),
                    Caller.info(), "network answered %s", code).FAIL();
            try {
                ctx.put(ContextConstants.RESPONSE.toString(),
                    reply(ctx.get(ContextConstants.REQUEST.toString()), answer));
                return PREPARED | NO_JOIN;
            } catch (Exception e) {
                return ctx.getResult().fail(CMF.SYSTEM_ERROR, Caller.info(), "%s", e).FAIL();
            }
        }
    }

    /** The message the network receives for the terminal's {@code request}, sent on {@code link}. */
    static ISOMsg outbound(ISOMsg request, String link, Space<String, Object> space,
                           String terminalId, String acquirerId) throws ISOException {
        ISOMsg out = (ISOMsg) request.clone();
        out.unset(48);
        out.set(11, nextStan(space, link));
        out.set(32, acquirerId);
        out.set(41, terminalId);
        return out;
    }

    /** The approval the terminal receives, given the network's approving {@code answer}. */
    static ISOMsg reply(ISOMsg request, ISOMsg answer) throws ISOException {
        ISOMsg r = (ISOMsg) request.clone(ECHOED);
        r.setResponseMTI();
        r.set(39, "00");
        for (int f : new int[] { 38, 54 })
            if (answer.hasField(f))
                r.set(f, answer.getString(f));
        return r;
    }

    /** The STAN of the next message the switch sends on {@code link}. */
    static String nextStan(Space<String, Object> space, String link) {
        long n = SpaceUtil.nextLong(space, "stan.switch");
        return ISOUtil.zeropad(((n - 1) % 999999) + 1, 6);
    }
}

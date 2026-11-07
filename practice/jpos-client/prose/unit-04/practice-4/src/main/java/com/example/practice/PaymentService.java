package com.example.practice;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.MUX;
import org.jpos.q2.iso.QMUX;
import org.jpos.util.NameRegistrar;

/** Sends purchases through whatever Q2 has deployed under one name. */
public class PaymentService {
    public enum Result { APPROVED, DECLINED, NO_ANSWER, UNAVAILABLE }

    private final String muxName;
    private final long timeout;

    public PaymentService(String muxName, long timeout) {
        this.muxName = muxName;
        this.timeout = timeout;
    }

    public Result pay(ISOMsg purchase) throws ISOException {
        MUX mux = mux();
        if (mux == null || !mux.isConnected())
            return Result.UNAVAILABLE;
        ISOMsg response = mux.request(purchase, timeout);
        if (response == null)
            return Result.NO_ANSWER;
        return "00".equals(response.getString(39)) ? Result.APPROVED : Result.DECLINED;
    }

    /** What is deployed under muxName right now, or null when nothing is. */
    MUX mux() {
        // TODO: The service is created when the application starts, and it runs for weeks while
        //       operators deploy, redeploy and reorganise what sits under that name. (about five
        //       lines)
        throw new IllegalStateException("TODO: mux");
    }
}

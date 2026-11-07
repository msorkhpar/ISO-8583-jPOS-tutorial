package com.example.jpos;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.packager.ISO87APackager;

/**
 * Build a small 0100 authorization request with jPOS, and pack it to the wire.
 *
 * <p>It is here so the runner's warm compiles and tests something real against
 * the library every exercise in this corpus leans on. The account number is a
 * standard test value that no bank issues.
 */
public final class Authorization {

    private Authorization() {
    }

    /** An 0100 carrying a PAN, a processing code, an amount and a trace number. */
    public static ISOMsg request(String pan, String amount, String stan) throws ISOException {
        ISOMsg message = new ISOMsg();
        message.setPackager(new ISO87APackager());
        message.setMTI("0100");
        message.set(2, pan);
        message.set(3, "000000");
        message.set(4, amount);
        message.set(11, stan);
        return message;
    }

    /** The message as bytes on the wire, in the packager's ASCII ISO-8583:1987 layout. */
    public static byte[] pack(ISOMsg message) throws ISOException {
        return message.pack();
    }
}

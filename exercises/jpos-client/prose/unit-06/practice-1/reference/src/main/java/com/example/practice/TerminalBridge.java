package com.example.practice;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;

/**
 * Sits between the terminal application and the ISOMsg the client sends. The terminal speaks hex
 * strings; its PIN pad driver writes each PIN block into one buffer that it reuses.
 */
public class TerminalBridge {

    /** The PIN pad driver: writes the PIN block the cardholder just entered into {@code buffer}. */
    public interface PinPad {
        void readInto(byte[] buffer);
    }

    private final PinPad pinPad;
    private final byte[] buffer = new byte[8];

    public TerminalBridge(PinPad pinPad) {
        this.pinPad = pinPad;
    }

    /** The purchase for the cardholder at the terminal now. {@code iccHex} is null for a swiped card. */
    public ISOMsg purchase(String pan, long amountMinor, String stan, String iccHex)
            throws ISOException {
        ISOMsg m = new ISOMsg("0200");
        m.set(2, pan);
        m.set(3, "000000");
        m.set(4, ISOUtil.zeropad(Long.toString(amountMinor), 12));
        m.set(11, stan);
        m.set(41, "29110001");
        pinPad.readInto(buffer);
        putPinBlock(m, buffer);
        if (iccHex != null)
            putChipData(m, iccHex);
        return m;
    }

    /** Puts the chip data the terminal read, given as hex text, into the request. */
    void putChipData(ISOMsg request, String iccHex) {
        request.set(55, ISOUtil.hex2byte(iccHex));
    }

    /** Puts the PIN block the PIN pad produced into the request. */
    void putPinBlock(ISOMsg request, byte[] fromPinPad) {
        request.set(52, fromPinPad.clone());
    }

    /** The host's chip data (field 55 of the response) as the terminal wants it: hex text, or null. */
    String chipDataForTerminal(ISOMsg response) {
        byte[] icc = response.getBytes(55);
        return icc == null ? null : ISOUtil.hexString(icc);
    }
}

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
        // TODO: The terminal hands over what the chip produced as hex text, two characters per
        //       byte. The host must receive the chip's bytes, whatever packager the link uses.
        throw new IllegalStateException("TODO: putChipData");
    }

    /** Puts the PIN block the PIN pad produced into the request. */
    void putPinBlock(ISOMsg request, byte[] fromPinPad) {
        // TODO: The driver writes every cardholder's PIN block into the same buffer. A request may
        //       wait to be packed until after the next cardholder has typed a PIN.
        throw new IllegalStateException("TODO: putPinBlock");
    }

    /** The host's chip data (field 55 of the response) as the terminal wants it: hex text, or null. */
    String chipDataForTerminal(ISOMsg response) {
        // TODO: Responses come from links with different packagers, so field 55 may arrive as an
        //       ISOBinaryField or as an ISOField. The terminal wants the same hex either way, and
        //       null when the host sent no chip data.
        throw new IllegalStateException("TODO: chipDataForTerminal");
    }
}

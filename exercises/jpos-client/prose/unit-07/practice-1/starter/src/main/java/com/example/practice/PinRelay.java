package com.example.practice;

import java.util.Map;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.MUX;
import org.jpos.security.EncryptedPIN;
import org.jpos.security.SMAdapter;
import org.jpos.security.SMException;
import org.jpos.security.SecureDESKey;

/**
 * Relays purchases from the shop's terminals to the acquirer's host. The host reads PIN blocks
 * only in ISO format 0 ({@code SMAdapter.FORMAT01}) under the PIN key it shares with the relay.
 */
public class PinRelay {
    /** One terminal: its PIN key (under the module's LMK) and the PIN block format it sends. */
    public record Terminal(SecureDESKey pinKey, byte pinBlockFormat) { }

    private final SMAdapter<SecureDESKey> hsm;
    private final Map<String, Terminal> terminals;
    private final SecureDESKey hostPinKey;
    private final MUX mux;

    public PinRelay(SMAdapter<SecureDESKey> hsm, Map<String, Terminal> terminals,
                    SecureDESKey hostPinKey, MUX mux) {
        this.hsm = hsm;
        this.terminals = terminals;
        this.hostPinKey = hostPinKey;
        this.mux = mux;
    }

    /** Sends the terminal's purchase to the host and returns the answer for the terminal. */
    public ISOMsg send(ISOMsg fromTerminal, long timeout) throws ISOException {
        ISOMsg up = (ISOMsg) fromTerminal.clone();
        if (up.hasField(52)) {
            Terminal terminal = terminals.get(up.getString(41));
            if (terminal == null || !putHostPin(up, terminal))
                return refusal(fromTerminal);
        }
        return mux.request(up, timeout);
    }

    /** The answer a terminal gets when its purchase is not sent: declined, system malfunction. */
    static ISOMsg refusal(ISOMsg fromTerminal) throws ISOException {
        ISOMsg answer = (ISOMsg) fromTerminal.clone(2, 3, 4, 11, 41);
        answer.setMTI(fromTerminal.getMTI());
        answer.setResponseMTI();
        answer.set(39, "96");
        return answer;
    }

    /** The card number of the purchase. */
    static String cardNumber(ISOMsg m) {
        // TODO: The card number this purchase was made with. A chip or contactless purchase has it
        //       in field 2. A swiped card may arrive with no field 2 and its track 2 in field 35:
        //       the card number, then "=", then the expiry date and the rest of the track.
        throw new IllegalStateException("TODO: cardNumber");
    }

    /** Replaces field 52 of {@code up} with the PIN as the host must receive it. */
    boolean putHostPin(ISOMsg up, Terminal terminal) throws ISOException {
        // TODO: Field 52 holds the PIN block as this terminal produced it. Put in its place the
        //       block the host can read. Return true when `up` is ready to go to the host, and
        //       false when it must not be sent at all; send() then answers the terminal itself.
        throw new IllegalStateException("TODO: putHostPin");
    }
}

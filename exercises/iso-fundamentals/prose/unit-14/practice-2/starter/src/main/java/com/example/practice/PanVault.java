package com.example.practice;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;

/** Swaps the card number in field 2 for a format-preserving token, and back. */
public final class PanVault {

    /** Replaces the PAN in field 2 with its token. */
    public void protect(ISOMsg message) throws ISOException {
        throw new UnsupportedOperationException("write protect");
    }

    /** Replaces a token this vault issued, in field 2, with its PAN. */
    public void reveal(ISOMsg message) throws ISOException {
        throw new UnsupportedOperationException("write reveal");
    }
}

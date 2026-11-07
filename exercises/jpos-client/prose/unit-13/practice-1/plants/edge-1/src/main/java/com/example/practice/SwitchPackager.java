package com.example.practice;

import org.jpos.iso.IFB_BINARY;
import org.jpos.iso.IFB_BITMAP;
import org.jpos.iso.IFB_LLNUM;
import org.jpos.iso.IFB_NUMERIC;
import org.jpos.iso.IFE_CHAR;
import org.jpos.iso.ISOBasePackager;
import org.jpos.iso.ISOFieldPackager;

/** The regional switch's layout, as its interface specification describes it. */
public class SwitchPackager extends ISOBasePackager {

    public SwitchPackager() {
        ISOFieldPackager[] f = new ISOFieldPackager[71];
        f[0]  = new IFB_NUMERIC(4, "MESSAGE TYPE INDICATOR", true);
        f[1]  = new IFB_BITMAP(16, "BITMAP");
        f[2]  = pan();
        f[3]  = new IFB_NUMERIC(6, "PROCESSING CODE", true);
        f[4]  = new IFB_NUMERIC(12, "AMOUNT, TRANSACTION", true);
        f[11] = new IFB_NUMERIC(6, "SYSTEM TRACE AUDIT NUMBER", true);
        f[41] = terminalId();
        f[52] = pinBlock();
        f[70] = networkCode();
        setFieldPackager(f);
    }

    /** Field 2, the PAN: n..19, LLVAR. Length and digits in BCD. */
    ISOFieldPackager pan() {
        return new IFB_LLNUM(19, "PRIMARY ACCOUNT NUMBER", false);
    }

    /** Field 41, the terminal ID: ans 8. Text on this switch is EBCDIC. */
    ISOFieldPackager terminalId() {
        return new IFE_CHAR(8, "CARD ACCEPTOR TERMINAL ID");
    }

    /** Field 52, the PIN block: b 64. */
    ISOFieldPackager pinBlock() {
        return new IFB_BINARY(8, "PIN DATA");
    }

    /** Field 70, the network management code: n 3, BCD, left-justified (the filler nibble last). */
    ISOFieldPackager networkCode() {
        return new IFB_NUMERIC(3, "NETWORK MANAGEMENT INFORMATION CODE", true);
    }
}

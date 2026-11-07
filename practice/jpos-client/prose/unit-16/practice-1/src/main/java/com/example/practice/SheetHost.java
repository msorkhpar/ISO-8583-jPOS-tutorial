package com.example.practice;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISORequestListener;
import org.jpos.iso.ISOSource;

import java.util.ArrayList;
import java.util.List;

/**
 * A host simulator that answers from the acquirer's test sheet. Each line of the sheet is
 * {@code case;mti;pan;amount from;amount to;rc}, for example
 * {@code C14;0200;;500.01;;51}. A blank column means "any", amounts are written in currency
 * units with two decimals, and an rc of {@code --} means the host answers nothing.
 */
public class SheetHost implements ISORequestListener {
    record Row(String caseId, String mti, String pan, String from, String to, String rc) { }

    private final List<Row> rows = new ArrayList<>();

    public SheetHost(List<String> sheet) {
        for (String line : sheet)
            rows.add(load(line));
    }

    static Row load(String line) {
        String[] c = line.split(";", -1);
        if (c.length != 6)
            throw new IllegalArgumentException("expected 6 columns: " + line);
        Row row = new Row(c[0].trim(), c[1].trim(), c[2].trim(), c[3].trim(), c[4].trim(), c[5].trim());
        check(row);
        return row;
    }

    /** Refuses a row the simulator could not answer from as the sheet means it. */
    static void check(Row row) {
        // TODO: The sheet is typed by hand. A row the simulator can't read exactly as the acquirer
        //       means it must stop the sheet from loading (IllegalArgumentException), rather than
        //       turn into a rule that answers the wrong requests. Look at the amount columns and rc.
        throw new IllegalStateException("TODO: check");
    }


    /** Whether this row applies to the request. */
    static boolean matches(Row row, ISOMsg m) throws ISOException {
        // TODO: Compare the row's columns with the request. Field 4 holds the amount in minor
        //       units, twelve digits; the sheet writes amounts as 500.00.
        throw new IllegalStateException("TODO: matches");
    }

    /** The row that answers this request, or null when the sheet has none. */
    Row decide(ISOMsg m) throws ISOException {
        // TODO: More than one row can apply to a request. The acquirer's notes say how to choose.
        throw new IllegalStateException("TODO: decide");
    }

    @Override
    public boolean process(ISOSource source, ISOMsg m) {
        try {
            if (!m.isRequest())
                return false;
            Row row = decide(m);
            if (row == null)
                return false;
            if (row.rc().equals("--"))
                return true;
            ISOMsg r = (ISOMsg) m.clone();
            r.setResponseMTI();
            r.unset(35, 45, 52, 55);
            r.set(39, row.rc());
            source.send(r);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

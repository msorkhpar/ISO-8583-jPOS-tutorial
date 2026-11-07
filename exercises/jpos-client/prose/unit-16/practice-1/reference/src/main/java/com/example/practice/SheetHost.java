package com.example.practice;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISORequestListener;
import org.jpos.iso.ISOSource;

import java.math.BigDecimal;
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
        if (!row.from().isEmpty() && !row.from().matches("\\d+\\.\\d\\d")
                || !row.to().isEmpty() && !row.to().matches("\\d+\\.\\d\\d"))
            throw new IllegalArgumentException("unreadable amount in " + row.caseId());
        if (!row.from().isEmpty() && !row.to().isEmpty() && minor(row.from()) > minor(row.to()))
            throw new IllegalArgumentException("empty range in " + row.caseId());
        if (!row.rc().matches("\\w\\w|--"))
            throw new IllegalArgumentException("bad rc in " + row.caseId());
    }

    static long minor(String amount) {
        return new BigDecimal(amount).movePointRight(2).longValueExact();
    }

    /** Whether this row applies to the request. */
    static boolean matches(Row row, ISOMsg m) throws ISOException {
        if (!row.mti().isEmpty() && !row.mti().equals(m.getMTI())) return false;
        if (!row.pan().isEmpty() && !row.pan().equals(m.getString(2))) return false;
        if (row.from().isEmpty() && row.to().isEmpty()) return true;
        if (!m.hasField(4)) return false;
        long amount = Long.parseLong(m.getString(4));
        return (row.from().isEmpty() || amount >= minor(row.from()))
            && (row.to().isEmpty() || amount <= minor(row.to()));
    }

    /** The row that answers this request, or null when the sheet has none. */
    Row decide(ISOMsg m) throws ISOException {
        for (Row row : rows)
            if (matches(row, m))
                return row;
        return null;
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

package com.example.practice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;

/**
 * The release check: compares a recorded response with the response the new build got for the
 * same request, and lists the paths on which they differ.
 */
public class ReplayCheck {
    private final Set<String> changesEveryRun;

    /** @param changesEveryRun paths such as "7", "11" or "127.2" that differ on every run by design */
    public ReplayCheck(Set<String> changesEveryRun) {
        this.changesEveryRun = changesEveryRun;
    }

    /** The paths ("39", "48.2") on which the two responses differ, in field order. */
    public List<String> differences(ISOMsg recorded, ISOMsg replayed) throws ISOException {
        List<String> out = new ArrayList<>();
        compare("", recorded, replayed, out);
        return out;
    }

    private void compare(String prefix, ISOMsg a, ISOMsg b, List<String> out) throws ISOException {
        for (int f : fields(a, b)) {
            String path = prefix + f;
            if (changesEveryRun.contains(path))
                continue;
            Object va = a.hasField(f) ? a.getComponent(f).getValue() : null;
            Object vb = b.hasField(f) ? b.getComponent(f).getValue() : null;
            if (va instanceof ISOMsg || vb instanceof ISOMsg)
                nested(path, va, vb, out);
            else if (!same(va, vb))
                out.add(path);
        }
    }

    /** The field numbers to compare, in the order the differences are reported. */
    @SuppressWarnings("unchecked")
    Iterable<Integer> fields(ISOMsg a, ISOMsg b) {
        Set<Integer> all = new TreeSet<>(((Map<Integer, ?>) a.getChildren()).keySet());
        all.addAll(((Map<Integer, ?>) b.getChildren()).keySet());
        all.remove(-1);
        return all;
    }

    /** Whether two values of the same field count as equal. Either may be null (field absent). */
    boolean same(Object a, Object b) {
        if (a instanceof byte[] && b instanceof byte[])
            return Arrays.equals((byte[]) a, (byte[]) b);
        return a != null && a.equals(b);
    }

    /** A field that has sub-fields on at least one side; the other side may be absent. */
    void nested(String path, Object a, Object b, List<String> out) throws ISOException {
        if (!same(a, b))
            out.add(path);
    }
}

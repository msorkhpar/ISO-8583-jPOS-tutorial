package com.example.practice;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
    Iterable<Integer> fields(ISOMsg a, ISOMsg b) {
        // TODO: The fields either response carries, in ascending order. A release must only fail
        //       on a real difference between the two responses. (about 4 lines)
        throw new IllegalStateException("TODO: fields");
    }

    /** Whether two values of the same field count as equal. Either may be null (field absent). */
    boolean same(Object a, Object b) {
        // TODO: True when the two values are the same. A value is a String, a byte[], or null
        //       when the response doesn't carry the field. (about 3 lines)
        throw new IllegalStateException("TODO: same");
    }

    /** A field that has sub-fields on at least one side; the other side may be absent. */
    void nested(String path, Object a, Object b, List<String> out) throws ISOException {
        // TODO: Field `path` holds sub-fields in at least one response. Report each sub-field that
        //       differs by its own path ("48.2"), as for any other field. (about 2 lines)
        throw new IllegalStateException("TODO: nested");
    }
}

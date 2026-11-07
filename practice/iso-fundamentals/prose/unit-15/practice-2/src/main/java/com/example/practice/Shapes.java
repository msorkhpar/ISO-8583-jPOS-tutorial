package com.example.practice;

import org.jpos.iso.ISOMsg;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** Which fields of a rejected message have a shape that none of the approved messages has. */
public final class Shapes {

    private Shapes() { }

    /** The fields, ascending, whose shape in {@code rejected} no message in {@code approved} has. */
    public static List<Integer> unseen(List<ISOMsg> approved, ISOMsg rejected) {
        Set<Integer> fields = new TreeSet<>(numbers(rejected));
        for (ISOMsg m : approved)
            fields.addAll(numbers(m));
        List<Integer> out = new ArrayList<>();
        for (int f : fields)
            if (!seen(approved, f).contains(shape(rejected, f)))
                out.add(f);
        return out;
    }

    /** The data fields a message carries: 2 and up (field 1 is the second bitmap, never a value). */
    private static Set<Integer> numbers(ISOMsg m) {
        Set<Integer> n = new TreeSet<>();
        for (int f = 2; f <= m.getMaxField(); f++)
            if (m.hasField(f))
                n.add(f);
        return n;
    }

    /**
     * The shape of one field: "absent"; "bytes N" for a binary value of N bytes; otherwise the kind
     * of characters and the length: "digits N", "text N" (printable ASCII, not all digits), or
     * "other N" (at least one character outside printable ASCII).
     */
    static String shape(ISOMsg m, int field) {
        // TODO: Describe the field so that two values that differ only as two purchases differ
        //       (another card, amount or time) get the same description, and a value whose form
        //       changed gets another. Use the forms in the comment above.
        throw new IllegalStateException("TODO: shape");
    }

    /** Every shape that {@code field} has in the approved messages. */
    static Set<String> seen(List<ISOMsg> approved, int field) {
        // TODO: Every description the field has across the approved messages.
        throw new IllegalStateException("TODO: seen");
    }
}

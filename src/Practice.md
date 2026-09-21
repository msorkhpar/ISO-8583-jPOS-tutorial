# Practices

The three series before this one explain ISO-8583 and show it being spoken by
jPOS. This one asks you to write it.

Each practice is plain Java — no build tool, no dependency, nothing to
download. The material these are drawn from is the format itself: the four
digits of the MTI, the sixty-four bits of the primary bitmap, and the lengths
that let a reader walk a message from one end to the other. None of that needs
a library, and writing it by hand once is the fastest way to stop treating a
message as opaque.

They are also the three shapes a practice can have. The first ships finished,
so its grader passes. The second ships wrong by one bit, so its grader fails
until you fix it. The third has no grader at all, because not everything worth
doing is worth scoring.

**This file is a record, not a chapter.** Each practice belongs to the unit
whose material it practises and is read onto *that unit's page*, under the
prose it is practising — never into a section of its own after the chapter is
over. The heading below names the container exactly as `README.md` names its
own, and the number of an entry is **the unit of that container the practice
joins**.

# ISO-8583 for Visa and Mastercard Transactions: A Comprehensive Developer's Guide

2. [Building the primary bitmap](src/p2.md)
3. [Unpacking data elements](src/p3.md)
4. [Reading the MTI](src/p1.md)

Certifying a new link means trying your layout against every sample message in the host's
specification. When a sample doesn't come out right, the person fixing the layout wants one
answer: which row to look at first.

Write `LayoutCheck.firstWrongField` in `src/main/java/com/example/practice/LayoutCheck.java`. It
gets the layout, the sample's bytes, and the value the specification documents for each field of
the sample. It returns the number of the field whose row to fix first, or 0 when the layout reads
the sample exactly as documented.

The tests use a purchase sample in an ASCII layout and break the layout in different ways. Some
of the broken layouts make `unpack` throw, and some don't. About eight lines in total.

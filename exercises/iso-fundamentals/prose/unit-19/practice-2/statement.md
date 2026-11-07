An acquirer's switch sends its totals in an `0500` every evening, and the network answers `2`,
out of balance, on most days, even on days when the network's settlement report pays exactly what
the switch expected.

`DayTotals.count` in `src/main/java/com/example/practice/DayTotals.java` reads one day's journal
and returns the totals the `0500` carries. Its journal holds purchases and refunds (processing
code `20`), authorizations, and reversals of purchases and of refunds, with their repeats. The
comment above `count` says what an entry holds.

Finish the three methods marked `TODO` so that the switch counts the day the way the network
does. `add` holds the switch's current counting, which is what makes the network answer `2`.
About 20 lines in total.

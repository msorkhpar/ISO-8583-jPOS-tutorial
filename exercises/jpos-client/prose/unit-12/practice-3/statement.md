Completions (0220 advices) report purchases that have already happened, so they must reach the
host, however long that takes. This SAF keeps them in a database table, the `Store` at the
bottom of `src/main/java/com/example/practice/AdviceSaf.java`, instead of a Space queue. The ideas
of 12.7 still apply. Only the tools are different: the table has `oldest`, `update`, `delete` and
`insert`, and no `push`.

Finish the four places marked `TODO`. `sendOldest` is called again and again by a single sender.
When you're done, an advice survives a process that dies while sending it, goes out as a repeat
after an unanswered attempt, and never lets a later advice overtake it. About 10 lines in total.

The tests simulate a crash by giving `sendOldest` a MUX that throws in the middle of `request`,
then building a new `AdviceSaf` on the same table.

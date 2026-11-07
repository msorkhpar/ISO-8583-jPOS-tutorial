This picks up where the previous practice ended, and the file already contains its solution.

Last night the node was restarted in the middle of the payout runs, and the operator moved
everything in `payouts/run/` back to `payouts/request/`. This morning two merchants called to say
they were paid twice. The same week, the acquirer resent a run by hand under a new file name, and
it was paid again too.

The `Ledger` is a database: it survives restarts, and each `pay` records the run together with its
payments. In `src/main/java/com/example/practice/PayoutProcessor.java`, finish `runOf` and
`alreadyPaid` so that a run reaching the processor again is answered as before and pays nobody,
whatever its file is called. A run number that comes back with different payouts is a problem for
a person, not a repeat. About six lines in total.

The tests restart the node by building a new `DirPoll` and processor on the same directories and
the same ledger.

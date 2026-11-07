Every evening the acquirer's settlement system drops a payout run into the server's `payouts/request/`
directory, and `PayoutProcessor`, a DirPoll processor, pays each merchant through the `Ledger`.
Treasury reports three kinds of trouble: a run that was paid short, a run that bounced to `bad/`
when it only needed a few more seconds, and a run that reached `bad/` only after DirPoll had tried
it all night.

A run file has a header `H|acquirer|run`, one `P|merchant|amount` line per merchant (a 15-character
merchant id and 1 to 12 digits in minor units) and a trailer `T|count|total`.

`process` in `src/main/java/com/example/practice/PayoutProcessor.java` already pays a good run and
answers it. Finish the five methods marked `TODO` so that a run is paid in full or not at all, a
file that may still be completed comes back later, and a file that never will goes to a person at
once. About 18 lines in total.

The tests take files through DirPoll's real `request/`, `run/`, `archive/` and `bad/` directories
in a temporary folder, and also call `process` directly.

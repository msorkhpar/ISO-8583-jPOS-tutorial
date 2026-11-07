Before a peak season the team runs a load test every night: twenty threads call `mux.request`
against the test host, and each call adds a `Sample` with how long the thread waited and whether
an answer came back. Last season the nightly report said the 99th percentile was under 5 ms, and
on the first busy Saturday the help desk took calls from cashiers whose terminals had shown
"outcome unknown" after the full timeout. The logs showed the same thing had happened every night
in the load test.

The report is built by `LoadReport` in `src/main/java/com/example/practice/LoadReport.java`.
Finish the three methods marked `TODO`: `nulls`, `latencies` and `percentile`. `percentile` uses
the nearest-rank definition its comment gives: with 20 waits, the 95th percentile is the 19th
smallest. When you're done, a run whose purchases end in `null` can't produce a report that looks
healthy. About ten lines in total.

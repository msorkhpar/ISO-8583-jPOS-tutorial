Last month a server lost both of its sessions to an issuer call that never returned, and terminals
reversed purchases for forty minutes before anyone looked at the log. Operations want a page the
moment a transaction stops finishing, and not one page more.

Finish `poll` in `src/main/java/com/example/practice/StuckWatch.java`. A monitor calls it every
few seconds for the life of the server, with the TransactionManager's `getHead()` and `getTail()`
and the time of the poll. It returns the id of a transaction that has been running for longer than
the limit given to the constructor, or `-1`. The server is busy at lunchtime and almost silent at
night, and the page has to be right at both. About ten lines in total.

The first test runs a real TransactionManager with two sessions, in which one purchase hangs in a
participant until the test releases it. The others pass head and tail values directly.

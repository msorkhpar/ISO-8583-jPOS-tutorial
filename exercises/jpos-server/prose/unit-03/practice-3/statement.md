Before a store closes for the night, the server signs its terminals off: an `0800` with field 70 =
`002`, addressed to one terminal through field 41. A batch job builds these messages and calls
`TerminalDirectory.send` for each. Today half of the sign-offs arrive at the wrong till, and the
till they were meant for keeps transacting.

`TerminalDirectory` in `src/main/java/com/example/practice/TerminalDirectory.java` is a request
listener that goes first in the QServer's chain. Finish its two methods marked `TODO`:

- `process(source, m)`: called for every message the server receives;
- `send(m)`: sends `m` to its terminal, and returns `false` when that terminal can't be reached now
  (the batch job retries those later).

Terminals reconnect during the day, and some are switched off without closing anything. About eight
lines in total.

The tests play QServer: they offer each message to the directory and then to a stand-in for the
rest of the chain, and their connections can be closed.

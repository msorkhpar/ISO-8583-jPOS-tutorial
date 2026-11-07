A parking-garage network allows one transaction in flight per terminal. Its terminals give up
after 20 seconds and send the next request at once, and the network wants that request declined
while the first one is still being processed. The switch runs two TransactionManagers, one for
purchases and one for refunds, and each has its own instance of the participant that uses
`TerminalLock`.

Finish `claim` and `release` in `src/main/java/com/example/practice/TerminalLock.java`. The claims
live in the space the class is given, under `key(terminal)`. When you're done:

- a terminal with a transaction in flight is refused, whichever TransactionManager asks, even when
  both ask at the same moment;
- a session that hangs and never calls `release` does not lock its terminal for longer than
  `maxHold` milliseconds;
- after `release` the terminal's next request is accepted at once.

About four lines in total. The tests use a space that records how each entry was written, and in
one test it makes two callers meet in the middle of their work.

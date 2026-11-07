An acquirer forwards every terminal request to its network. On the morning of the security test,
the testers report three findings: a purchase whose amount they changed on the way was approved, a
purchase they sent with no MAC was approved, and every PIN purchase from a new terminal comes back
from the network as `55`, incorrect PIN, though the cardholders typed the right one.

`TerminalGate` in `src/main/java/com/example/practice/TerminalGate.java` is the first participant of
the chain. `prepare` already decides what happens; finish the two methods it calls, marked `TODO`:

- `macVerifies`: whether this request really comes from the terminal it names;
- `forNetwork`: what the network receives in place of the terminal's request.

`QueryHost` comes later in the chain and sends whatever `forNetwork` returned. `macInput`, `hsm()`
and `keys()` are given. About twelve lines in total.

The tests deploy a real TransactionManager with a real `QueryHost` and QMUX in the test's own JVM,
with a real software security module and key file. Two terminals and the network hold their own
copies of the keys.

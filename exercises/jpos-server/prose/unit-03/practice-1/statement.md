Tonight the card database is offline for two hours. While it is down, the TransactionManager can't
approve or decline anything, and terminals would wait for their timeout and then reverse. Operations
wants them to get a decline at once instead: response code `91`, issuer or switch inoperative.

`MaintenanceListener` in `src/main/java/com/example/practice/MaintenanceListener.java` goes first in
the QServer's chain, before `IncomingListener`. Operations calls `begin()` when the database goes
offline and `end()` when it is back. `refusal(m)` already builds the `91` answer.

Finish the two methods marked `TODO`:

- `answersItself(m)`: which messages the listener declines, and when;
- `process(source, m)`: what it does with each message it is offered.

When you're done, a terminal that asks for a decision during maintenance gets its `91` straight
away, and for everything else the server behaves as if the listener weren't there. About ten lines
in total.

The tests offer each message to your listener and then to a stand-in for `IncomingListener`, the way
a QServer session does.

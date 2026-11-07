Merchants complain that on slow days some cardholders are charged twice. The culprit is
`PaymentClient.send` in `src/main/java/com/example/practice/PaymentClient.java`: when the host
doesn't answer in time, it sends the same message again.

`send` handles every kind of message the client sends: purchases and authorizations, advices, and
echoes. Fix the three places marked `TODO` so that each ends safely and returns the `Outcome`
that says what happened:

- `send`: when the link is down;
- `outcomeOf`: when the host answered;
- `onNoAnswer`: when it didn't. This is the part that matters.

Anything that must still reach the host later goes onto the `saf.host` queue of the space
(`sp.out`). Something else delivers it, as in 12.7. The `ReversalBuilder` from 12.6 is already in
the file. About 15 lines in total.

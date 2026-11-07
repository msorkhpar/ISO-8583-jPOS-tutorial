This is the gateway from 12.3.1, and it is lunchtime. Three hundred terminals send their
purchases through `Upstream.forward`, and the host knows the gateway as a single terminal,
`GW000001`. Some purchases fail with `Duplicate key`.

`forward` already works for one terminal. Finish the three methods marked `TODO` in
`src/main/java/com/example/practice/Upstream.java` so that it also works for many:

- `nextStan()`: the gateway's numbering for its own link;
- `upstreamCopy(...)`: what the host receives;
- `forTerminal(...)`: what the terminal gets back.

The comment on each method says what it is for. When you're done, two terminals can send the
same STAN at the same moment and both get their answers, and each terminal recognises the answer
as its own. About ten lines in total.

The tests run a real QMUX against a test host like the one in 12.1. Some of them keep two
purchases in flight at once.

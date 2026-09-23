An operator reading a server's log after an incident needs one thing above all: every
transaction the server touched left a trace, including the ones that failed. The page builds
its event in a `try` and logs it in a `finally` for exactly that reason.

Write `TransactionProcessor`, built from a jPOS `Logger`, with `boolean process(ISOMsg msg)`.
For every call it delivers exactly one event tagged `transaction.process` to that logger:

- for a good message the event says `Processing transaction: <MTI>` and then
  `Transaction processed successfully`, and `process` returns `true`;
- for a message that cannot be processed (one with no MTI, say) the event carries the
  exception jPOS raised instead of the success line, and `process` returns `false`. It is
  still logged, once.

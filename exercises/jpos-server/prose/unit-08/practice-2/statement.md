During an incident an operator turns a server's debug mode on, reads what it says, and turns
it off again. The page adds and removes a listener to do that. Here the switch is a flag, and
what it controls is one extra event per transaction.

Write `DebuggableServer`, built from a jPOS `Logger`, with `setDebugMode(boolean on)` and
`processTransaction(ISOMsg msg)`. Every transaction is logged as one event tagged
`transaction.process`, saying `Processing transaction: <MTI>`. While debug mode is on, a
`transaction.debug` event comes first, saying `MTI: <MTI>` and then `Amount: <field 4>`.

- Debug mode starts off, and switching it off again stops the debug events.
- A message with no amount (field 4) is logged as `Amount: none`. An operator reading
  `Amount: null` cannot tell a missing field from a bug.

The card number is deliberately not logged, even in debug mode: see the client's unit 8.

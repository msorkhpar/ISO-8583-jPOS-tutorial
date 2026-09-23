The page's `TransactionLogger` writes the MTI, the card number and the amount of every
transaction into the log. A full card number (PAN) in a log file is exactly what the card
schemes' security rules forbid: logs are copied, shipped and kept far longer than a
transaction.

Write `TransactionLogger`, built from a jPOS `Logger`, with `logTransaction(ISOMsg msg)`. It
delivers one event tagged `Transaction` to that logger, carrying, in this order:

- `MTI: <the message type>`;
- `PAN: <the card number, masked>`, keeping its first six and last four digits and replacing
  every digit between with `*`. So `4111111111111111` is logged as `411111******1111`;
- `Amount: <field 4>`.

A message that carries no card number (field 2) logs no `PAN:` line at all, rather than a
line saying nothing.

Your acquirer wants card numbers out of every system that does not strictly need them. Messages
leaving the secure zone should carry a token in field 2 instead of the PAN, and only the vault,
inside the secure zone, may turn a token back into the card number. The token has to look like
a card number, because routing, logging and receipts downstream still expect one.

Write the class `PanVault` with two methods, each working on field 2 of the message in place:

- `protect(ISOMsg message)` replaces the PAN with a token: the same length, digits only, the
  first six digits (the BIN, which routing needs) and the last four (what the cardholder sees
  on a receipt) kept, the digits between replaced. A token equal to the card number protects
  nothing, so never hand one out.
- `reveal(ISOMsg message)` replaces a token this vault issued with the card number it stands for.

Why the edges matter:

- The same card must always get the same token, or reports and fraud checks downstream see
  one card as many.
- A token the vault never issued is refused with an `ISOException`, and field 2 is left
  exactly as it was. Passing along an empty or unchanged field 2 would send a message on
  with no real card number in it.

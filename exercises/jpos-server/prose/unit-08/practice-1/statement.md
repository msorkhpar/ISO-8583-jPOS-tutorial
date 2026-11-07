The support desk has a run of tickets about cards refused with `54` (expired card), and a few with
`30`, from cardholders whose cards are valid until next year. The server's log has the
TransactionManager's events and nothing else: `prepare: c.e.p.CardExpiry ABORTED`. Nobody can tell
which expiry date the server read, or which month it compared it with.

Write `prepare` in `src/main/java/com/example/practice/CardExpiry.java`. `refusal` already decides
and `reply` builds the answer: `54` for an expired card, `30` for an expiry that can't be read.

When you're done, an operator who has a terminal ID and a STAN finds in the log one entry for
every purchase this participant handled, whether it let it through, refused it, or couldn't read
it, with what it decided and why. From that entry the operator can find the TransactionManager's
event for the same transaction. The log is shipped to a platform that a few hundred people can
search. About 15 lines.

The tests call the participant directly. They give it a logger whose chain is the production one,
`ProtectedLogListener` and then a writer, the way `logger="Q2" realm="card-expiry"` would under
Q2. The current month is fixed at November 2026.

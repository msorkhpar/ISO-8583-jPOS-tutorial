The acquirer's host MACs every message in both directions, and its spec, copied into the class
comment of `src/main/java/com/example/practice/MacLink.java`, differs from the one on the page in
one point: when a message has any field above 64, the MAC goes in field 128 instead of 64. Your
reversals carry field 90, so they are such messages.

Two problems reached operations this month:

- every reversal the client sends is answered `63`, security violation;
- on a day the host's MAC key was being changed, a few hundred cardholders were told "declined"
  for purchases that their banks show as paid.

Finish the four methods marked `TODO`: `macField`, `macInput`, `sign` and `outcomeOf`. `send`
already signs a copy of the request and passes the response, or `null` when none came, to
`outcomeOf`. About twenty lines in total.

The tests use a real `JCESecurityModule` with a test MAC key on each side and the course's
`iso87ascii.xml` packager on every message. The host behind the fake `MUX` checks your MAC with
its own code, answers `63` when it doesn't verify, and declines amounts over 500.00. Some tests
change its answer on the way back.

Your server authorizes cash withdrawals for an ATM network, and the network's operations team has
sent three complaints this week:

- During last night's database maintenance every withdrawal was answered `05`, and their on-call
  engineer was not paged.
- Twice an ATM dispensed cash for a withdrawal that your server has no record of. Both times the
  journal's disk was full.
- Since someone's quick fix, withdrawals over the limit come back `96` instead of `61`.

All three pass through `AnswerFailures` in `src/main/java/com/example/practice/AnswerFailures.java`,
which stands right before `SendResponse`. Its comment lists what the network's specification
says. Finish `codeFor` and `needsAnswer`, marked `TODO`. About six lines in total.

The tests deploy a real TransactionManager with Q2, in the test's own JVM. Participants in front
of yours report causes, throw, approve, approve part of the amount, or refuse on their own, as
each test asks.

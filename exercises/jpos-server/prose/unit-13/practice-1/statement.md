An acquirer's ATMs wait 15 seconds for the answer to a withdrawal, then show "unable to process"
and send a reversal. On payday evenings the server's queue and its PIN checks are slow, and the
issuer's team reports approvals for withdrawals that were reversed a few seconds later, and
requests for withdrawals whose ATM had already given up. Each one holds the cardholder's money
until the reversal arrives.

`AskIssuer`, in `src/main/java/com/example/practice/AskIssuer.java`, asks the issuer about each
withdrawal and leaves the answer for the participant after it. Its `terminal-wait` property is how
long the ATMs wait. Finish `remaining` and `ask`, marked `TODO`: the issuer must never be given
longer than the ATM is still waiting, and a withdrawal its ATM has stopped waiting for must not
reach the issuer at all. Then the transaction stops, and its `Result` says why, so the participant
that answers on the abort path has a cause. About ten lines in total.

The tests call `prepare` the way the TransactionManager does, with a Context like the one
`IncomingListener` builds, and an issuer link registered as a MUX that records what it was sent.

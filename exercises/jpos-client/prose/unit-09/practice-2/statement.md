This picks up where the previous practice ended. The file already contains its solution.

The client now reads the host's answers with the network's own code list, which its constructor
receives as a map from field 39 codes to a `Category`. The network's operations team sent a list
of complaints:

- Some cardholders were told "declined" and were charged anyway.
- On the night the issuer was down for maintenance, some cardholders were told their card was
  declined, and others were charged twice.
- After Tuesday's release the host rejected every purchase from one terminal model for six hours.
  Each cardholder was told "declined", and nobody on the team found out.
- During a host upgrade, some answers came back with no field 39, and some with a code that isn't
  on the network's list. The client reported them all as declines. The host's records show that
  some were approvals.

Finish `outcomeOf` in `src/main/java/com/example/practice/PaymentClient.java`. The `Outcome`
enum has a value for every kind of ending. Anything the developers must look at goes onto the
`alerts` queue of the space (`sp.out`), and `unknown(...)` hands a message to the reversal
process. About ten lines.

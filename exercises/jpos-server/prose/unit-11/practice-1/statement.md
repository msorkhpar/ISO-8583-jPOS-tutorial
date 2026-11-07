A prepaid card can't spend more than its balance. `HoldFunds` takes the purchase amount off the
card while the issuer decides, and must give it back when the purchase doesn't go through. An
earlier version passed every test the team had, and still lost cardholders' money in production.

The team's checks are in `src/main/java/com/example/practice/HoldFundsChecks.java`. The tests run
them against a real TransactionManager whose chain is `HoldFunds`, an issuer stand-in and
`SendResponse`, each check on a fresh chain (the `Chain` interface says what a check can do).
`approvedPurchase` is written. Finish the two checks marked `TODO`, so that a correct `HoldFunds`
passes all three, and a `HoldFunds` that gets the cardholder's money or the terminal's answer wrong
fails at least one. The tests try several wrong versions. About ten lines in total.

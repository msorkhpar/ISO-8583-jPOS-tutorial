A cardholder paid 19.95 at one shop, and her receipt says 640.00, the amount of another
cardholder's purchase at another shop a moment earlier. `CardStatus` asks the card service about
each purchase and builds the answer. The version in production passed every check the team had.

The team's checks are in `src/main/java/com/example/practice/InFlightChecks.java`. The tests run
them against a real TransactionManager with two sessions, whose chain is `CardStatus` and
`SendResponse`; the `Chain` interface says what a check can do, including holding the card
service. `onePurchase` is written. Finish `twoCardholdersAtOnce`, so that a correct `CardStatus`
passes, and a `CardStatus` that mixes anything of one purchase into the other's answer fails. The
tests try several wrong versions. About twelve lines.

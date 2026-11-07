The fraud team has a velocity rule: no card gets more than three purchases a day through this
server, and the fourth is refused with `65`. `VelocityLimit` enforces it, in
`src/main/java/com/example/practice/VelocityLimit.java`, ahead of the authorizer. The daily counts
are given (`DailyCounts`), and so is the refusal (`tooMany`).

Finish `prepare` and `abort`, marked `TODO`. A purchase counts only if it goes through: one the
authorizer declines must not use up the card's day, and once a card has had its three, every
further purchase that day is refused. About 15 lines in total.

The tests deploy a real TransactionManager with Q2, in the test's own JVM, with the real
`SendResponse` after the authorizer.

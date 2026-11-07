Since the fraud-scoring service went live, terminals time out at lunchtime. The service needs up
to two seconds for a score, and while one purchase waits for its score, the purchases queued
behind it wait too.

`ScoreFraud`, in `src/main/java/com/example/practice/ScoreFraud.java`, asks the service about each
purchase and leaves the score under `SCORE` in the Context. The participant after it, given in the
tests, decides: 800 or more is declined with `59`. The service is the `Scorer` interface at the
bottom of the file. It answers later, on a thread of its own, through the listener you pass it,
`answers`.

Finish `prepare` and the two methods of `Answers`, marked `TODO`. A purchase waiting for its score
must not hold up the others, each score must reach its own purchase, and a purchase the service
can't score goes on without one. About eight lines in total.

The tests deploy a real TransactionManager with Q2, in the test's own JVM, and give it a single
session. Their scorer holds some purchases until the test releases them, so several can wait at
once.

The acquirer now offers cardholders the price in their card's currency. `QuoteRate`, in
`src/main/java/com/example/practice/QuoteRate.java`, asks an exchange-rate service for a quote and
pauses the transaction. The service has no timeout of its own, so the participant schedules one;
without a rate, the purchase goes on and no conversion is offered. After `QuoteRate` the
transaction pauses again while the card's issuer is asked.

Since the release, a few purchases a day are answered `91` (issuer unavailable) a few milliseconds
after the issuer was asked. The issuer approves each of them shortly afterwards, so the cardholder
is charged for a purchase the terminal reported as failed. Every one of them had a slow or failed
quote.

Finish `Pending`, the nested class marked `TODO`: one `Pending` exists per transaction, and the
quote, a failure and the timeout reach it on other threads, in any order. About ten lines in total.

The tests deploy a real TransactionManager with one session, with Q2 in the test's JVM. The
exchange-rate service and the issuer answer when the tests say so, and the timeout is one second.

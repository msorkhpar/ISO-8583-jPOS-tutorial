A participant that calls an outside service is hard to test against the real thing: it may be
slow, down, or unable to give you the verdict you want to check. The page tests such a
participant with the service mocked, and tests a validating participant on its own with a fresh
`Context`. This participant does both jobs, and the tests you are graded by mock its service
exactly that way.

Write `FraudScreeningParticipant`, a jPOS `TransactionParticipant` built from a
`FraudService` (declared for you as a nested interface). In `prepare`, read the context entries
`amount` and `cardNumber` (strings), then:

- no amount (or an empty one): put `Amount is missing` in the context entry `error`;
- an amount that is not a number: `Invalid amount format`;
- an amount that is not positive: `Amount must be positive`.

In each of those cases return `ABORTED` without calling the service: there is no point
screening a transaction that is already refused. Otherwise ask the service
`isFraudulent(cardNumber, amount)`, with the amount as a `BigDecimal`. A fraud verdict puts
`Boolean.TRUE` in `fraudDetected` and returns `ABORTED`; a clean verdict returns `PREPARED` and
leaves `fraudDetected` unset.

- Zero is not a positive amount.
- If the service itself fails (it throws), `prepare` must not throw: put
  `Fraud check unavailable` in `error` and return `ABORTED`.

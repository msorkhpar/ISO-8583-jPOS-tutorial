A jPOS client fails in a handful of distinct ways: the message itself is wrong
(`ISOException`), the network let you down (`IOException`), the host never answered
(`TimeoutException`), or the host answered and declined. Whoever called the client needs to
know which of these happened, because each one calls for a different reaction: fix the
request, try again later, or tell the cardholder. Handling them in one place keeps that
decision consistent across the whole application.

Write `ErrorHandler.handle(Throwable failure)`, returning an `ErrorResponse` with a type, a
message and an HTTP-style status:

- `ISOException`: type `ISO-8583 Error`, the exception's own message, status 400;
- `IOException`: type `Network Error`, message `Failed to communicate with the server`,
  status 503;
- `TimeoutException`: type `Timeout Error`, message `The operation timed out`, status 408;
- `TransactionDeclinedException`: type `Transaction Declined`, message
  `Transaction was declined with response code: <code>`, status 402;
- anything else: type `Internal Server Error`, message `An unexpected error occurred`,
  status 500.

`TransactionDeclinedException` is yours to write too, as a nested class: an unchecked
exception that carries the host's response code.

- A narrower kind of failure belongs with its family: a refused connection is an
  `IOException`, so it is a network error.
- An unexpected failure says nothing about its cause. Its message may carry internals a
  caller has no business seeing.

When something goes wrong inside a server, the other side still needs an answer it can act on,
and the operator needs to know which kind of thing went wrong. The page gives each kind of
failure its own exception type and maps each type to its own response code. Here you build that
mapping and the participant that uses it.

Write `ErrorHandlingParticipant`, a jPOS `TransactionParticipant`. The three exception types
are declared for you as nested classes: `ISO8583ParseException`,
`TransactionProcessingException` and `NetworkCommunicationException`.

`static ISOMsg errorResponse(Exception failure)` builds the error response: field 39 holds the
code and field 63 the message.

- A parse failure is `01`, a processing failure `02`, a network failure `03`, each with the
  failure's own message.
- Any other failure is `99` with the message `An unexpected error occurred`. Its own text may
  hold an internal detail (a query, a host name, a key alias) and must never reach the response.

`prepare` reads the request, an `ISOMsg`, from the context entry `REQUEST`:

- no request is a parse failure, `Invalid or missing ISO message`;
- processing code (field 3) `999999` is a processing failure, `Invalid processing code`;
- processing code `888888` is a network failure, `Network is unavailable`;
- anything else is fine, and `prepare` returns `PREPARED | NO_JOIN`.

On any failure, including one you did not anticipate, `prepare` must not throw. It puts the
error response in the context entry `RESPONSE` and returns `ABORTED | NO_JOIN`.

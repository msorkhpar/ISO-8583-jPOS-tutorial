The gateway's `POST /payments` now keeps its idempotency keys in a database table instead of a map
in memory, so that they survive a restart and every gateway node sees the same keys. Shops' HTTP
clients give up after a few seconds and call again with the same `Idempotency-Key`. Some call twice
at once from two servers, and one shop's release reused keys for a day.

Write `pay` in `src/main/java/com/example/practice/PaymentsEndpoint.java`. A cardholder pays at
most once for each payment a shop sends, and every call, first or repeated, tells the shop the truth
about that payment with one of the statuses listed on `Result`.

Everything else is in the file. `KeyTable` behaves like a table with a unique constraint on its key
column. `Host.request` returns `null` when no answer came in time. `purchase` builds the 0200,
`reversalOf` the 0420 that undoes it (add it to `reversals`, which a store-and-forward sender
delivers), and `outcomeOf` reads the host's answer. About twenty lines.

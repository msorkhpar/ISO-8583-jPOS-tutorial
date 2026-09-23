The page's factory stamps the fields every request shares, so a message type only adds what is
its own. But its trace number is a fixed `"123456"` and its date is a constant. A host matches a
response, and later a reversal, to the request by these fields; two requests with the same trace
number are two requests nobody can tell apart.

Write `RequestFactory`, built from a terminal id, a `java.time.Clock` and the last trace number
already used (`0` for a fresh start).

- `create(mti)` returns an `ISOMsg` with that MTI, field 7 as the clock's time in UTC formatted
  `MMddHHmmss`, field 11 as the next trace number (six digits, zero-padded) and field 41 as the
  terminal id.
- `authorization(pan, amountMinor)` is an `0100` and `financial(pan, amountMinor)` an `0200`;
  both carry field 2 (the PAN), field 3 `000000`, field 4 (the amount in minor units, twelve
  digits zero-padded) and field 49 `840`.
- `reversal(original)` is an `0400` carrying the original's fields 2, 3, 4 and 49, and field 90:
  the original's MTI, then its field 11, then its field 7, then 22 zeros (42 digits in all).

Edge cases:

- After `999999` the trace number starts again at `000001`; `000000` is never used.
- A reversal is a new message: it takes its own trace number, not the original's.

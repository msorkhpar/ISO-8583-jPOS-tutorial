A new card-network link is booked for certification next week. Its specification lists the
fields of an authorization and gives sample messages as hex dumps; the certification analyst
will compare your messages with those samples, byte for byte.

`src/main/java/com/example/practice/MastercardLayout.java` builds a `GenericPackager` from a list of field definitions. Finish
`fields()`, which returns one `<isofield .../>` element per field, so that the packager writes
messages the way the samples do. About ten lines.

The specification's field table:

| Field | Name | Format |
|---|---|---|
| 0 | message type indicator | n 4 |
| 1 | bitmap | b 64 |
| 2 | primary account number | n ..19, two-digit length |
| 3 | processing code | n 6 |
| 4 | amount, transaction | n 12 |
| 11 | system trace audit number | n 6 |
| 41 | card acceptor terminal ID | ans 8 |
| 42 | card acceptor ID | ans 15 |
| 49 | currency code, transaction | n 3 |

One of its samples, an `0200` with fields 3, 4, 11, 41 and 49:

```text
F0F2F0F03020000000808000F0F0F3F0F0F0F0F0F0F0F0F0F0F0F4F8F7F5F0F0F0F9F1F5D2C9D6E2D2F0F740F9F7F8
```

The tests pack other messages and compare them with the specification's other samples.

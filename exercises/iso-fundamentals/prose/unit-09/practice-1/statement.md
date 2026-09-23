Every transaction ends in one of two ways: a response arrives and field 39 says what the
issuer decided, or something fails before it can. Each case calls for different handling,
and getting the category wrong is costly: calling an unknown code an approval gives goods
away, and retrying a security failure hides an attack.

Write `ResponseHandling` with two methods.

`outcomeOf(ISOMsg response)` reads field 39 and returns an `Outcome`:

- `APPROVED` for the page's approval codes: `00`, `10` (partial approval) and `85`
  (no reason to decline);
- `REFER` for the referral codes `01` and `02`: the issuer wants to be called, which is
  neither an approval nor a plain decline;
- `DECLINED` for everything else.

`actionFor(Exception failure)` returns an `Action`:

- `RETRY` for a network error, which arrives as a `java.io.IOException`;
- `LOG_FOR_FIX` for a format error, which jPOS reports as an `org.jpos.iso.ISOException`;
- `ALERT_SECURITY` for a `SecurityException`.

Edge cases to handle:

- `10` and `85` are approvals as well as `00`.
- A response that carries no field 39 is `DECLINED`; asking must not fail.
- Network errors come in many kinds: a `java.net.SocketTimeoutException` is an
  `IOException` and is retried like the rest.

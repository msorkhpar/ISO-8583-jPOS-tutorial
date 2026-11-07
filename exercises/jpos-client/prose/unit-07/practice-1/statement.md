A shop's PIN pads send their purchases through `PinRelay` to the acquirer's host. The host reads PIN
blocks in one way only: ISO format 0 under the PIN key it shares with the relay (`hostPinKey`).
The terminals are less tidy. Each has its own PIN key, and the shop runs two models: the older
one sends ISO format 0, the newer one ISO format 1. The relay's `terminals` map knows each
terminal's key and format.

Three complaints have come in:

- cardholders who swipe their card and type a PIN are always declined. A swiped purchase can
  arrive with no field 2, only the card's track 2 in field 35;
- on the new model, every PIN purchase is declined;
- last month a PIN pad's key fell out of step after a repair, and for an afternoon the host
  approved its purchases without checking any PIN.

Finish the two methods marked `TODO` in `src/main/java/com/example/practice/PinRelay.java`:
`cardNumber` and `putHostPin`. `send` and `refusal` already work; when `putHostPin` returns
`false`, `send` answers the terminal itself. About fifteen lines in total.

The tests run a real `JCESecurityModule` on both sides, with test keys and the test cards
`4111111111111111` and `5555555555554444`. The host behind the fake `MUX` reads each PIN block it
receives and declines with `55` when it can't find the cardholder's PIN.

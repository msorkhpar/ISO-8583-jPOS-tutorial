A PIN must never cross the network in the clear, and both sides must be able to tell whether a
message was changed on the way. The page's `SecureISOClient` does both: it encrypts the PIN,
adds a Message Authentication Code (MAC) to what it sends, and checks the MAC on what comes
back. The order of those steps and what each MAC is computed over are what make it work.

Write `SecureClient`. It is built from two collaborators, both nested interfaces of the class:
`Security` (the security module: `encryptPin(pin, pan)` and `mac(msg)`) and `Link` (the
channel: `exchange(request)` sends a request and returns the response). Your `send(ISOMsg msg)`:

- replaces the clear PIN in field 52 with `encryptPin(pin, pan)`, where the PAN is field 2;
- puts in field 64 the MAC of the message as it is sent, so computed after the PIN is
  encrypted, and over the message without field 64 itself;
- exchanges the message over the `Link`;
- checks the response: its field 64 must equal the MAC of the response without its field 64.
  If it does, `send` returns the response; if not, it throws a `SecurityException`.

Mind these:

- A response whose MAC does not match (someone changed the amount, say) is refused.
- A response with no field 64 at all is refused too. A missing MAC is not a valid one.

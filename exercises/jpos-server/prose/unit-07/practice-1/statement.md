A cardholder's PIN must never travel, or sit in memory longer than it has to, in the clear.
The server hands it to a security module, which returns a PIN block: the PIN combined with
the card number and encrypted under a key the application never sees. The block is what goes
into field 52. Because the application only talks to the `SMAdapter` interface, the module
behind it can be jPOS's software `JCESecurityModule` or a hardware HSM, and in a test a mock.

Write `PinEncryptionService`, built from an `SMAdapter`, with
`ISOMsg encryptPIN(ISOMsg msg, String clearPin)`. It:

- asks the module to encrypt the PIN with the card number from field 2, through
  `encryptPIN(pin, accountNumber)`. Pass the whole card number; jPOS takes the digits it needs;
- puts the returned block (`EncryptedPIN.getPINBlock()`) into field 52 and returns the same message;
- never puts the clear PIN into any field.

Watch for:

- field 52 is binary: it holds the block's 8 bytes themselves, not their hex digits as text;
- a message with no card number cannot have a PIN block. Throw `IllegalArgumentException`
  before asking the module anything.

The page's `encryptPIN(clearPIN, pan, "01")` and `getEncoded()` are not jPOS 2.1.7 API; use
the two-argument `encryptPIN` and `getPINBlock()`.

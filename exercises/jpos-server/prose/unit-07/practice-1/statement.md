A cardholder's PIN must never travel, or sit in memory longer than it has to, in the clear.
The server hands it to a security module, which returns it encrypted under the module's own
Local Master Key (LMK). That block is only good inside the module's domain: nobody else holds
the LMK, so the next hop could never read it. Before it goes on the wire, the module
re-encrypts (exports) the PIN under the Zone PIN Key (ZPK) the two parties share, in the
PIN block format they agreed on. That exported block is what goes into field 52. Because the
application only talks to the `SMAdapter` interface, the module behind it can be jPOS's
software `JCESecurityModule` or a hardware HSM, and in a test a mock.

Write `PinEncryptionService`, built from an `SMAdapter<SecureDESKey>`, the ZPK (a
`SecureDESKey`) and the PIN block format (a `byte`, such as `SMAdapter.FORMAT05`), with
`ISOMsg encryptPIN(ISOMsg msg, String clearPin)`. It:

- asks the module to encrypt the PIN with the card number from field 2, through
  `encryptPIN(pin, accountNumber)`. Pass the whole card number; jPOS takes the digits it needs;
- exports that result under the ZPK in the service's format, through
  `exportPIN(pinUnderLmk, zpk, format)`;
- puts the exported block (`EncryptedPIN.getPINBlock()`) into field 52 and returns the same message;
- never puts the clear PIN into any field.

Watch for:

- field 52 is binary: it holds the block's 8 bytes themselves, not their hex digits as text;
- a message with no card number cannot have a PIN block. Throw `IllegalArgumentException`
  before asking the module anything.

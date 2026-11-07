The certification lab runs a test issuer. Its software security module decrypts each field 52 and
passes the clear block, with the card number of the message, to `PinBlockReader.pin`, which
returns the PIN the cardholder typed. The issuer then compares that PIN with the test card's own.

The lab's devices are a mix: the older PIN pads send ISO format 0, the new ones format 3, and one
ATM vendor's test machine sends format 1, often without knowing the card number. Nothing else is in
use, and a block in any other format is refused.

A reader that gets a block wrong does one of two things, and both cost certification time. It
returns a PIN where it should have refused the block, so a wrong PIN can be approved; or it refuses
a PIN the cardholder typed right, and the vendor goes hunting for a bug in a pad that works.

Finish the two methods marked `TODO` in `src/main/java/com/example/practice/PinBlockReader.java`:

- `pinField(block, pan)`: the PIN field the device built, before it mixed anything in;
- `pinFrom(field)`: the PIN in it, or an `IllegalArgumentException` when the field isn't one a
  device would build.

`pin` already checks that the block is 16 hex digits and calls the two. About twenty lines in
total. `org.jpos.iso.ISOUtil` has `hex2byte`, `xor`, `hexString` and `isNumeric` if you want them.

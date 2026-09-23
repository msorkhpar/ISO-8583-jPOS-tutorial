A PIN never travels in the clear, and it never travels alone: before it is encrypted it is
laid out in a fixed sixteen-digit block and mixed with the card number, so a block captured
from one card is useless with another. The page shows the two halves separately, the padded
PIN layout under Format 0 and the XOR with the card number under Format 1. In ISO 9564 the
block that combines both is what Format 0 actually is, and that is the block you build here.

Write `PinBlock`, working in upper-case hexadecimal strings:

- `pinField(String pin)`: `0`, then the PIN length as **one** hexadecimal digit, then the PIN,
  then `F` up to sixteen digits. PIN `1234` gives `041234FFFFFFFFFF`.
- `panField(String pan)`: `0000` followed by the twelve rightmost digits of the PAN
  **excluding** its last digit, the check digit.
- `format0(String pin, String pan)`: the PIN field XOR the PAN field, digit by digit, as
  sixteen hexadecimal digits.

Edge cases to handle:

- A PIN of ten, eleven or twelve digits still has a one-digit length: `A`, `B` or `C`.
- A PIN is four to twelve decimal digits. Anything else is refused with an
  `IllegalArgumentException`, from `pinField` and from `format0`. You may assume a PAN is
  at least thirteen digits.

`org.jpos.iso.ISOUtil` has `hex2byte`, `xor` and `hexString`, if you want them.

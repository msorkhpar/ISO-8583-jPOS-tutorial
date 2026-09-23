The receiving side of a link never sees your `ISOMsg`: it sees bytes, and the bitmap is how it
learns which data elements follow. Reading it wrongly shifts every field after the mistake, so
the whole message parses as nonsense.

Write `BitmapReader.present(String bitmapHex)`. It takes the bitmap part of a message as
hexadecimal digits (either case) and returns, in ascending order, every data element it says is
present.

- The first sixteen digits are the primary bitmap. Bit *n*, counted from 1, most significant
  first, stands for data element *n*, from 2 to 64.
- Bit 1 of the primary bitmap is not a data element. When it is set, a secondary bitmap of
  sixteen more digits follows, and its bit *n* stands for data element 64 + *n*, from 65 to 128.
  Only the primary's bit 1 is a flag: there is no tertiary bitmap here.
- The length must agree with bit 1: sixteen digits when it is clear, thirty-two when it is set.
  Anything else, including a digit that is not hexadecimal, is refused with an
  `IllegalArgumentException`.

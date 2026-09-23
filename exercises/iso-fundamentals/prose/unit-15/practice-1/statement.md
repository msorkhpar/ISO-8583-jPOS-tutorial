A message reached your switch with its primary bitmap captured separately, in a trace, as
sixteen hexadecimal digits. Before anybody trusts the parsed fields, you want to know where
the bitmap and the message disagree.

Write `BitmapCheck.mismatches(String bitmapHex, ISOMsg message)`. It returns, in ascending
order, every data element from 2 to 64 whose bit in the bitmap says one thing while the
message says the other: a bit set for a field the message does not carry, or a field the
message carries whose bit is clear. A message that matches its bitmap gives an empty list.

- Bit 1 is not a data element. It flags that a secondary bitmap follows, so it is never
  reported, whatever it says.
- A bitmap that is not exactly sixteen hexadecimal digits is refused with an
  `IllegalArgumentException`.

Bits are counted from 1, most significant first: bit *n* stands for data element *n*.

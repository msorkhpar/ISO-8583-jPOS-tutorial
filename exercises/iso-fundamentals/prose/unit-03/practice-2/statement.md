Every data element has a format, written the way the data element tables write it: `n6`,
`an12`, `n..19`, `ans..999`. The format is all a receiver has to find where one field ends and
the next begins, so a value written in any other shape breaks the rest of the message.

Write `FieldEncoder.encode(String format, String value)`, which returns the field as it goes on
the wire.

- The format is a type followed by either a length (`n6`: fixed, exactly 6 characters) or `..`
  and a maximum (`n..19`: variable, up to 19).
- The types are `n` (digits), `a` (letters), `an` (letters and digits), `ans` (any printable
  ASCII character, space included) and `z` (track 2: digits and `=`). A value with a character
  outside its type is refused.
- A fixed field is padded to its full length. Numeric fields are padded with zeros on the left;
  every other type with spaces on the right.
- A variable field is not padded. It is preceded by its length, written with as many digits as
  the maximum has: two for `n..19` or `ans..40`, three for `ans..999`.
- A value longer than the field allows is refused.

Refuse with an `IllegalArgumentException`. You may assume the format itself is well formed.

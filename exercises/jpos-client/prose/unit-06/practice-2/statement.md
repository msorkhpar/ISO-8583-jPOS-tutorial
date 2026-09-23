Not every field is a plain string. Field 48 carries numbered subfields of its own, and field 55
carries the chip's EMV data as raw bytes. jPOS stores each as a component of the right kind: a
nested `ISOMsg` for subfields, an `ISOBinaryField` for bytes. Code that pushes these through
`set(int, String)` loses their structure or mangles their bytes, and the packager then sends
something the other side cannot read.

Write `ComplexFields` with four static methods:

- `putSubfield(ISOMsg msg, int field, int subfield, String value)` puts `value` into subfield
  `subfield` of `field`. The field is a nested `ISOMsg` whose own field number is `field`.
- `getSubfield(ISOMsg msg, int field, int subfield)` returns that subfield's value.
- `putBinary(ISOMsg msg, int field, byte[] data)` stores `data` in `field` as an
  `ISOBinaryField`.
- `getBinary(ISOMsg msg, int field)` returns the bytes stored in `field`.

Mind these:

- A field gets its subfields one call at a time. Putting subfield 2 of field 48 must not lose
  the subfield 1 already there.
- `getSubfield` returns `null` when the message does not carry the field at all, or carries it
  without that subfield.

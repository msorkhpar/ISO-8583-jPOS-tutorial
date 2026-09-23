PCI-DSS lets a PAN be shown only with its first six and last four digits visible. Screens,
support tools and log lines all show messages, so the rule is applied to the copy that is
shown, while the message being processed keeps the real card number it needs.

Write `CardMasking.forDisplay(ISOMsg message)`. It returns a copy of the message fit to
show:

- field 2, the PAN, keeps its first six and last four digits, and every digit between them
  is replaced by `*` (`4111111111111111` is shown as `411111******1111`);
- every other field is carried over as it is;
- a message with no field 2 is copied as it is.

Edge cases:

- PANs are not all sixteen digits. However long the PAN, exactly the first six and the
  last four stay visible.
- The message passed in is not changed. It is still being processed, and it needs its full
  PAN.

`ISOUtil.protect` masks with `_`, not `*`, so if you use it, mind the character.

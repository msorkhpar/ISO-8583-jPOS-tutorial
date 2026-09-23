A chip card authorization reaches your host with the card's EMV data in field 55, the ICC
system related data. The field is binary: one tag-length-value item after another, with no
separators. Before your risk rules can look at the Application Interchange Profile (tag `82`)
or the Terminal Verification Results (tag `95`), something has to split the field into its
items, and a mistake there silently feeds the rules the wrong bytes.

Write `ChipData.tags(ISOMsg message)`. It returns a map from each tag to its value, both as
upper-case hexadecimal, in the order the items appear in field 55. Each item is:

- the tag: one byte, unless the low five bits of that byte are all set (`0x1F`), in which case
  the tag is two bytes, so `9F 26` is the single tag `9F26`;
- the length: one byte, the number of value bytes that follow;
- the value: exactly that many bytes.

A message without field 55 is not an error: it simply has no chip data, and you return an
empty map. A field whose last item claims more bytes than are left is damaged; refuse it with
an `ISOException` rather than return a value cut short.

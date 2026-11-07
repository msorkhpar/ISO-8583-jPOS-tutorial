An issuer's authorization rules need two or three items from field 48 of every incoming
request. The team keeps field 48 in the packager as plain text, so that a network release that
adds a subelement can never stop a message from unpacking, and reads the items in code.

`src/main/java/com/example/practice/Field48Reader.java` already hands the rules what they ask
for through `wanted`. Finish the two methods it relies on:

- `categoryCode`: the transaction category code, or `null` when the field has none;
- `value`: the data of the subelement with a given two-digit tag, or `null` when the field
  doesn't carry that tag.

The field arrives exactly as the network sent it. About ten lines in total.

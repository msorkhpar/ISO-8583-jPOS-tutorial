A partner processor takes your authorization requests and sends its answers as JSON over a socket,
one object per message. Its system reads each object with an ordinary JSON parser and packs the
fields for its own 1987 link to the networks, so whatever a field holds when it leaves you must be
exactly what the partner's side reads, for every field a 0100 or a 0110 can carry. Both sides'
test messages use the same kinds of value as production, including PIN blocks, chip data and
merchant names as merchants type them.

`JsonPackager` in `src/main/java/com/example/practice/JsonPackager.java` is a working
`ISOPackager`: it writes one member per field and reads such an object back with a small JSON
parser of the same kind the partner uses. Three methods are left, marked `TODO`:

- `name`: the member name a field travels under;
- `jsonValue`: the value written after the colon;
- `put`: setting one member that was read back into the message.

The partner already sends text fields as members named by the field number, as the first test
shows. About fifteen lines in total.

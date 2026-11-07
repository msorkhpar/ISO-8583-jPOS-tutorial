The disputes service keeps card numbers in a table, encrypted, and support finds a card's rows by a
lookup value stored beside them. The key policy now says the data key changes every quarter, and
the security team wants to retire the first key by the end of the year without stopping the service.

`PanVault` in `src/main/java/com/example/practice/PanVault.java` holds the keys. `rotate` makes a
new data key current, `retire` destroys an old one, and `seal` already encrypts under the current
key. Finish the three methods marked `TODO`:

- `open(...)`: the card number in any record the table may hold;
- `rewrap(...)`: what a background job stores in place of an old record;
- `lookupValue(...)`: the value support searches by.

When you're done, rows written before any number of rotations still open, every row can be moved
to the current key so that old keys can be retired, and support's searches keep finding the same
rows through all of it. About ten lines in total.

The tests build their keys from fixed bytes. Real keys come from a keystore or an HSM.

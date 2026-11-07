The fraud team investigates disputed e-commerce purchases from the network's 2023 link. They have
the messages, and they want to ask for one value at a time with the paths of 17.5, such as
`104.0x03.0xDF10` or `55.0x9F27`, instead of reading hex dumps by hand. The messages come from other
systems, and some of the disputed ones are damaged. A wrong value is worse than no value: the team
acts on what the tool prints.

`DatasetFinder.find` in `src/main/java/com/example/practice/DatasetFinder.java` already parses the
path and looks tags up. It returns the bytes as upper-case hex, `null` when the message doesn't
have them, and refuses with an `IllegalArgumentException`. Finish the three methods marked `TODO`:

- `hasEnvelope`: which fields hold datasets;
- `isTlvDataset`: which datasets hold tags;
- `content`: one dataset's content.

The comment on each says what it is for. About fifteen lines in total.

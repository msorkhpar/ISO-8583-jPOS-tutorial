Support pastes messages captured on hex-ASCII links into tickets. The first questions are always
the same: which fields does the sender say it sent, and where do they begin? Today someone answers
them with a pencil, and gets them wrong about once a week.

`DumpReader` in `src/main/java/com/example/practice/DumpReader.java` takes one capture, from the
MTI on, exactly as the link carried it. Its constructor and `mti()` already work. Write the three
methods marked `TODO`:

- `bit(from, n)`, which the other two use;
- `fields()`, every field the capture says is present, in ascending order;
- `dataStart()`, the index in the capture where the first field's characters begin.

The tool reads at most two bitmaps. A capture that announces a third one must not be reported as
something it isn't: `fields()` refuses it with an `IllegalArgumentException`.

About fifteen lines in total.

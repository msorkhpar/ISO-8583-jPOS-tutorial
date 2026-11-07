Terminals settle at the end of the day, and from then on their transactions belong to a new
batch. The switch keeps each terminal's profile, with its current batch number, in the Space under
`profile.<terminal>`. In the tests the space is `tspace:`; in production it is `je:terminals`, so
that the profiles survive a restart.

Finish `nextBatch` in `src/main/java/com/example/practice/BatchNumbers.java`. When you're done, it
returns the new batch number, and afterwards:

- `profile(terminal)` shows that number, in a transient space and in a persistent one;
- the terminal still has one profile in the space;
- two sessions settling the same terminal at the same moment get two different numbers.

About five lines. One test uses a space that behaves like `je:`.

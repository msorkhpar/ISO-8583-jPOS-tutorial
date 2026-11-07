A merchant group says its busiest tills time out on purchases several times an hour, while
quieter tills on the same server never do. The server answers purchases in `CardCheckListener`,
which looks every card up in the blocked-card table. Under load a lookup takes a few seconds.

The table can't be made faster this quarter. Change `CardCheckListener` in
`src/main/java/com/example/practice/CardCheckListener.java` so that a slow lookup no longer
keeps a terminal waiting longer than its own purchase takes. The answer each terminal gets stays
exactly what it is today, and `answer(...)` already builds and sends it.

Finish the two places marked `TODO`, in `process` and `newWorkers`. About five lines in total.

The tests call `process` the way a QServer session would and use a card table whose lookups they
can hold and release. One test keeps two lookups running at once on one connection.

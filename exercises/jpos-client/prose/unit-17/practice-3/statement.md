The scrubber in 17.6 keeps card numbers out of the log, and it throws away the one thing the
support desk keeps asking for: the bytes of a reply the client couldn't read. The team wants
those bytes kept, but not in the log, which is shipped to a platform many people can search. They
go to an `EvidenceStore` that only the team can read, labelled with the event's realm and with the
part they came from.

`EvidenceKeeper` in `src/main/java/com/example/practice/EvidenceKeeper.java` takes the
scrubber's place in the logger, after `ProtectedLogListener` and before the writers. Finish the
two methods marked `TODO`: `log` and `entryFor`.

When you're done, no raw bytes reach the writers, the store holds the exact bytes of every failed
unpack, so that a test can be built from them, and everything else in the event reaches the
writers as it did before. About 15 lines in total.

The tests build `receive` events the way a channel builds them, with and without a header, with an
image from `DebugFilter`, and run one through a logger with a writer after the keeper.

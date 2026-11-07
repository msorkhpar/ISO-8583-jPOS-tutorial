A merchant group's acquirer gets the network's acknowledgement file every morning: one record per
sale it presented the day before. Its operations team wants one report that says, for every
record and every approval, what happened to it, so that each finding goes to whoever has to act.

`ClearingMatcher.match` in `src/main/java/com/example/practice/ClearingMatcher.java` builds that
report: one `Result` per clearing record, in the file's order, then one per approval that no record
presented, in the journal's order. The kinds of finding are in `Kind`. Finish the three methods
marked `TODO`; the comment on each says what it is for. About fifteen lines in total.

The tests use several terminals and compare whole lists of `Result`s.

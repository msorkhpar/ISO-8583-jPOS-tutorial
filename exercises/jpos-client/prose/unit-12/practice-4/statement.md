This picks up where the previous practice ended. The file already contains its solution.

An advice that the host never answers would sit at the front of the line forever, and every
advice behind it would wait too. The network allows a fixed number of attempts per advice. After
the last one, the advice goes to a second table, `parked`, where an operator deals with it, and
the line moves on.

Finish `notAnswered` in `src/main/java/com/example/practice/AdviceSaf.java`. The `Store` rows
already have an `attempts` column. The tests restart the SAF between attempts, and they also keep
the link down for a while. About 7 lines.

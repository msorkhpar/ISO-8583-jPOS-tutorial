You are on the issuer's side now. `Journal` records every request the issuer has processed, as it
arrived from the acquirers' links. When a reversal comes in, `onReversal` acknowledges it and
marks the request it cancels as reversed, so the money can go back.

Finish `originalOf(reversal)` in `src/main/java/com/example/practice/Journal.java`. When you're
done, a reversal cancels exactly the request it is about, wherever and whenever that request came
from, and cancels nothing when the issuer never saw that request. About ten lines.

The tests use several acquirers' requests, some of them days apart.

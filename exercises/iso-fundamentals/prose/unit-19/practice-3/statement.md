This picks up where the previous practice ended: `src/main/java/com/example/practice/DayTotals.java`
already counts the day correctly. Now the same class builds the `0500` and plays the receiving
side. Your test lab uses it as a stand-in host: it gets the switch's `0500`, counts the same day
from its own copy of the traffic, and answers.

`request` is written, except for the net settlement amount, DE 97. Finish `de97` and `answer`.
The receiver's answer is only useful if it says `2` whenever the two ends counted the day
differently, and the `0500` it received is evidence of what the switch said, so it keeps it as it
came. About ten lines in total.

The tests build totals directly; they include a day of more refunds than sales.

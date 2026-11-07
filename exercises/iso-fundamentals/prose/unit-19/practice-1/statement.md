Your switch has joined a card network run from London, and its first two weeks of `0510` answers
were out of balance more often than not. The network's operations guide says:

- the business day ends at **16:00 London time**: a transaction at 16:00:00 or later belongs to
  the next business day;
- the network settles Monday to Friday, except on its own holidays (the constructor gets them);
- most of the host's answers carry DE 15.

`BusinessDay.de15For` gives the DE 15 your switch records for each transaction, and `settles` the
day behind it. Both already work, except for the four methods marked `TODO` in
`src/main/java/com/example/practice/BusinessDay.java`. The comment on each says what it is for.
Finish them so that your switch counts every transaction on the day the network counts it. About
ten lines in total.

The switch runs in Tokyo. The tests include summer and winter dates and the network's Christmas.

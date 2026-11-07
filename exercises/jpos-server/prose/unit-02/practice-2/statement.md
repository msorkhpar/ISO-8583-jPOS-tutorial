Besides answering, your server now starts messages of its own on the link to each terminal:
echoes, notices that it is about to sign off for maintenance, and short texts for the merchant
(an 0620 with the text in field 63). `LinkMessages` in
`src/main/java/com/example/practice/LinkMessages.java` keeps one template per kind, and the
methods that build each kind are written.

The operations team reports two problems from the pilot. A terminal displayed a maintenance
window it had never been sent, the one added to the previous notice. And the terminals' logs show
an echo and a merchant text from the server in flight at the same time with the same STAN.

Finish the two methods marked `TODO`: `fromTemplate` and `nextStan`. The constructor receives
the last STAN the server used on the link, so numbering carries on across restarts, and a clock
that a test can fix. About six lines in total.

An acquirer's link carries the messages of many terminals at once, and the host sends a few of
its own. `Inbox` keeps every message the acquirer has sent and not yet seen answered. When
something arrives, `deliver` hands it to the sent message it answers, or returns `null` so the
caller can log it.

`deliver` already works. Finish `answers(sent, incoming)` in
`src/main/java/com/example/practice/Inbox.java`. When you're done, every answer reaches the
message it belongs to, including answers to advices and to their repeats, and nothing else
reaches anyone. About ten lines.

The tests send messages from several terminals with STANs that sometimes coincide.

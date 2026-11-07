An online bookshop sends its purchases to the acquirer through `Submitter.submit`, in
`src/main/java/com/example/practice/Submitter.java`. Yesterday a merchant whose name and location
ran one character too long in field 43 took the link down. The purchase in flight beside it was
approved by the host but reported to the shop as unanswered, and the support team couldn't tell
which of the two had actually reached the host.

Finish the three places marked `TODO` so that every `Result` tells the truth: `ANSWERED` with the
host's response, `NO_ANSWER` for a request that went out and heard nothing back, and `NOT_SENT`
with a reason for a request that never left. The `packager` passed to the constructor is the one
the link's channel uses. About 10 lines in total.

The tests run a real QMUX against a test host on the far side of its queues, and some of them use
a network whose fields are not the standard's lengths.

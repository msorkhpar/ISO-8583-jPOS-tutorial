When the Visa host doesn't answer, a participant writes a reversal to the `reversal.visa` queue of
the Space and lets the transaction finish. `ReversalForwarder` gets each reversal from there to
the link's sender, which talks to the host and can take several seconds per message. In
production two forwarders listen to the same queue, one for each of the link's two channels.

Finish `notify` in `src/main/java/com/example/practice/ReversalForwarder.java`. When you're done:

- every reversal written to the queue reaches a sender, and goes to the host once, not once per
  forwarder;
- a reversal that has been handed to a sender is no longer in the queue;
- the participant that queued a reversal gets on with its transaction while the host is still
  answering.

About three lines. The tests hold the host's answer with a latch in one of them.

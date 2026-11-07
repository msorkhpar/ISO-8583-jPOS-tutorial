The core banking system now sends a message for the terminal's screen with every decision, and
its new release issues longer approval codes. Since the upgrade, some terminals get no answer at
all, and their acquirers reverse purchases the core system approved. The server's log shows a
`<send>` for each of them.

`CoreAnswer`, in `src/main/java/com/example/practice/CoreAnswer.java`, turns the core's
`Decision` into the response that `SendResponse` sends. The server runs it on several links, and
their layouts differ. Finish the two methods marked `TODO`:

- `packs(...)`: whether a message can go out where the request came from;
- `makeSendable(...)`: what to change when it can't.

When you're done, every decision reaches the terminal as close to what the core system said as
the link allows. About fifteen lines in total.

The tests put the channel the request came in on into the Context, as `IncomingListener` does,
and pack your response with that channel's packager.

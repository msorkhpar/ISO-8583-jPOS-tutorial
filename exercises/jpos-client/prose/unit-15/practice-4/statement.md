A new acquirer link, `acq-mux`, goes live next week, and the host's certification team ran their
network management script against it today. Their report:

- echoes, sign-ons, sign-offs, key changes and the nightly cutover advice all went unanswered;
- the client is required to stop sending purchases when the host signs the link off, and to sign
  on again once the host accepts sign-ons;
- the host must never receive a key back from a client, and must not be told a key is in place
  when it isn't;
- a function the client does not know must not be approved.

Write the listener that answers them: `NetworkRequests` in
`src/main/java/com/example/practice/NetworkRequests.java`. The host's rules for these messages are
copied into its class comment. They are not the same as the page's. Finish `process` and
`resultFor`, the two methods marked `TODO`. About twenty lines in total. The `KeyInstaller` you are
given does the security module's part.

The tests put the host's messages on the in queue of a real QMUX that has your listener, and read
what comes out on its out queue.

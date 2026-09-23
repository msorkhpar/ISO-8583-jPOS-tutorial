The page's `ClientLogger` builds a `LogEvent` and hands it to `logger.log(ev)`. In jPOS 2.1.7
`Logger.log` is static: it delivers an event to the logger of the event's **source**, and an
event with no source never reaches the listeners of the logger you meant. So the client's log
stays empty, and nothing tells you.

Write `ClientLogger`, built from a jPOS `Logger` and a realm, with
`log(String tag, String... messages)`. One call delivers exactly one event to that logger's
listeners. The event carries the tag, the realm and the messages, in the order given.

- A blank or `null` message is left out.
- A call with nothing left to say logs no event at all: an empty event in a transaction log
  is noise somebody has to read past.

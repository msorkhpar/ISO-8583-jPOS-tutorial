A jPOS `Logger` hands each `LogEvent` to its listeners one after another. A transaction log
is one such listener: it keeps the ISO messages of the events that are transactions and lets
everything else go by. Get it wrong and you either lose transactions from the log or, worse,
silently starve every listener added after yours.

Write `TransactionRecorder`, a `org.jpos.util.LogListener`:

- `log(LogEvent event)` records every `ISOMsg` in the payload (`event.getPayLoad()`, a list)
  of an event whose tag is `"TX"`; events with any other tag are not recorded;
- `recorded()` returns the recorded messages in the order they arrived.

Edge cases:

- The logger passes to the next listener whatever your `log` returns, and stops when it
  returns `null`. Every event, recorded or not, must reach the listeners after yours.
- A `TX` event's payload can carry more than the message, such as a text note. Record only
  the `ISOMsg` items, and do not fail on the rest.

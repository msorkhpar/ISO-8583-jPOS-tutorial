A pool of connections is there so a request does not pay for a connect. The page's pool
connects a client every time it is borrowed and disconnects it every time it is returned, so
every request still opens a fresh connection, and the pool saves nothing.

Write `ChannelPool` on Apache Commons Pool 2 (`org.apache.commons.pool2`), built from a
`Supplier<ISOChannel>` that makes a new, unconnected channel, and the most channels it may hold.

- `withChannel(work)` borrows a connected channel, runs `work.apply(channel)`, gives the channel
  back and returns what the work returned.
- A channel is connected once, when the pool makes it, and stays connected while it sits in the
  pool; returning it does not disconnect it.
- `active()` and `idle()` say how many channels are borrowed and how many wait in the pool.
- `close()` disconnects every channel the pool holds.

Edge cases:

- Work that throws still gives its channel back, and the exception reaches the caller unchanged.
  A channel that is never returned is a connection lost to the pool for good.
- A channel that dropped its connection while it sat in the pool is disconnected and thrown
  away, and a freshly connected one is used instead.

The acquirer is adding a second network link. Its ChannelAdaptor descriptor is ready, and
`LinkDescriptors.channelAdaptor(...)` in `src/main/java/com/example/practice/LinkDescriptors.java`
builds it. Nobody has written the multiplexer that goes on top of it yet, and the operations team
has three complaints about the last link someone set up by hand: requests timed out with the host
up, the dashboard said "connected" during an outage, and messages from the host that nobody was
waiting for ended up mixed with another link's.

Finish `muxFor(adaptor, muxName)` so that it returns the QMUX descriptor for the link that
`adaptor` describes, as the `20_` file in `deploy/` would hold it. The adaptors in the tests use
names and queues of their own, so read them from `adaptor` (`getAttributeValue`, `getChildText`).
About 8 lines.

The tests deploy what you return as a real QMUX, put a stand-in for the adaptor on its queues,
and send through it. A host behind the stand-in answers every request.

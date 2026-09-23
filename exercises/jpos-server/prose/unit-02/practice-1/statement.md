A switch sends the same kinds of message all day: purchases, balance inquiries. Each kind
always carries the same MTI, processing code, POS condition code and terminal id; only the
card, the amount and the trace number change. The page keeps one template per kind and builds
every message from it, then hands the message to a packager for the wire.

The page's `MsgFactory` is not part of jPOS 2.1.7, so you build the same idea on `ISOMsg`.
Write `MessageTemplates`:

- `new MessageTemplates(ISOPackager packager)` keeps the packager it is given.
- `define(String name, ISOMsg template)` remembers a template under a name.
- `newMessage(String name)` returns a new message that starts as a copy of that template:
  its MTI and every field the template has. The caller then sets the fields that vary.
- `pack(ISOMsg message)` returns the bytes the packager makes of the message, and
  `unpack(byte[] bytes)` returns a new message read back from such bytes.

Nobody sets the bitmap by hand: it has to describe exactly the fields the message carries at
the moment it is packed. Calling the packager straight on a message that has never had its
bitmap worked out fails in jPOS 2.1.7, so find how a message gets packed with its bitmap
right.

Why it matters: a template is shared by every message of its kind, so a message that leaks a
change back into it would quietly put one customer's card number into the next request.

- Changing a message you were given never changes the template, nor any message made from it
  afterwards.
- Asking for a name that was never defined is refused with an `IllegalArgumentException`.
  An empty message would get as far as the wire before anyone noticed.

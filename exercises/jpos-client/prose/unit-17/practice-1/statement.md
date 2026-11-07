Last month a test built from a ticket passed, the ticket was closed, and the purchase failed again
the next morning. The bytes in the test were not the bytes in the ticket, and nothing had said so.

From now on the team turns every dump into bytes with `HexDumpReader.read`, in
`src/main/java/com/example/practice/HexDumpReader.java`. The dumps it gets come from the
acquirer's support desk (made with `hexdump -C`), from our own channel's log events, and from
colleagues who paste them into chat and mail. `read` already splits a dump into lines, skips blank
ones and collects the bytes. Finish the two methods marked `TODO`:

- `parse`: one line of a dump, as the offset it states and the bytes it shows;
- `check`: whether that line can come next.

When you're done, `read` returns exactly the bytes the dump shows, or throws
`IllegalArgumentException` when it can't be sure of them. It never returns other bytes. About 25
lines in total.

The tests use an authorization from another terminal, dumped by jPOS and by `hexdump -C`, copied
whole, with lines lost or pasted twice, with a byte position damaged, and with text columns that
happen to look like hex.

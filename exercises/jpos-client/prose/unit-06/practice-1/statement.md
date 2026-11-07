The fuel stations' new pay-at-pump terminals talk to `TerminalBridge`, in
`src/main/java/com/example/practice/TerminalBridge.java`. Their software sends everything as hex
text, and the PIN pad driver hands over each PIN block in a buffer that it fills again for the next
customer. The client sends to two hosts: the course's ASCII link and a second link whose packager
treats field 55 as binary.

`purchase` already builds the request. Finish the three methods marked `TODO`:

- `putChipData`: the chip's data into the request;
- `putPinBlock`: the PIN block into the request;
- `chipDataForTerminal`: the host's chip data, from field 55 of a response, back to the terminal.

When you're done, both hosts receive exactly the bytes the chip produced and the PIN block each
customer typed, even when a request is packed after the next customer has used the PIN pad. The
terminal gets the host's chip data as hex from either link, and `null` when the host sent none.
About five lines in total.

The tests pack each message with the link's packager and unpack it again, as the other end does.

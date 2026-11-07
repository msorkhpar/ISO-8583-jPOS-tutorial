A new acquirer link goes into pilot. Its interface spec says: "Every message is preceded by its
length in two bytes, binary, most significant byte first. The length does not include the two
length bytes. When the link is idle the host sends a message of length zero."

Our reader for that link, `FrameReader` in `src/main/java/com/example/practice/FrameReader.java`,
was never finished: its three methods are marked `TODO`. The comment on each says what it is for.
Finish them so that `next()` returns each message the host meant, exactly as the host sent it,
whatever the network does to the bytes on the way, and fails loudly when the connection drops in
the middle of a message.

About 14 lines in total. The tests feed the reader streams that behave like real connections: some
hand out a few bytes per read, some deliver several messages at once.

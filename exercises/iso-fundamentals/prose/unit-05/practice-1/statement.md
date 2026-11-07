The support desk wants to see what Visa's side of a link really sends, frame by frame, instead of
what the application logged after unpacking. A small pass-through will sit on the connection: it
reads everything the network sends and hands each frame to the desk as two byte arrays, the header
bytes and the message bytes. Because it sits on a live link, it has to treat everything that
arrives the way jPOS's `VAPChannel` does, or the link suffers.

`Base1Frames` in `src/main/java/com/example/practice/Base1Frames.java` already reads the prefixes and
returns the frames. Finish the three methods marked `TODO`:

- `poll`: what to do with a prefix that has no message behind it;
- `readHeaders`: every header byte in front of the message, on any frame the network may send;
- `messageLength`: how many message bytes follow.

About 20 lines in total.

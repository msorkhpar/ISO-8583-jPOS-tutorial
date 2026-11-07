A network has started declining some chip transactions, and the support desk is reading DE 55 from
hex dumps in the logs. They want a small tool that prints each object in the dump, and that says
so when a dump is damaged instead of printing a guess, because a guess is what they would act on.

`TlvReader` in `src/main/java/com/example/practice/TlvReader.java` already walks the data and
collects the objects. Finish the three methods marked `TODO`:

- `tagEnd`: where the tag that starts at a position ends;
- `lengthAt`: the value's length, and where the value starts;
- `value`: the value's bytes.

Dumps come from several card schemes, so expect tags and lengths beyond the common ones. Refuse what
can't be split with an `IllegalArgumentException`; `byteAt` already refuses to read past the end.
About 15 lines in total.

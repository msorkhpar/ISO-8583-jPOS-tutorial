The certification team tests a new receiver by feeding it raw ISO 8583 messages that a small tool
writes by hand, without a packager. The receiver is strict: anything it can't read exactly as its
specification says, it rejects, and a message the tool gets wrong costs a test slot. The tool's
`FieldWriter` in `src/main/java/com/example/practice/FieldWriter.java` writes one field at a time,
from the format as the specification gives it (`n4`, `ans15`, `n..28`, `ans...300`) and the value.

It already parses the format. Finish the three methods marked `TODO`:

- `allowed`: whether the receiver would accept the value's characters for that type;
- `fixed`: a fixed field, as the receiver will take it;
- `variable`: a variable field, as the receiver will take it.

When the receiver would reject a value, `write` must refuse it with an `IllegalArgumentException`
rather than send something else. About 15 lines in total.

Your client is getting a second link, to a regional debit switch. Its layout differs from the
acquirer's in four rows, and `SwitchPackager` in `src/main/java/com/example/practice/SwitchPackager.java`
already has every other row. Finish the four methods marked `TODO`. Each returns the field packager
for one row, and the comment above each method quotes the row from the switch's specification:

| Field | Specification |
|---|---|
| 2 PAN | n..19, LLVAR. Length and digits in BCD. |
| 41 terminal ID | ans 8. Text on this switch is EBCDIC. |
| 52 PIN block | b 64 |
| 70 network management code | n 3, BCD, left-justified (the filler nibble last) |

The specification comes with sample messages. Here is its sign-on, for code `001`:

```text
08 00 80 20 00 00 00 00 00 00 04 00 00 00 00 00 00 00 00 09 14 00 10
```

When you're done, the switch's samples read as the specification documents them and pack back to
the same bytes. About four lines in total.

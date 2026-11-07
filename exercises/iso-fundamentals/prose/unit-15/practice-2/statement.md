One approved message is a thin reference. Approved purchases differ among themselves: some cards
have 19 digits, some terminals send an expiry date and others don't. Compared with one of them, a
rejected purchase can show differences that the host accepts every day.

So the support desk keeps the morning's approved purchases from the same release, and
`Shapes.unseen(approved, rejected)` lists the fields of a rejected message whose shape none of them
has. `unseen` is written. Finish the two methods marked `TODO` in
`src/main/java/com/example/practice/Shapes.java`:

- `shape(...)`: one field described in one of the forms the comment above it lists;
- `seen(...)`: every description a field has across the approved messages.

About twelve lines in total. Some tests' messages carry a PIN block, which is binary.

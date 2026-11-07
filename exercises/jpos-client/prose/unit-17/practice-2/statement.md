Before each release the team replays a day of recorded requests through the new build, against a
host simulator like the ones in unit c16, and compares every response the new build gets with
the response recorded for the same request. Some fields differ on every run by design: the
transmission time, the STAN, the retrieval reference number. The release fails when anything else
differs.

The check has started failing releases that changed nothing, and nobody reads its report any
more. Finish the three methods marked `TODO` in
`src/main/java/com/example/practice/ReplayCheck.java`:

- `fields`: which fields to compare;
- `same`: whether two values of a field are equal;
- `nested`: a field that carries sub-fields.

`differences` returns the paths on which the two responses differ (`"39"`, `"48.2"`), in field
order, leaving out the paths it was told change every run. When you're done, a replay that matches
its recording returns an empty list, and a replay that doesn't names exactly the paths that
changed. About ten lines in total.

The tests compare responses built in code and responses unpacked from the bytes the simulator
sent, with chip data and with sub-fields.

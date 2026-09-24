# ISO-31 handoff: the aggregate pages match their parts, asserted by digest (AX-11/1)

**Office** `po-int`. **Branch** `int/m10-iso-23`, from `b4d6b31`. **Framework** the corpus pin
`39f12d4e`.

## The claim, and why it was false

`corpus.json` excludes `src/ISO.md`, `src/Client.md` and `src/Server.md` from the material. Each
one, it says, is a *"whole-series aggregate: the exact ordered concatenation of"* its parts,
*"asserted by digest"*. **Nothing asserted it.** Since `f6ad016` (ISO-27), the fix rounds edited
the parts and never the aggregates, so **58 fences differed**:

| aggregate | its parts | fences | differing before | differing after |
|---|---|---|---|---|
| `src/ISO.md` | `src/1.md` … `src/16.md` | 83 | 14 | 0 |
| `src/Client.md` | `src/c1.md` … `src/c11.md` | 67 | 20 | 0 |
| `src/Server.md` | `src/s1.md` … `src/s11.md` | 68 | 24 | 0 |

No record in the repository shows how the aggregates were first produced: they arrived with the
tutorial. So the rule was **measured, not assumed**:

- **Where:** at `1a31fbe`, the last commit before any part was edited.
- **What was found:** each part sits in its aggregate verbatim and in order. Only the aggregate's
  own runs of newlines separate them (0 to 4 per boundary, varying), and a run of newlines ends
  the file.
- **The check:** with those runs recorded, the rule rebuilds all three aggregates at `1a31fbe`
  byte for byte (their SHA-256 values match).

A plain join with a fixed separator does not, which is why the runs are recorded data.

## What changed (`7d6f922`)

- **`tests/test_aggregates.py`** (new) records the rule: each aggregate's parts in order, the
  newline run before each, and the run at the end.
  - `test_every_aggregate_is_its_parts_by_digest`: each aggregate's SHA-256 must equal that of
    its parts under the rule. A failure prints a unified diff of the aggregate against its
    parts, and names the command that regenerates it. **This makes "asserted by digest" true.**
  - `test_the_check_fires_on_a_one_character_drift`: it changes one character of one part in
    memory and shows the digest disagreeing. It is a standing negative control, so the check
    cannot rot into a pass.
  - `test_the_parts_are_the_manifests_own_exclusions`: the three aggregates are exactly the
    manifest's "whole-series aggregate" exclusions, so a fourth one needs a line in the test.
- **The aggregates are regenerated, not hand-edited:** `python3 tests/test_aggregates.py
  --write`. Differing fences went from 58 to 0.
  - ⛔ **This is now the one way to update them after any part changes.** Every earlier "fix
    them" round would have needed it.

## Positive control, and the plant

- **Before the regeneration** (the drifted tree), the new test was **RED, exit 1**. It printed
  the diff, e.g. the `MUX.request` bullet and the `(Context) context` cast that c4 and s4 gained.
- **Planted on disk after the regeneration:** one character in `src/Server.md`. This is the diff
  of the plant against the regenerated file, printed before the run was read:
  ```
  435c435
  < ## 3.2 NAC Channel
  ---
  > ## 3.2 MAC Channel
  ```
  The test on the planted tree was **RED, exit 1**, and printed:
  ```
  --- src/Server.md (on disk)
  +++ src/Server.md (its parts)
  @@ -434,3 +434,3 @@
  -## 3.2 MAC Channel
  +## 3.2 NAC Channel
  ```
  After the file was restored, the test was GREEN (3 passed). The plant was never committed.

## Rendered or narrated? Neither

- **Not rendered.** Each container's section page (e.g. `client-implementation.section.html`)
  is a list of contents and carries none of the aggregate's text: a sentence unique to c3's intro
  appears in the unit page and not in the section page.
- **Not narrated.** The narration record has no clip id outside the 38 units' own.
- **Named only as a path.** The container maps name each aggregate only as the container's
  `origin` path, with no digest of it. `ingest/read.py` reads that path from the README's
  record.
- So ISO-30's join has nothing to re-narrate here, and it stays at 0 stale and 0 silent.

## Gates, at `7d6f922`

- `studyforge validate .`: **GREEN, exit 0.**
- The corpus suite: **GREEN, exit 0**, 38 passed (35 before, plus the 3 new tests).
  - ⚠️ Before the commit, the suite read RED (2 failed). `test_non_destructive` reads the
    *uncommitted* working tree and flags the rewritten aggregates until they are committed; the
    second failure did not recur on the clean tree. As in ISO-23, the committed tree is the
    reading.

## For the register

- `corpus.json`'s wording is now true. It was left unedited, because the manifest is reonboard's
  output.
- Any future edit to a part must be followed by `python3 tests/test_aggregates.py --write`, and
  the suite enforces it.
- A framework-level check could do the same for any corpus that declares an aggregate. That
  would need a manifest field for the parts, which this corpus carries only as prose.

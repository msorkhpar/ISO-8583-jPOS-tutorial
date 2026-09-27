# ISO-M9-13 hand-off: re-pin to W512 and the W513 editor

**Office** `M9-13`. **Branch** `main`, from `6a930f1`. The tip is the commit that adds this file.
Nothing was pushed, and no remote was touched.

**Outcome:** the corpus is re-pinned to studyforge `27e2d0d2`, which adds W512 (every text writer
writes LF, and the container acceptance tests stage through the engine harness). The editor is
rebuilt from code-server-toolchain `fd9ee52`, which adds W513 (the editor's secondary side bar
never opens, so no Chat panel paints while it starts). The editor and site tags moved and both
images were rebuilt; the runner tag did not move. Narration did not change: no clip was written
or retired. The site is up on Docker Desktop on :8770, and every gate is GREEN.

## Where and with what

- **Library:** studyforge 0.1.0, built from `27e2d0d25f5546c6c6a500701a8e95462e7cb80e`, from a
  scratch clone into a fresh scratch venv, following the README (its pinned `pytest` set included).
- **Toolchain:** `code-server-toolchain` at `fd9ee52` (`provides: 4`), from a `git archive` in
  scratch. **narrate-service** `consuming.json` at `48017c1`, the same way, unchanged since M9-12.
- **Containers:** by register ruling they run on Docker Desktop. Every docker command passed
  `--context desktop-linux`, and the toolchain build ran with `DOCKER_CONTEXT=desktop-linux`.

## Commits (all `M9-13 <m9-13@example.invalid>`)

| commit | what |
|---|---|
| `418fa71` | `reonboard(framework_commit=27e2d0d2…)`, step 5b `stage_site`, then steps 5 and 5a: `write`, `record_runner`, `record_editor` |
| this commit | this hand-off |

There is no build commit: `studyforge build . --out .` at `27e2d0d2` rewrote no byte of the tree.

## 1. Re-pin and images

- `reonboard('.', framework_commit=…).write('.', regenerate=True)` moved the pin, the three skill
  digests and `docs/archive/ONBOARDING.md`. `hand_edited` read `[]` before and after.
- The execution skill's `write` changed no byte of `compose.yaml`, `EXECUTION.md` or the prime.
  Only `editor.env`, `site.env` and `written.json` moved.

| image | was | now |
|---|---|---|
| site | `studyforge-site:0.1.0-6d690c2743a0` | `studyforge-site:0.1.0-b88e74c622f4` |
| editor | `…editor:java-maven-amd64-3dc866065990` | `…editor:java-maven-amd64-e6537b160c73` |
| runner | `…runner:java-maven-amd64-9872bf544630` | unchanged |

- The editor was built with the toolchain's `docker/editor/build.py --runtimes java,maven --prime
  <this corpus>/.studyforge/execution/prime`. It printed the recorded tag, the lockdown step
  passed, and nothing was pulled.
- The site was built with EXECUTION.md's `… build site`, with `--context desktop-linux`.
- Removed once nothing used it: editor `3dc866065990`. The Java hand-off `M9-13.md` lists the
  Java editor and the old site image it removed.

## 2. Narration

- The count used the library's own walk, with no service and no write: 1,151 record entries,
  1,151 units spoken, **0 dead, 0 owed, 0 superseded**.
- `studyforge narrate . --prune`: `0 clip(s) deleted, 0 record entries removed, 0 held`.
  `.studyforge/narration.json` did not change.
- No narration service was started. **There is no pack** (ISO-M9-10/1 stands).

## 3. Gates

| gate | result |
|---|---|
| `studyforge validate .`, before and after the build | **GREEN**: `valid: 0 finding(s), 0 unchecked claim(s)` |
| `studyforge plan .` | **GREEN**: 0 to create, 43 to replace, 1,191 to keep, 152 claimed, 0 superseded clips, 0 refusals |
| `studyforge build . --out .` | **GREEN, exit 0**; no file changed |
| `studyforge build . --out .`, again | **GREEN, exit 0**; the tree is clean after it |
| `hand_edited('.')` | **GREEN**: `[]` |
| generated tests, `python3 -m pytest tests` | **GREEN**: 39 passed in 33 s |
| page references | **GREEN**: 38 pages, 1,151 `data-audio` references, 0 missing, 0 inside a code-examples block, every clip referenced |
| personal data | **GREEN**: no committed file of this step holds a home path, host name or email |

## 4. The site, live

- From the corpus root, EXECUTION.md's compose command with `--context desktop-linux`: `down`,
  then `up -d --wait`, **exit 0**. The site, editor and runner report healthy, and :8770 answers **200**.
- A Submit through the runner, `POST /api/v1/run/<corpus>/test/jpos-server/unit-09/practice-prose`,
  ran `TransactionStorageParticipantTest`. Both of its tests stopped on the starter's
  `UnsupportedOperationException`. The tree stayed clean.
- **The W510 workspace** was driven with the framework's own DevTools harness (`tests/visual`),
  real pointer input, headless Chrome at 1440x900 against :8770:
  - the editor frames the practice (the child frame is `http://127.0.0.1:8443/`);
  - the left divider drags: the description went from 605 px to 405 px, and `aria-valuenow` read 28;
  - *Hide description* closes it (the panel is then 1,404 px wide), and the edge opens it again
    at the width it had;
  - Submit reports `0/2 passed`. The report divider drags (the body went from 326 px to 206 px);
    the bar collapses the report to *Show report 0/2 passed*, and reopens it.
- **W513** was read live on the Java course's framed editor (see `M9-13.md` there): no Chat panel
  and no secondary side bar in any frame from first paint, on a first start and after a reload.
  This corpus's editor is built from the same toolchain commit.

## Findings

| id | concerns | finding |
|---|---|---|
| ISO-M9-13/1 | studyforge `build site` | **Low.** The site build is not reproducible. Both corpora stage a byte-identical context under one tag, but each `… build site` makes a new image. The second build left the Java site on an untagged image until it was brought up again. Build the shared tag once, or bring every course up again after the last build. |
| ISO-M9-13/2 | studyforge `archive/process:workspace.json` (carried: ISO-M9-12/2) | **Low.** It still pins code-server-toolchain at `6e226d0`. The editor tag here comes from `fd9ee52`. |
| ISO-M9-13/3 | `studyforge narrate` (carried: ISO-M9-12/3) | **There is still no count-only mode.** |

# ISO-M9-12 hand-off: re-pin to W509, W510 and W511

**Office** `M9-12`. **Branch** `main`, from `dbc6dc8`. The tip is the commit that adds this file.
Nothing was pushed, and no remote was touched.

**Outcome:** the corpus is re-pinned to studyforge `ea081dbd`. That commit carries W509 (no
container mounts a host temp directory), W510 (the practice workspace's description resizes and
closes, and its report collapses) and W511 (the site image builds through compose from
`site.env`, and the editor and runner carry `org.studyforge.binds` labels). The editor and site
tags moved and both images were rebuilt; the runner tag did not move. Narration did not change:
no clip was written or retired. The site is up on Docker Desktop on :8770, and every gate is GREEN.

## Where and with what

- **Library:** studyforge 0.1.0, built from `ea081dbdb386b3991a15415ac088901eb8a9027c`, from a
  scratch clone into a fresh scratch venv, following the README (its pinned `pytest` set included).
- **Toolchain:** `code-server-toolchain` at `bc64c0e` (`provides: 4`), from a `git archive` in
  scratch. **narrate-service** `consuming.json` at `48017c1`, the same way. It is byte-identical
  to `62725dd`'s.
- **Containers:** by register ruling they run on Docker Desktop. Every docker command passed
  `--context desktop-linux`, and the toolchain builds ran with `DOCKER_CONTEXT=desktop-linux`.

## Commits (all `M9-12 <m9-12@example.invalid>`)

| commit | what |
|---|---|
| `4114d32` | `reonboard(framework_commit=ea081dbd…)`, step 5b `stage_site`, then steps 5 and 5a: `write`, `record_runner`, `record_editor` |
| `4c39dac` | the build: `page.css`, `page.js` and the 35 pages that carry a practice (W510 markup only) |
| this commit | this hand-off |

## 1. Re-pin and images

- `reonboard('.', framework_commit=…).write('.', regenerate=True)` moved the pin, the three skill
  digests and `docs/archive/ONBOARDING.md`. `hand_edited` read `[]` before and after.
- The execution skill's `write` rewrote `compose.yaml`: each of the editor and the runner now
  carries an `org.studyforge.binds` label, and the `site` service has a `build`
  (`context: ./site`). `EXECUTION.md` now prints one-line commands that also run in PowerShell,
  including `… build site`. `write` rewrote no byte of the prime.

| image | was | now |
|---|---|---|
| site | `studyforge-site:0.1.0-74be4ce462a2` | `studyforge-site:0.1.0-6d690c2743a0` |
| editor | `…editor:java-maven-amd64-ac345c02d1b2` | `…editor:java-maven-amd64-3dc866065990` |
| runner | `…runner:java-maven-amd64-9872bf544630` | unchanged |

- The editor was built with the toolchain's `docker/editor/build.py --runtimes java,maven --prime
  <this corpus>/.studyforge/execution/prime`. It printed the recorded tag, and the lockdown probe
  passed. `--print-tag` on `docker/minimal/build.py` confirmed the runner tag.
- The site was built with EXECUTION.md's `… build site`, with `--context desktop-linux`.
- The superseded course images were removed after `down`/`up`, because nothing used them:
  editor `ac345c02d1b2`, `383bad672b8d` and `418405a27fd2`; site `74be4ce462a2` and
  `a73499c28f4b`; runner `57f9563fbd20`.

## 2. Narration

- The count used the library's own walk, with no service and no write: 1,151 record entries,
  1,151 units spoken, **0 dead, 0 owed, 0 superseded**.
- `studyforge narrate . --prune`: `0 clip(s) deleted, 0 record entries removed, 0 held`.
- No narration service was started. **There is no pack** (ISO-M9-10/1 stands).

## 3. Gates

| gate | result |
|---|---|
| `studyforge validate .`, before and after the build | **GREEN**: `valid: 0 finding(s), 0 unchecked claim(s)` |
| `studyforge plan .` | **GREEN**: 0 to create, 43 to replace, 1,191 to keep, 152 claimed, 0 superseded clips, 0 refusals |
| `studyforge build . --out .` | **GREEN, exit 0**, committed in `4c39dac` |
| `studyforge build . --out .`, again | **GREEN, exit 0**; the tree is clean after it |
| `hand_edited('.')` | **GREEN**: `[]` |
| generated tests, `python3 -m pytest tests` | **GREEN**: 39 passed in 33 s |
| page references | **GREEN**: 38 pages, 1,151 `data-audio` references, 0 missing, 0 inside a code-examples block, every clip referenced |
| personal data | **GREEN**: no committed file of this step holds a home path, host name or email |

## 4. The site, live

- From the corpus root, EXECUTION.md's compose command with `--context desktop-linux`: `down`,
  then `up -d --wait`, **exit 0**. The site, editor and runner report healthy, and :8770 answers **200**.
- The run index names the editor (`127.0.0.1:8443`, folder `/home/coder/repo/code`), which was
  found by its labels.
- A Submit through the runner, `POST /api/v1/run/<corpus>/test/jpos-server/unit-09/practice-prose`,
  ran `TransactionStorageParticipantTest`. Both of its tests stopped on the starter's
  `UnsupportedOperationException`, as an unsolved starter should. The tree stayed clean.
- **The W510 workspace** was driven with the framework's own DevTools harness (`tests/visual`),
  using real pointer input on headless Chrome at 1440x900 against :8770:
  - the editor frames the practice (the child frame is `http://127.0.0.1:8443/`);
  - the left divider drags: the description went from 605 px to 405 px, and `aria-valuenow` read 28;
  - *Hide description* closes it (the panel is then 1,404 px wide, and the edge shows *Show
    description*), and the edge opens it again at the width it had;
  - Submit reports `0/2 passed`. The report divider drags (the body went from 326 px to 206 px);
    the bar collapses the report to *Show report 0/2 passed*, and reopens it.
- **W510/1:** after a drag, the title shows **no focus outline** across the top bar. That holds in
  the headless run, and in a real drag in a headed Chrome with the title focused first
  (`:focus-visible` false, outline `none`).

## Findings

| id | concerns | finding |
|---|---|---|
| ISO-M9-12/1 | code-server-toolchain (closes ISO-M9-11/1 and /2) | **Closed.** The build scripts honoured `DOCKER_CONTEXT`, and the lockdown probe passed from the default temp directory. No shim and no `TMPDIR` were needed. |
| ISO-M9-12/2 | studyforge `archive/process:workspace.json` | **Low.** It still pins code-server-toolchain at `6e226d0`, but the editor tag recorded here comes from `bc64c0e`. The corpus records no toolchain commit, only the tags. |
| ISO-M9-12/3 | `studyforge narrate` (carried: ISO-M9-11/3) | **There is still no count-only mode.** |
| ISO-M9-12/4 | Docker Desktop images | Two images that no course records were left in place: `…runner:java-maven-amd64-a3fcf99d2902` and `…runner:python-amd64-f8f1db0be48a`. `…runner:java-maven-amd64-f8f1db0be48a` is the editor's unprimed base, and it was kept. |

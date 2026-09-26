# ISO-M9-11 hand-off: re-pin to W508, primed images rebuilt

**Office** `M9-11`. **Branch** `main`, from `9ac199f`. The tip is the commit that adds this file.
Nothing was pushed, and no remote was touched.

**Outcome:** the corpus is re-pinned to studyforge `16c1a67a`, which carries W508: a code-example
panel is never narrated. No page of this corpus carries a code-example panel, so no clip was
retired and none was written. The scrubbed primed pom (`9ac199f`) moved the runner and editor
tags; both images were rebuilt at the re-pin, and the site is up on Docker Desktop on :8770.
Every gate is GREEN.

## Where and with what

- **Library:** studyforge 0.1.0, built from `16c1a67aad12772b04bfaedd8380021af2a1aef0`, from a
  scratch clone into a fresh scratch venv, following the README (its pinned `pytest` set included).
- **Toolchain:** `code-server-toolchain` at its pinned `6e226d0`, from a `git archive` in scratch.
  **narrate-service** `consuming.json` at `62725dd`, the same way.
- **Containers:** by register ruling they run on Docker Desktop, and every docker command passed
  `--context desktop-linux`, the image builds included (see ISO-M9-11/1).

## Commits (all `M9-11 <m9-11@example.invalid>`)

| commit | what |
|---|---|
| `e8bd90c` | `reonboard(framework_commit=16c1a67a…)`, step 5b `stage_site`, then steps 5 and 5a: `write`, `record_runner`, `record_editor` |
| `b4489bd` | the build: only `page.css` and `page.js` changed |
| this commit | this hand-off |

## 1. Re-pin and images

- `reonboard('.', framework_commit=…).write('.', regenerate=True)` moved the pin, the three skill
  digests and `docs/archive/ONBOARDING.md`. `hand_edited` read `[]` before and after.
- **Step 5b:** site tag `studyforge-site:0.1.0-74be4ce462a2` (it was `…-a73499c28f4b`), built with
  `docker --context desktop-linux build --pull=false`.
- **Steps 5 and 5a**, with the toolchain's and narrate-service's `consuming.json`: `write` rewrote
  no byte of the prime (the pom already matched), and the tags moved:

| image | was | now |
|---|---|---|
| runner | `…runner:java-maven-amd64-57f9563fbd20` | `…runner:java-maven-amd64-9872bf544630` |
| editor | `…editor:java-maven-amd64-418405a27fd2` | `…editor:java-maven-amd64-ac345c02d1b2` |

- Both were built from the toolchain's `docker/minimal/build.py` and `docker/editor/build.py`
  with `--runtimes java,maven --prime <this corpus>/.studyforge/execution/prime`. Each printed the
  recorded tag, and the editor's lockdown probe passed (activation and banner both read).

## 2. Narration

- The count (the library's own walk, no service, no write): 1,151 record entries, 1,151 units
  spoken, **0 dead, 0 owed, 0 superseded**.
- `studyforge narrate . --prune`, **exit 0**: `0 clip(s) deleted, 0 record entries removed, 0 held`.
- No narration service was started. **There is no pack** (ISO-M9-10/1 stands).

## 3. Gates

| gate | result |
|---|---|
| `studyforge validate .` | **GREEN, exit 0**: `valid: 0 finding(s), 0 unchecked claim(s)` |
| `studyforge plan .` | **GREEN**: 0 to create, 43 to replace, 1,191 to keep, 152 claimed, 0 superseded clips, 0 refusals |
| `studyforge build . --out .` | **GREEN, exit 0**, committed in `b4489bd` |
| `hand_edited('.')` | **GREEN**: `[]` |
| generated tests, `python3 -m pytest tests` | **GREEN**: 39 passed in 34 s |
| page references | **GREEN**: 38 pages, 1,151 `data-audio` references, 0 missing, 0 inside a code-examples block, every clip referenced |
| the site | **GREEN**: EXECUTION.md's compose command, `up -d --wait`, exit 0; :8770 answers 200; a Submit of `jpos-server/unit-09/practice-prose` ran its tests in the runner (the starter fails them, as it should) |
| personal data | **GREEN**: no committed file of this step holds a home path, host name or email |

## Findings

| id | concerns | finding |
|---|---|---|
| ISO-M9-11/1 | code-server-toolchain build scripts | **They call a bare `docker`.** There is no way to name a context, so the builds ran through a scratch `docker` shim that passes `--context desktop-linux` on every call. |
| ISO-M9-11/2 | code-server-toolchain, editor lockdown probe on Docker Desktop | **The probe cannot run from a default temp directory on Docker Desktop.** It bind-mounts a `tempfile` directory, and Desktop does not share `/tmp`: `mounts denied`. With `TMPDIR` in a shared but long path, headless Chrome died at once (`Socket path too long` for its SingletonSocket). The probe sends Chrome's output to `/dev/null`, so that read only as "the extension host activated it in no session". A short shared `TMPDIR` passed. Two untagged probe-refused images were removed. |
| ISO-M9-11/3 | `studyforge narrate` (carried: ISO-M9-10/3) | **There is still no count-only mode.** |

# ISO-37 handoff: a follow-up regeneration onto `3c574952`, with the changed clips re-narrated

**Office** `po-int`. **Branch** `int/iso-37-regenerate`, from `main` `dbc003e` (ISO-36).
**Framework** `studyforge` 0.1.0, built from `3c574952`. **Toolchain** `code-server-toolchain` at
`181164f`. **narrate-service** at `62725dd`.

| commit | what |
|---|---|
| `f47e8db` | re-onboard, the execution regenerate, 323 clips narrated and 323 pruned, and the build |
| this commit | this handoff |

## How the library was installed

This is the same method as ISO-36. `studyforge` was cloned into scratch with
`git clone --no-local` and checked out at `3c574952`. A wheel was built from that clone with
`pip wheel --no-build-isolation --no-index`. It went into a fresh venv with `--no-index --no-deps`,
which uses `--system-site-packages` for the host's pytest (9.0.2). `PYTHONPATH` was unset.
`library.commit()` reported `3c574952efd7bb6989fcbbd34bd3b1b2a6dfe8fe`.

## Images

⚠️ **Both tags move.** Under `/tmp/studyforge-container-gate.lock`, with `TC_KEEP_IMAGES=1`, from
toolchain `181164f`:

| image | command | tag, and `--print-tag` / the skill's record |
|---|---|---|
| runner | `python3 docker/minimal/build.py --runtimes java,maven --prime <worktree>/.studyforge/execution/prime --pull never` | `code-server-toolchain/runner:java-maven-amd64-230dc78178c2`, exit 0; the same in `runner.env` |
| editor | `python3 docker/editor/build.py --runtimes java,maven --prime <worktree>/.studyforge/execution/prime --pull never` | `code-server-toolchain/editor:java-maven-amd64-415f4b759fae`, exit 0, with activation and confinement proved; the same in `editor.env` |

Why the runner moved: between `497b4cd` and `181164f`, `docker/minimal/Dockerfile` changed in
exactly one comment line (a citation reworded). The runner's tag digests its inputs, comments
included (`ISO-37/2`). The editor moved for `editor.fontLigatures: false`, and for its runner base.

## What was done

1. `reonboard('.', framework_commit='3c574952…').write('.', regenerate=True)`. `hand_edited` read
   `[]` before and after. `ingest/__init__.py` now says it depends on the installed library, which
   closes ISO-36/8.
2. The execution regenerate through the skill (`generate`, `write`, `record_runner` and
   `record_editor`), exactly as in ISO-36:
   - it read the toolchain's `consuming.json` from a `git archive` of `181164f`, and
     narrate-service's from `62725dd`;
   - `EXECUTION.md` was accepted as the skill's own this time, and it rewrote byte-identical;
   - `compose.yaml` was unchanged.
3. `python3 -m ingest . 2026-09-21` changed no archive byte. Outline numbers are removed at render
   time, not in the archive. `validate` then reported 323 `narration-stale` findings.
4. **Narration.**
   - This row's own service ran from narrate-service's compose files, as its README describes,
     with the GPU overlay. A scratch overlay changed three things:
     - project `iso37-narrate`;
     - `127.0.0.1:18871` in place of the published 8870;
     - `image: narrate-service:rel13-da8fa84`, with no build.
   - That image's `narrate/` package is byte-identical to `62725dd`'s. `narrate-service:local` is
     older and differed in 11 files.
   - It started with `--pull never --no-build`; the engine image was already present by its
     digest.
   - `studyforge narrate . --voice am_liam --service http://127.0.0.1:18871` exited 0: **323
     written, 1598 already synthesised**.
   - `studyforge narrate . --prune` exited 0: **323 superseded clips deleted**, 0 record entries
     removed.
   - The service was then brought down with `down -v`. No container, network or volume of it
     remains.
   - No `--pack` or `--publish` was run.
   - The 1921 clips the pages name all exist on disk.
5. `build . --out .`.

## Actual diff

- The pin, the stubs, `installed.json`, the commit sentence in `ONBOARDING.md`, `ingest/__init__.py`
  and the generated `tests/test_non_destructive.py` moved.
- `editor.env` carries `…-415f4b759fae`, and `runner.env` carries `…-230dc78178c2`. `written.json`
  records both digests.
- **Clips: 323 added and 323 deleted** under `src/study/audio/`. `.studyforge/narration.json`
  re-points those 323 entries.
- The build changed:
  - `page.css`: no ligatures in code, and the grouping by section;
  - `index.html`: `--segments: 3`;
  - all 43 unit and section pages, where headings lose their outline numbers and the clip names
    move.
- The archive is unchanged. `corpus.json` is unchanged: `linked` (`corpus_api` 8) and `curriculum`
  were not adopted, because nothing in the skills asks this corpus to.

## Gates

On the host, installed library `3c574952`, in this worktree.

| gate | reading |
|---|---|
| `python3 -m studyforge.skills.onboarding.verify .` | GREEN, exit 0 |
| `python3 -m pytest tests/test_framework_pin.py` | GREEN, exit 0 |
| `python3 -m studyforge.cli validate .` | GREEN, exit 0: 0 findings, narration included. Before the narration it was 323 `narration-stale` |
| `python3 -m studyforge.cli plan .` | GREEN, exit 0 |
| `python3 -m studyforge.cli build . --out .` | GREEN, exit 0. After the commit, a second build left the tree clean |
| the corpus suite, before the commit | GREEN, exit 0, 39 passed |
| the corpus suite at `f47e8db` | GREEN, exit 0, 39 passed |
| `hand_edited('.')` | `[]` |
| the compose `config --images` (runner, editor and instance env files; neither image variable set) | GREEN, exit 0: `…/runner:java-maven-amd64-230dc78178c2`, `…/editor:java-maven-amd64-415f4b759fae` |
| `python3 -m studyforge.look . --out <scratch> --all` | GREEN, exit 0, 44 of 44 pages. What the screenshots show is in `ISO-37/1` and `ISO-37/3` |

**The screenshots.**
- Index: the grouped progress bar and the rail by section.
- Fundamentals unit 2: the headings "Message Type Indicator (MTI)", "Four-digit structure" and
  "Bitmaps" carry no "2.1."-style numbers, and the "Play narration" bar is present (1 / 57).
- Client unit 8: see `ISO-37/1`.

## Findings

| id | where | what |
|---|---|---|
| `ISO-37/1` | studyforge, W485's outline-number stripping | **Medium.** Two headings keep their number: `## 8.1 jPOS Logging Framework` in client unit 8 and in server unit 8. The page shows "8.1 jPOS Logging Framework", and its clip (`…prose.b3-d7ea7df1`) says it. Every other numbered heading, including "8.1.1 Logger" on the same page, is stripped. **This is not measured, only suspected:** the word after the number starts with a lowercase letter ("jPOS"). The page's outline box shows it too. Framework row. Once fixed, a regenerate re-narrates 2 clips |
| `ISO-37/2` | toolchain, the runner's tag digest | Low. A comment-only change to `docker/minimal/Dockerfile` moved the runner tag, and with it a rebuild and a recreate for every consumer. That may be intended, since the digest covers inputs byte for byte. Toolchain's ruling |
| `ISO-37/3` | studyforge `look --all` | Low. It walked this worktree's ignored `.scratch/` and rendered two stray pages from it (`.scratch/r10/chrome/plant.unit.html` and `.scratch/r16/…`). The site `build` writes does not contain them. `look` should read the plan's pages, or respect git's ignore rules |
| `ISO-37/4` | narrate-service | Low. Its compose publishes a fixed `127.0.0.1:8870`, so a second instance needed a scratch overlay with `!override`. Also, `narrate-service:local` on this host is older than `main`, and `up --build` would rebuild the tag another deployment may use |
| — | ISO-36/1 | Closed: the primed editor is now the tag that is built and recorded. ISO-36/2 (the marker sentence) did not recur this time, but the mechanism stands |

## For the register

- `main` was not moved, and no branch or worktree was touched. The corpus main checkout and its two
  containers were not touched. The narration service this row started is removed. Nothing was
  pushed.
- **To redeploy :8770:**
  1. Advance the main checkout to this tip.
  2. Recreate **both** the editor and the runner from the recorded tags, which are already built
     on this host, with the "Bring it up" command in `EXECUTION.md`.

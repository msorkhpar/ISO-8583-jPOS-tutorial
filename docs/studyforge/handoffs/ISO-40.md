# ISO-40 handoff: regenerated on `5f17031a`, with the scaffold joining the authored practices

**Office** `po-int`. **Branch** `int/iso-40-regenerate`, from `main` `d10f9cb` (ISO-39).
**Framework** `studyforge` 0.1.0, built from `5f17031a` (W490–W500 merged). **Toolchain**
`code-server-toolchain` at `181164f`, unchanged. **narrate-service** at `62725dd`.

| commit | what |
|---|---|
| `29bb331` | re-onboard, the adapter's practice join, ingest, the execution regenerate, 40 clips pruned, and the build |
| this commit | this handoff |

## What was done

1. **Install.** `studyforge` was cloned into scratch and checked out at `5f17031a`. A wheel was built
   from the clone and installed into a fresh venv with `--no-index --no-deps`. `PYTHONPATH` was
   unset. `library.commit()` reported `5f17031a6a52f4ef041849a8db4a619cca032468`.
2. **Re-onboard.** `reonboard('.', framework_commit='5f17031a…').write('.', regenerate=True)`.
   `hand_edited` read `[]` before and after. It regenerated `ingest/emit.py` with the scaffold's
   practice join (`studyforge.skills.adapter.practices.authored`). It also moved the
   `.studyforge/**` not-material entry within `corpus.json`, with the same glob and reason.
3. **W498/1: the scaffold's practice join was adopted.** `skills.exercises.practised` was not
   called per bundle.
   - The corpus's own adapter stops reading authored exercises. In `ingest/read.py`,
     `containers` no longer raises the counts, and `documents` no longer appends the authored
     documents.
   - `ingest/exercises.py` (ISO-21) is removed. `emit.py`'s join now reads every committed bundle
     and quiz, checks each gate record, numbers each after the source's own practice, and raises
     the unit's count.
   - **Why this, not `practised` per bundle.** The join keeps a practice that `read.documents`
     already returns, and it keeps that practice as-is, without `concepts`. So keeping ISO's own
     reader would have needed a second concepts path beside the framework's. One reader of
     bundles now serves this corpus and every scaffolded one.
   - The corpus's own tests follow the join:
     - `tests/ingest/test_exercises.py` catches the framework's `PracticeRefused`. The collision
       case now matches the framework's refusal, "already returns a different practice".
     - `tests/ingest/test_curriculum_region.py` reads containers and documents through
       `authored(...).joined(...)`, as `emit` does.
4. **Ingest.** `python3 -m ingest . 2026-09-21` exited 0, and its audit reported valid.
   - ⭐ All 42 authored practice documents changed.
   - The only change in each is a new `concepts` list, read from its unit's `coverage.json`.
5. **The execution regenerate** ran through the skill against a `git archive` of `181164f`.
   - ⭐ **No tag moved.** The editor is still `…/editor:java-maven-amd64-415f4b759fae`, and the
     runner is still `…/runner:java-maven-amd64-230dc78178c2`.
   - What is new comes from W490–W494: `compose.yaml` binds `.studyforge/execution/code` (the
     copy of the corpus's code that a lesson's code links open), `EXECUTION.md` lists it among the
     binds that must exist first, and `.studyforge/execution/code/.gitignore` is new.
6. **Narration.**
   - `narrate --prune` exited 0: **40 clips deleted, 40 record entries removed**. Every one is a
     `practice-prose*` speech unit, the withheld Lesson heading of W496/W498.
   - `narrate --voice am_liam` exited 0: **0 written, 1885 already synthesised**. It reported 0
     dead record entries and 0 superseded clips.
   - **No other speech unit changed.**
   - ⚠️ The GPU profile would not start (`ISO-40/2`). The run used narrate-service's CPU compose
     file, whose engine was already present at its pinned digest. The overlay set project
     `iso40-narrate`, `127.0.0.1:18871`, image `narrate-service:rel13-da8fa84`, `--pull never
     --no-build`. The service was then removed with `down -v`.
7. `build . --out .`.

## Gates

On the host, installed library `5f17031a`, in this worktree.

| gate | reading |
|---|---|
| `python3 -m studyforge.skills.onboarding.verify .` | GREEN, exit 0 |
| `python3 -m pytest tests/test_framework_pin.py` | GREEN, exit 0 |
| `python3 -m studyforge.cli validate .` | GREEN, exit 0, 0 findings with narration included |
| `python3 -m studyforge.cli plan .` | GREEN, exit 0 |
| `python3 -m studyforge.cli build . --out .` | GREEN, exit 0. After the commit, a second build left the tree clean |
| the corpus suite, before the commit | ⚠️ RED, exit 1, 38 of 39 passed. The one failure is `test_nothing_that_already_existed_changed_but_what_is_declared`, which names the two corpus test files edited in step 3. That is the known ISO-36/5 shape: a corpus's own edit reads as undeclared until it is committed |
| the corpus suite at `29bb331` | GREEN, exit 0, 39 passed |
| `hand_edited('.')` | `[]` |
| the compose `config --images` | GREEN, exit 0: `…-415f4b759fae` and `…-230dc78178c2`, both unchanged |

## The live reading, on this row's own serve

- **The setup.** A `git archive` of `29bb331` went into scratch, and the execution skill's
  `record_instance` gave that copy its own instance: project `iso40-live`, editor port 18443,
  containers `iso40-live-editor` and `iso40-live-runner`.
- It was brought up with EXECUTION.md's command and `--pull never`, and served with
  `studyforge serve . --site . --port 18772`. The pages were driven by a headless Chrome over CDP.
- Afterwards the serve was stopped. The compose project was brought down with `down -v`, and the
  copy was deleted.
- `docker events` shows every run landed in `iso40-live-runner`. The site's runner received
  nothing.

**The quiz (fundamentals unit 1): graded in the page, with no request.**
- The Check button shows.
- A first check with both answers wrong read **"0 of 2 answered correctly."** Each option's
  "Not this one." sentence was shown.
- A second check with both answers right read **"Every question answered correctly."** It showed
  "Right. Section "ISO-8583:2003" lists support…" and "Right. It is the second of the four roles in
  section "Role in electronic financial transactions"." The quotes render as quotes.
- **The serve's request log gained 0 lines** across both checks.

**The Practice list and the workspace (fundamentals units 2–4):**

| unit | Practice list (card titles) | cards with a concepts list | the workspace |
|---|---|---|---|
| 2 | "Practice: Building the primary bitmap", "Read which data elements a bitmap says are present" | the authored one, 2 concepts | opens full-screen (1400×913 of 1400×913), titled after the practice. The editor is shown, and the run/test controls are shown |
| 3 | "Practice: Unpacking data elements", "Write a field value the way its format says" | the authored one, 3 concepts | the same |
| 4 | "Practice: Reading the MTI", "Decide what a response says about its request", "Build the reversal of an earlier transaction" | both authored ones, 2 and 1 concepts | the same |

**The three source practices still run in this row's runner.**

| practice | Run | Test |
|---|---|---|
| unit 2 `practice/bitmap/Bitmap.java` | "Finished.", exit 0 | "Finished with errors.", 4 checks fail. This is as the corpus ships it: its starting file is wrong by one bit, and W476's handoff describes it that way |
| unit 3 `practice/fields/Fields.java` | "Finished.", exit 0, printing the unpacked elements | none: it declares only a run command, and the page shows no Test button |
| unit 4 `practice/mti/Mti.java` | "Finished.", exit 0 | "Passed.", every check passed, as it ships finished |

## Findings

| id | what |
|---|---|
| `ISO-40/1` | **Medium, toolchain.** The editor panel in the workspace shows a side bar titled **"Build with Agent"**, reading "AI responses may be inaccurate", with "Generate Agent Instructions" and a "Describe what to build" chat box. It is in image `…/editor:java-maven-amd64-415f4b759fae`, which is the one the site runs. This contradicts the 2026-09-22 removal of the bundled chat. Seen in the unit 4 workspace screenshot of the live reading |
| `ISO-40/2` | Low, host. narrate-service's GPU profile fails to start: `failed to fulfil mount request: open /run/nvidia-persistenced/socket`. `/run/nvidia-persistenced` does not exist on the host now, although the same profile ran in ISO-37–39. The CPU profile serves as a stand-in, and it yields the same clips' conditions. That did not matter here, since nothing was synthesised |
| `ISO-40/3` | Low, framework. The generated non-destructive test reads the corpus's own edited tests as undeclared until they are committed. This is ISO-36/5 again, and now also for `tests/ingest/**` |
| — | ISO-37/2, ISO-37/4 and ISO-36/2 still stand |

## For the register

- `main` was not moved, and no branch or worktree was touched. The corpus main checkout, :8770 and
  its two containers were not touched. The narration service and the live stack this row started
  are removed. Nothing was pushed.
- At the register's direction, this row deleted only its own pytest run directories.
- **To redeploy :8770:** advance the main checkout to this tip, and restart the serve on the new
  library (W500 removed the server's quiz route). Neither image tag moved, so the containers need
  no recreate.

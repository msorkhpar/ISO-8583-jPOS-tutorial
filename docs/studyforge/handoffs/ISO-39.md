# ISO-39 handoff: heading numbers cited in prose become named links

**Office** `po-int`. **Branch** `int/iso-39-regenerate`, from `main` `c815de8` (ISO-38).
**Framework** `studyforge` 0.1.0, built from `a33243c3` (W488 and W489 merged). **Toolchain**
`code-server-toolchain` at `181164f`, unchanged. **narrate-service** at `62725dd`.

| commit | what |
|---|---|
| `f218b17` | re-onboard, the execution regenerate, 4 clips narrated and 4 pruned, and the build |
| this commit | this handoff |

⭐ **Every gate is GREEN.** The corpus suite was held up twice by the host's `/tmp` quota
(`ISO-39/1`), and both readings were taken once the register had freed space.
⚠️ **The regeneration was first committed as `673477d`.** To read the suite on an uncommitted tree,
that commit was undone with `git reset --soft`, and the same content was committed again as
`f218b17`. `673477d` is on no branch.

## What was done

Exactly as in ISO-38.

1. **Install.** `studyforge` was cloned into scratch and checked out at `a33243c3`. A wheel was
   built from the clone and installed into a fresh venv with `--no-index --no-deps`. `PYTHONPATH`
   was unset. `library.commit()` reported `a33243c3f1d05e125c40667ef56e7c891f86bea9`.
2. **Re-onboard.** `reonboard('.', framework_commit='a33243c3…').write('.', regenerate=True)`.
   `hand_edited` read `[]` before and after.
3. **The execution regenerate** ran through the skill against a `git archive` of `181164f`. ⭐ No
   tag moved: the editor is still `…/editor:java-maven-amd64-415f4b759fae`, and the runner is still
   `…/runner:java-maven-amd64-230dc78178c2`. Every execution file rewrote byte-identical.
4. **Ingest.** `python3 -m ingest . 2026-09-21` changed no archive byte. Its audit reported exactly
   the four expected `narration-stale` units.
5. **Narration.**
   - The service ran as in ISO-37 and ISO-38: project `iso39-narrate` on `127.0.0.1:18871`, image
     `narrate-service:rel13-da8fa84`, and the GPU overlay.
   - It started with `--pull never --no-build`, and was removed with `down -v`. No container or
     volume of it remains.
   - `narrate --voice am_liam` exited 0: **4 written, 1921 already synthesised**.
   - `narrate --prune` exited 0: **4 clips deleted**, 0 record entries removed.
6. `build . --out .`.

## Actual diff against the expectation

⭐ **It matches: 4 synthesised, 4 pruned, no tag moved.**

- **The four speech units re-narrated:**
  - `iso-fundamentals--unit-02.practice-prose.b2`
  - `iso-fundamentals--unit-03.practice-prose.b2`
  - `iso-fundamentals--unit-04.practice-prose.b2`
  - `jpos-client--unit-01.prose.b5`
- **Their pages:** each sentence now names the heading in double quotes, as an in-page link. For
  example, client unit 1 reads: *section "<a href="#prose-b14">Creating the Main Application</a>"*.
- **Changed besides:** `.studyforge/narration.json`, and the pin, stubs, `installed.json` and
  `ONBOARDING.md` commit sentence.
- **The unit 1 quiz:** `quiz.json` is unchanged in the corpus. The rewrite happens when it is
  served. Read in-process through `CorpusContent`, the two explanations are served as:
  - *Section "ISO-8583:2003" lists support for new transaction types…*
  - *It is the second of the four roles in section "Role in electronic financial transactions".*

  They are never spoken.

## Gates

On the host, installed library `a33243c3`, in this worktree.

| gate | reading |
|---|---|
| `python3 -m studyforge.skills.onboarding.verify .` | GREEN, exit 0 |
| `python3 -m pytest tests/test_framework_pin.py` | GREEN, exit 0 |
| `python3 -m studyforge.cli validate .` | GREEN, exit 0, 0 findings with narration included; **0 `link-unresolved`**. It was 4 `narration-stale` before the narration |
| `python3 -m studyforge.cli plan .` | GREEN, exit 0 |
| `python3 -m studyforge.cli build . --out .` | GREEN, exit 0. After the commit, a second build left the tree clean |
| the corpus suite, before the commit (read on the staged tree) | GREEN, exit 0, 39 passed |
| the corpus suite at `f218b17` | GREEN, exit 0, 39 passed. It was read after pytest's three stale run directories from this corpus were deleted, at the register's direction |
| `hand_edited('.')` | `[]` |
| the compose `config --images` | GREEN, exit 0: `…-415f4b759fae` and `…-230dc78178c2` |
| `python3 -m studyforge.look . --out <scratch> --all` | GREEN, exit 0, 42 of 42 pages, with nothing from `.scratch/` |

**Fundamentals unit 2.**
- The screenshot shows the top of the page, with the narration bar present.
- The practice's sentence sits below the fold. Its DOM reads: *…says which fields it carries.
  Section "<a href="#prose-b11">Bitmaps</a>" describes it as…*

## Findings

| id | what |
|---|---|
| `ISO-39/1` | **Host, now resolved.** The per-user disk quota on the `/tmp` tmpfs ran out twice. Each run of the corpus suite leaves a copy of the corpus in `/tmp/pytest-of-<user>`, and each copy holds about 200 to 570 MB because of the clips. The failing run filled the quota: a write probe found 0 MB free. The register freed the space. At its direction, only `pytest-32`, `pytest-33` and `pytest-34` were deleted: this corpus's own runs, identified by `tests/ingest/test_exercises.py`'s test names. A 1000 MB write probe then succeeded. ⚠️ **Worth a framework row:** the generated suite's corpus copy is not cleaned up, and pytest keeps the last three runs, so three runs of this corpus take up to about 1.5 GB of the quota |
| — | ISO-37/2, ISO-37/4 and ISO-36/2 still stand |

## For the register

- `main` was not moved, and no branch or worktree was touched. The corpus main checkout and its two
  containers were not touched. The narration service this row started is removed. Nothing was
  pushed.
- To redeploy :8770, advance the main checkout. Neither tag moved, so no container needs
  recreating.

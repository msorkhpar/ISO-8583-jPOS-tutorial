# ISO-38 handoff: W487's fix, carried in

**Office** `po-int`. **Branch** `int/iso-38-regenerate`, from `main` `85c00f2` (ISO-37).
**Framework** `studyforge` 0.1.0, built from `ff597ec9` (W487 merged). **Toolchain**
`code-server-toolchain` at `181164f`, unchanged. **narrate-service** at `62725dd`.

| commit | what |
|---|---|
| `64370f9` | re-onboard, the execution regenerate, 8 clips narrated and 4 pruned, and the build |
| this commit | this handoff |

## What was done

1. **Install.** This is the method from ISO-36 and ISO-37. `studyforge` was cloned into scratch
   and checked out at `ff597ec9`. A wheel was built from the clone with
   `pip wheel --no-build-isolation --no-index`, and installed with `--no-index --no-deps` into a
   fresh venv. `PYTHONPATH` was unset. `library.commit()` reported
   `ff597ec929548eba317f127ce27063d05fa98ae1`.
2. **Re-onboard.** `reonboard('.', framework_commit='ff597ec9…').write('.', regenerate=True)`.
   `hand_edited` read `[]` before and after. What moved: the pin, the three stubs, `installed.json`
   and the commit sentence in `ONBOARDING.md`.
3. **The execution regenerate** ran through the skill against a `git archive` of `181164f`.
   ⭐ **No tag moved.** The editor is still `…/editor:java-maven-amd64-415f4b759fae`, and the
   runner is still `…/runner:java-maven-amd64-230dc78178c2`. Every execution file rewrote
   byte-identical, and no image was built.
4. **Ingest.** `python3 -m ingest . 2026-09-21` changed no archive byte. Its audit reported the
   two expected `narration-stale` findings, for the `prose.b3` clips of client unit 8 and server
   unit 8.
5. **Narration.**
   - The service ran as in ISO-37: narrate-service's compose files with the GPU overlay, plus a
     scratch overlay. The overlay set project `iso38-narrate`, `127.0.0.1:18871` and the existing
     image `narrate-service:rel13-da8fa84`.
   - It started with `--pull never --no-build`, and was removed with `down -v`. No container or
     volume of it remains.
   - `studyforge narrate . --voice am_liam` exited 0: **8 written, 1917 already synthesised**.
   - `studyforge narrate . --prune` exited 0: **4 clips deleted, 2 record entries removed**.
   - The 1925 clips on disk are the ones the pages name.
6. `build . --out .`.

## Actual diff against W487's expectation

⭐ **It matches: 8 synthesised, 4 pruned, no tag moved.**

- **Client and server unit 8:** each `prose.b3` clip moves from `…-d7ea7df1` to `…-c4a93369`, and
  the heading reads "jPOS Logging Framework".
- **Fundamentals unit 7:**
  - the two outline-only paragraphs (`prose.b10`, "7.2.1. Batch processing 7.2.2. …", and
    `prose.b16`, "7.3.1. Chargeback initiation …") are each now a three-item list;
  - that makes six new `.i1`–`.i3` clips, and the two old paragraph clips are deleted.
- **Changed besides:** `.studyforge/narration.json` and those three pages.
- **Unchanged:** the archive, `corpus.json`, the execution files, `page.css`, `page.js`, the index
  and every other page.

## Gates

On the host, installed library `ff597ec9`, in this worktree.

| gate | reading |
|---|---|
| `python3 -m studyforge.skills.onboarding.verify .` | GREEN, exit 0 |
| `python3 -m pytest tests/test_framework_pin.py` | GREEN, exit 0 |
| `python3 -m studyforge.cli validate .` | GREEN, exit 0, 0 findings with narration included. It was 2 `narration-stale` before the narration |
| `python3 -m studyforge.cli plan .` | GREEN, exit 0 |
| `python3 -m studyforge.cli build . --out .` | GREEN, exit 0. After the commit, a second build left the tree clean |
| the corpus suite, before the commit | GREEN, exit 0, 39 passed |
| the corpus suite at `64370f9` | GREEN, exit 0, 39 passed |
| `hand_edited('.')` | `[]` |
| the compose `config --images` (runner, editor and instance env files; neither image variable set) | GREEN, exit 0. It named `…/editor:java-maven-amd64-415f4b759fae` and `…/runner:java-maven-amd64-230dc78178c2` |
| `python3 -m studyforge.look . --out <scratch> --all` | GREEN, exit 0. It rendered 42 of 42 pages, with nothing from `.scratch/` (ISO-37/3 is fixed) |

**The screenshots.**
- Client unit 8 and server unit 8: the first section heading reads "jPOS Logging Framework", in the
  page and in the "On this page" outline, with no "8.1". The "Play narration" bar is present.
- Across all built pages, no prose heading begins with an outline number.

## Findings

| id | what |
|---|---|
| — | Nothing new. ISO-37/1 and ISO-37/3 are closed by W487. ISO-37/2, ISO-37/4 and ISO-36/2 still stand |

## For the register

- `main` was not moved, and no branch or worktree was touched. The corpus main checkout and its two
  containers were not touched. The narration service this row started is removed. Nothing was
  pushed.
- **To redeploy :8770:** advance the main checkout to this tip. Neither image tag moved since
  ISO-37. So if the site already runs `…-415f4b759fae` and `…-230dc78178c2`, no container needs
  recreating. If it does not, recreate both with "Bring it up" in `EXECUTION.md`.

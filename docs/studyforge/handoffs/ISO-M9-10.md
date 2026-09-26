# ISO-M9-10 hand-off: narration speaks lesson prose only

**Office** `po-int`. **Branch** `main`, from `4c83508` (ISO-44). The tip is the commit that adds
this file. Nothing was pushed, and no remote was touched.

**Outcome:** the corpus is re-pinned to `f1b55bc7` and follows the register ruling that narration
speaks lesson prose only (W507). 734 practice and fence-caption clips are retired, no prose clip
was owed, and every gate is GREEN. **There is no pack:** `--pack` refuses this corpus by name,
because it commits its clips (see ISO-M9-10/1).

## Where and with what

- **Library:** studyforge 0.1.0, built from `f1b55bc792d9aa24ec94a5d6c5795e84a6194453`. The wheel
  came from a scratch clone of the framework's `main` at that commit and was installed into a fresh
  scratch venv, following the README (its pinned `pytest` set included).
  `python3 -m studyforge.skills.onboarding.verify` reports that commit.
- **narrate-service** at `62725dd`, from a scratch clone, as below.
- **Left alone:** the site on :8770, the editor on 8443, the runner and every `studyforge-*iso-8583*`
  container. None was stopped, restarted or changed. The site serves this checkout, so it now
  serves the rebuilt pages.

## Commits (all `po-int <po-int@example.invalid>`)

| commit | what |
|---|---|
| `8d8b887` | `reonboard(framework_commit=f1b55bc7…)`, then step 5b `stage_site` |
| `05d5826` | the narration record after `--prune`, the 734 retired clips removed from the tree, and the build |
| this commit | this hand-off |

## 1. Re-pin

- `reonboard('.', framework_commit=…).write('.', regenerate=True)` moved the pin, the three skill
  digests and `docs/archive/ONBOARDING.md` together. No manifest answer moved, so the execution
  skill's `write` was not refused and was not re-run.
- **Step 5b** ("re-run 5b when the library or the pin moves"): `stage_site` gave the site tag
  `studyforge-site:0.1.0-a73499c28f4b` (it was `…-0d0cfd0720c3`). The image was built on the
  default engine under the gate lock with `docker build --pull=false`.
- `hand_edited` read one sentence **before** the re-pin: `.studyforge/execution/site/.gitignore`
  was missing. Step 5b wrote it back, and `hand_edited` then read `[]` (ISO-M9-10/2).

## 2. The count, before narrating

`studyforge narrate` has no count-only mode, so the count used the library's own walk (the one
`narrate` and `--prune` share: `survey`, `dead_entries`, and the record compared with each unit's
wanted clip name), with no service and no write.

| what | count |
|---|---|
| record entries | 1,885 |
| speech units the pages speak now | 1,151 |
| clips no page speaks any more | **734**: 516 practice units, 218 fence captions |
| prose units that need a new clip | **0**: no list item or paragraph moved its words in this corpus |

## 3. Narration

- **Service:** `narrate-service` at `62725dd`, built from its own `compose.yaml` by a scratch
  override that set project `m9-10-narrate`, port `127.0.0.1:8891` and image
  `narrate-service:m9-10`. The engine `ghcr.io/remsky/kokoro-fastapi-cpu:v0.8.2@sha256:8545e2a4…c59a`
  was already on disk, and the project ran with `up --pull never`. `/healthz` read
  `provides 3`, `chunk_chars 1800`, `engine_profile cpu`: the conditions the record holds.
- `studyforge narrate . --voice am_liam --service http://127.0.0.1:8891`, **exit 0**:
  `0 clip(s) written, 1151 already synthesised`, `dead record 734`, `superseded clips 0`.
- The service was then taken down with `down -v`, and the image was removed. No `m9-10`
  container, volume or network remains.
- `studyforge narrate . --prune`, **exit 0**: `734 clip(s) deleted, 734 record entries removed, 0 held`.
- **Written 0, retired 734, held 0.** The record and the tree now hold 1,151 clips.

## 4. Gates

| gate | result |
|---|---|
| `studyforge validate .` | **GREEN, exit 0**: `valid: 0 finding(s), 0 unchecked claim(s)` |
| `studyforge plan .` | **GREEN, exit 0**: 0 to create, 43 to replace, 1,191 to keep, 152 claimed, 0 superseded clips, 0 refusals |
| `studyforge build . --out .`, first run | **GREEN, exit 0**: the 38 unit pages re-link, committed in `05d5826` |
| `studyforge build . --out .`, run twice more | **GREEN, exit 0 both times**, the tree clean after each |
| `hand_edited('.')` | **GREEN**: `[]` |
| generated tests, `python3 -m pytest tests` | **GREEN, exit 0**: 39 passed in 34 s, `test_audit` and `test_non_destructive` included |
| page references | **GREEN**: the 41 pages carry 1,151 `data-audio` references, all resolve to a clip, and none sits in a practice panel, a code figure, a `pre` or a `code` |
| personal data | **GREEN**: no committed file of this step holds a home path, host name or email |

## 5. Pack

- **Refused by name, exit 1:**
  `studyforge narrate . --pack <volumes-dir> --tag narration-1.0.0`
  answered: *"this corpus's corpus.json commits its clips (media.commit 'auto'), so every checkout
  already carries them; a release is for a corpus that declares media.commit 'never'"*.
- **The tag:** no pack was ever made for this corpus: no `.studyforge/narration-release/` exists
  on any branch. So `narration-1.0.0` was never packed or uploaded, and it was the tag asked for.
- **No volumes, no `--publish` dry run and no upload command.** The empty folder made for the
  volumes was removed.

## Findings

| id | concerns | finding |
|---|---|---|
| ISO-M9-10/1 | this corpus, and the brief | **This corpus cannot be packed as it is declared.** It commits its clips (`media.commit` is `auto` by default), so `--pack` refuses it. A release needs `media.commit: "never"`, which is a manifest answer (this office did not check which re-onboarding route changes it), and it would take the clips out of git. That is a register decision. Until then a clone carries every clip, and the retirement shows as 734 deleted files in `05d5826`. |
| ISO-M9-10/2 | execution skill, generated ignore file | **`site/.gitignore` is ignored by `.studyforge/.gitignore`**, so it is not in git. A checkout that did not run step 5b itself lacks it, and `hand_edited` names it missing until 5b runs. The Java course tracks the same file. |
| ISO-M9-10/3 | `studyforge narrate` (carried: M9-7-INT F2, M9-8 F4) | **There is still no count-only mode.** The count before narrating needed the library's own walk from a script. |

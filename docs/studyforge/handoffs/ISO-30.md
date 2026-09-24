# ISO-30 handoff: re-narrate what the fix rounds changed

**Office** `po-int`. **Branch** `int/m10-iso-23`, from `0799d0a`. **Framework** the corpus pin
`39f12d4e`. **Narration service** `../narrate-service` at `ce975b6`, CPU profile, with its
existing images `narrate-service:local` and the pinned `kokoro-fastapi-cpu` digest (no build,
no pull). It ran under its own compose project, `iso30-narrate`, on `127.0.0.1:8870`, and was
taken down afterwards with its volume removed.

**Why.** ISO-27, ISO-28 and ISO-29 changed prose on 13 pages and renamed 6 headings, but they
re-synthesised no narration. `narrate.playable` joins a clip on the speech id, so a changed
paragraph kept **playing its old clip**, recorded only as `stale` (`WORDS_MOVED`). Nothing gated
it.

## Measured with the framework's own join

The measurement:

- for every unit the build reads, `build_unit` (the page pass's document);
- then `playable_of(document, read_state(state_file(root)), audio=audio_dir(root, unit_location(…)))`,
  the same call `skills/onboarding/standing.py` makes.

### Before (at `0799d0a`) and after (at `b3a6c2b`)

| container | lesson: stale | practice: silent | lesson: silent | stale after | silent after |
|---|---|---|---|---|---|
| `iso-fundamentals` | 32 | 255 | 1 | 0 | 0 |
| `jpos-client` | 24 | 162 | 0 | 0 | 0 |
| `jpos-server` | 17 | 139 | 0 | 0 | 0 |
| **total** | **73** | **556** | **1** | **0** | **0** |

| | before | after |
|---|---|---|
| clips the pages play | 1364 | 1921 |
| stale (plays a clip made from other words) | 73 | **0** |
| silent (no clip) | 557 | **0** |

- **The 73 stale units** are the lesson paragraphs, list items and headings the fix rounds
  changed.
- **556 of the 557 silent units are in the practice documents** that ISO-23 added (the
  exercises' statements and quizzes), which had never been narrated. **One silent unit is
  lesson prose**: `iso-fundamentals` unit 8 `prose.b37`, the paragraph ISO-29 added to
  `src/8.md`.
- Reaching 0 and 0 therefore meant synthesising the silent units as well as the stale ones.

## What ran

1. `studyforge narrate . --voice am_liam` (format mp3) at the pin. The voice and format are
   the ones the record's conditions name (`am_liam`, mp3, kokoro, provides 3, chunk 1800).
   - **630 clips written, 1291 already synthesised under these conditions. Exit 0.**
   - It reported 73 superseded clips and 0 dead record entries.
2. `studyforge narrate . --prune`: **73 superseded clips deleted, 0 record entries removed, 0
   held. Exit 0.**
3. `studyforge build . --out .`: exit 0. **36 unit pages** now reference their new clips; two
   pages have nothing that changed.

The commit holds 630 clips added, 73 removed, the 36 pages and `.studyforge/narration.json`.

## Mechanical listen-check

The paragraph checked is `jpos-client` unit 3 `prose.b2`, the page intro that ISO-28 rewrote
("…two main components: a client channel and SSL/TLS on that channel").

- **The digest matches:** `digest_of` of the unit's current spoken text is `bbca29f1`, and the
  record and the page name `jpos-client--unit-03.prose.b2-bbca29f1.mp3`.
- **The page changed:** at `0799d0a` it played `…prose.b2-4783e410.mp3`, the stale clip.
- **The file is valid:** "MPEG ADTS, layer III, v2, 128 kbps, 24 kHz, Monaural", with an ID3
  2.4 tag. A frame walk counts 668 frames, **16.0 s**, a plausible length for its roughly 45
  words.

## Gates, at `b3a6c2b`

- The join, re-run after the rebuild: **0 stale, 0 silent**, 1921 clips across 38 units.
- `studyforge validate .`: **GREEN, exit 0.**
- The corpus suite: **GREEN, exit 0.**

## For the register

- The practice documents are now narrated too. Any later re-authoring of a statement or quiz
  makes its units stale in the same way. A narration gate after each authoring or fix round
  would catch it: the join above run as a check, failing when stale or silent is not 0.
- `src/study/audio` is now 166 MB in the tree.
- The main checkout, `workspace.json`, `:8770` and the user's containers were not touched.

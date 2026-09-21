"""The practice series: a fourth container this repository's curriculum record does not carry.

**What it does.** Reads `src/Practice.md` — the practice series' own record and
introduction — and returns the container it declares plus the two documents each
of its units holds: the lesson that explains the shape, and the practice that
sets the work.

**How you use it.** `container(root)`, `documents(root, container)` and
`expected_units(root)`. `ingest.read` calls all three; nothing else does.

**Depends on.** `studyforge.corpus.container` for the `Container` and `Unit`
shapes, `studyforge.archive.markdown` for the block vocabulary, and
`ingest.read` for the two regular expressions the record shares with `README.md`.

## ⛔ Why this is a container of its own and not units added to an existing one

⚠️ **R3: no existing file is moved, renamed or rewritten.** The curriculum this
corpus records lives in `README.md`, and every unit of the three recorded series
is a whole file whose heading count `studyforge validate` compares against the
archive. ⛔ **Adding a practice document to one of those units would raise that
unit's heading total above the file it was read from**, and `validate` would
report a short read — correctly, because the practice's prose is not in that
file. ⭐ So the practices bring their own material, in their own files, under
their own record, and nothing that already existed is touched.

## ⭐ Each unit is ONE file, split at its first level-2 heading

The lesson is everything above the first `##`; the practice is that heading and
everything below it. ⛔ **The split is why the heading counts still add up**:
the two documents together hold exactly the headings the source file carries, so
`check_completeness` compares the same number on both sides. ⚠️ A heading
invented here — a "Starting code" the file does not carry — would break that,
which is why the record's own `## Starting code` heading is the last thing in
each file and this module appends only a code **block** beneath it.

## ⛔ The starting code is READ FROM THE JAVA FILE, never retyped

⭐ The file the reader edits is the one truth about what the reader starts from.
A fenced copy in the Markdown would be a second one, and two copies of a thing
nobody diffs is how a practice comes to show code its grader never sees.
"""

from __future__ import annotations

from pathlib import Path

from studyforge.address import Address
from studyforge.archive.markdown import parse as parse_markdown
from studyforge.corpus.container import Container, Unit

from ingest.read import ENTRY, CurriculumChanged, VARIANT, _titled

#: The record this container is read from: the practice series' introduction,
#: and the list of its entries in reading order. ⛔ Excluded from ingestion by
#: `corpus.json` for the same reason the three aggregates are — its `origin`
#: places the container's page and no unit reads it.
RECORD = "src/Practice.md"

#: The title the record opens with, and the address it is served at. ⛔ Recorded
#: here and derived from nothing (§6) — no title is slugified in this package.
TITLE = "Practices"
ADDRESS = "iso-practice"

#: The kinds this container's units hold, one document each.
LESSON = "lesson"
PRACTICE = "practice"

#: Where the practice half of a unit begins. ⭐ A level, not a wording: the
#: files are free to name their sections whatever suits them, and a renamed
#: heading must not silently move the split.
PRACTICE_LEVEL = 2

#: The language every code block this module appends is fenced as.
LANGUAGE = "java"

#: ⛔ **The workspace each unit sets, recorded and derived from nothing.** One
#: entry per unit of `RECORD`, in `studyforge.exercise`'s own key order.
#:
#: ⭐ **Three shapes, on purpose** (the runnable fixture's discipline): unit 1
#: ships finished and its grader passes, unit 2 ships wrong by one bit position
#: and its grader fails until the reader fixes it, and unit 3 names a file and
#: no grader at all — §7's ungraded state, which is not a shortfall.
#:
#: ⛔ **Plain `java`, and nothing else.** Every command is the JDK's own
#: source-file launcher, which compiles in memory and writes no class file
#: anywhere in this repository (R3). There is no build tool, no jar and nothing
#: to fetch — jPOS is a dependency, and a dependency is vendored or pinned by
#: digest, never downloaded loose. The ISO-8583 *format* is what this material
#: teaches, and reading an MTI or building a bitmap needs no library at all.
#:
#: ⚠️ **`bundled` / `authoritative`**: these graders ship in this repository,
#: beside the material, and are this corpus's own. R5 forbids a `generated`
#: grader that claim, which is a rule an authoring skill will meet and this
#: hand-authored sample does not.
WORKSPACES: dict[int, dict] = {
    1: {
        "main_path": "practice/mti/Mti.java",
        "test_path": "practice/mti/MtiTest.java",
        "run_command": ["java", "practice/mti/Mti.java"],
        "test_command": ["java", "practice/mti/MtiTest.java"],
        "provenance": "bundled",
        "trust": "authoritative",
    },
    2: {
        "main_path": "practice/bitmap/Bitmap.java",
        "test_path": "practice/bitmap/BitmapTest.java",
        "run_command": ["java", "practice/bitmap/Bitmap.java"],
        "test_command": ["java", "practice/bitmap/BitmapTest.java"],
        "provenance": "bundled",
        "trust": "authoritative",
    },
    3: {
        "main_path": "practice/fields/Fields.java",
        "run_command": ["java", "practice/fields/Fields.java"],
    },
}


def _record(root: Path) -> list[str]:
    """Return the record's lines, or refuse because there is no record."""
    path = Path(root) / RECORD
    if not path.is_file():
        raise CurriculumChanged(
            f"{RECORD} records the practice series — its entries, their ordinals, "
            f"their titles and the file each is served from — and it is not there. "
            f"Restore it, or remove this container from the adapter."
        )
    return path.read_text(encoding="utf-8").splitlines()


def _entries(root: Path) -> list[tuple[int, str, str]]:
    """Return `(ordinal, title, href)` per entry, in the order the record writes them."""
    found: list[tuple[int, str, str]] = []
    for number, line in enumerate(_record(root), start=1):
        entry = ENTRY.match(line)
        if entry is None:
            continue
        ordinal = int(entry.group("n"))
        if ordinal != len(found) + 1:
            raise CurriculumChanged(
                f"{RECORD} line {number} numbers its entries out of sequence: entry "
                f"{len(found) + 1} is recorded as {ordinal}. The ordinal is the reading "
                f"order, so a gap or a repeat is a decision about the order."
            )
        found.append((ordinal, _titled(entry.group("title")), entry.group("href")))
    if not found:
        raise CurriculumChanged(
            f"{RECORD} records no entry, so this container would reach the contents "
            f"page as an empty row. Record its practices, or remove the record."
        )
    return found


def container(root: Path) -> Container:
    """Return the practice series as one `Container`, with one unit per entry."""
    root = Path(root)
    units = []
    for ordinal, title, href in _entries(root):
        if not (root / href).is_file():
            raise CurriculumChanged(
                f"{RECORD} records an entry whose material is not in this repository. "
                f"A broken record link is reported, never skipped (R6): restore the "
                f"file, or remove the entry."
            )
        if ordinal not in WORKSPACES:
            raise CurriculumChanged(
                f"{RECORD} records entry {ordinal} and this adapter holds no workspace "
                f"for it. A practice that sets no work is a reading page with a Run "
                f"button that does nothing: record the file it sets in "
                f"ingest.practices.WORKSPACES, or record the entry elsewhere."
            )
        units.append(Unit(n=ordinal, title=title, practices=1, origin=href))
    return Container(
        address=Address.of(ADDRESS),
        titles=(TITLE,),
        variant=VARIANT,
        # ⛔ Replaced by `emit` with the run's own date, exactly as `read` does.
        ingested="1970-01-01",
        units=tuple(units),
        origin=RECORD,
    )


def _split(blocks: list[dict]) -> tuple[list[dict], list[dict]]:
    """Return `(lesson blocks, practice blocks)`, cut at the first level-2 heading."""
    for at, block in enumerate(blocks):
        if block.get("type") == "heading" and block.get("level", 1) >= PRACTICE_LEVEL:
            return blocks[:at], blocks[at:]
    raise CurriculumChanged(
        f"a unit of {RECORD} carries no level-{PRACTICE_LEVEL} heading, so there is "
        f"nothing to split its lesson from its practice. Every practice file opens "
        f"with its lesson and turns to the work at a '##' heading."
    )


def _starting_code(root: Path, workspace: dict) -> str:
    """Return the file the reader starts from, read from the file itself."""
    return (Path(root) / workspace["main_path"]).read_text(encoding="utf-8")


def documents(root: Path, held: Container) -> list[dict]:
    """Return the fields for this container's documents: a lesson and a practice per unit."""
    root = Path(root)
    fields = []
    for unit in held.units:
        workspace = WORKSPACES[unit.n]
        lesson, practice = _split(parse_markdown((root / unit.origin).read_text(encoding="utf-8")))
        code = _starting_code(root, workspace)
        common = {"address": held.address, "variant": held.variant, "unit": unit.n}
        fields.append({**common, "kind": LESSON, "ordinal": 1, "title": unit.title,
                       "blocks": lesson})
        fields.append({**common, "kind": PRACTICE, "ordinal": 1, "title": unit.title,
                       "blocks": [*practice, {"type": "code", "lang": LANGUAGE, "text": code}],
                       "starting_code": code,
                       "exercise": dict(workspace)})
    return fields


def expected_units(root: Path) -> int:
    """Return how many units this container's own record declares.

    ⛔ Counted from the record rather than from `container`, for the reason
    `ingest.read.expected_units` states: a reading compared with itself agrees
    by construction and catches nothing.
    """
    return sum(1 for line in _record(Path(root)) if ENTRY.match(line) is not None)

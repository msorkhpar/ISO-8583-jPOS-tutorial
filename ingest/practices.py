"""The practices: additive material that joins the unit it practises.

**What it does.** Reads `src/Practice.md` — the record of which unit each
practice belongs to — and returns, per container, the practice each of its
units carries: the file it was read from and the document built out of it.

**How you use it.** `attachments(root)`, `document(...)` and `expected(root)`.
`ingest.read` calls all three; nothing else does.

**Depends on.** `studyforge.archive.markdown` for the block vocabulary, and
`ingest.read` for the two regular expressions the record shares with
`README.md`.

## ⭐ A practice is part of its TOPIC page, not a section after the chapter

⛔ **Register ruling (2026-09-21):** a practice is part of its topic page, never
a separate section after the whole chapter. A reader who finishes
a chapter and then navigates to a Practices section has left the material
behind. ⚠️ **This corpus shipped exactly that** — a fourth container — and
`W428` is the row that undid it.

## ⛔ Why it could not be done before, and what changed

⚠️ **R3: no existing file is moved, renamed or rewritten**, and
`corpus.manifest.edits` refuses an edit to any file this corpus's own `content`
policy includes — which is every prose unit file here. ⛔ So the practice's
prose could not be appended to `src/4.md`, and
`validate.source.completeness` compared a unit's whole heading total against
that one file, so attaching a practice document read as a short read.

⭐ **`container_api: 3` settles it.** A unit declares `practice_origin` — a
**second, additive** file — and the check accounts per origin: the lesson
against `origin`, the practice against `practice_origin`. Nothing existing is
touched and nothing is declared in `permitted_edits`.

## ⭐ The record says WHICH unit, and it is a record rather than a derivation

⛔ Nothing here infers the attachment from a title, a filename or a number in
the prose (§6). `src/Practice.md` groups its entries under the container's own
title, exactly as `README.md` does, and the ordinal of an entry is **the unit
of that container the practice joins**.

## ⛔ The starting code is READ FROM THE JAVA FILE, never retyped

⭐ The file the reader edits is the one truth about what the reader starts
from. A fenced copy in the Markdown would be a second one, and two copies of a
thing nobody diffs is how a practice comes to show code its grader never sees.
⚠️ Only a code **block** is appended, never a heading: a heading invented here
would break `check_completeness`, which is the point of it.
"""

from __future__ import annotations

from pathlib import Path

from studyforge.archive.markdown import parse as parse_markdown

from ingest.read import CONTAINERS, ENTRY, LABEL, CurriculumChanged

#: The record this module reads: which practice joins which unit, in reading
#: order. ⛔ Excluded from ingestion by `corpus.json` — no unit's `origin` or
#: `practice_origin` names it, because it is a record and not material.
RECORD = "src/Practice.md"

#: The archive's word for a document that sets work.
PRACTICE = "practice"

#: The language every code block this module appends is fenced as.
LANGUAGE = "java"

#: ⛔ **The workspace each practice sets, recorded and derived from nothing** —
#: keyed by the practice's own source file, which is the one identifier that
#: does not move when a practice is re-attached to a different unit.
#:
#: ⭐ **Three shapes, on purpose** (the runnable fixture's discipline): the
#: first ships finished and its grader passes, the second ships wrong by one
#: bit position and its grader fails until the reader fixes it, and the third
#: names a file and no grader at all — §7's ungraded state, which is not a
#: shortfall.
#:
#: ⛔ **Plain `java`, and nothing else.** Every command is the JDK's own
#: source-file launcher, which compiles in memory and writes no class file
#: anywhere in this repository (R3). There is no build tool, no jar and nothing
#: to fetch — jPOS is a dependency, and a dependency is vendored or pinned by
#: digest, never downloaded loose.
#:
#: ⚠️ **`bundled` / `authoritative`**: these graders ship in this repository,
#: beside the material, and are this corpus's own. R5 forbids a `generated`
#: grader that claim.
WORKSPACES: dict[str, dict] = {
    "src/p1.md": {
        "main_path": "practice/mti/Mti.java",
        "test_path": "practice/mti/MtiTest.java",
        "run_command": ["java", "practice/mti/Mti.java"],
        "test_command": ["java", "practice/mti/MtiTest.java"],
        "provenance": "bundled",
        "trust": "advisory",
    },
    "src/p2.md": {
        "main_path": "practice/bitmap/Bitmap.java",
        "test_path": "practice/bitmap/BitmapTest.java",
        "run_command": ["java", "practice/bitmap/Bitmap.java"],
        "test_command": ["java", "practice/bitmap/BitmapTest.java"],
        "provenance": "bundled",
        "trust": "advisory",
    },
    "src/p3.md": {
        "main_path": "practice/fields/Fields.java",
        "run_command": ["java", "practice/fields/Fields.java"],
    },
}


def _record(root: Path) -> list[str]:
    """Return the record's lines, or refuse because there is no record."""
    path = Path(root) / RECORD
    if not path.is_file():
        raise CurriculumChanged(
            f"{RECORD} records which unit each practice joins, and it is not there. "
            f"Nothing else in the repository expresses the attachment — a practice "
            f"file does not name its unit and a unit does not name its practice — so "
            f"restore it, or remove the practices from this adapter."
        )
    return path.read_text(encoding="utf-8").splitlines()


def attachments(root: Path) -> dict[str, dict[int, str]]:
    """Return `{address key: {unit ordinal: practice source file}}`.

    ⛔ **The ordinal is the UNIT's**, not a position in this record: a practice
    joins the unit it practises, and the record is where that is written down.
    ⚠️ Two practices on one unit is refused rather than merged — a unit carries
    one `practice_origin`, so the second would silently replace the first.
    """
    found: dict[str, dict[int, str]] = {}
    address: str | None = None
    for number, line in enumerate(_record(Path(root)), start=1):
        label = LABEL.match(line)
        if label is not None:
            address = CONTAINERS.get(label.group("text"), (None,))[0]
            continue
        entry = ENTRY.match(line)
        if entry is None:
            continue
        if address is None:
            raise CurriculumChanged(
                f"{RECORD} line {number} records a practice above the first heading "
                f"naming a container of this corpus. A practice joins a unit, and a "
                f"unit has no identity without the container it is in: put the entry "
                f"under the container's own title, exactly as README.md groups its own."
            )
        href = entry.group("href")
        if href not in WORKSPACES:
            raise CurriculumChanged(
                f"{RECORD} line {number} records a practice this adapter holds no "
                f"workspace for. A practice that sets no work is a reading page with a "
                f"Run button that does nothing: record the file it sets in "
                f"ingest.practices.WORKSPACES, or remove the entry."
            )
        if not (Path(root) / href).is_file():
            raise CurriculumChanged(
                f"{RECORD} line {number} records a practice whose material is not in "
                f"this repository. A broken record link is reported, never skipped "
                f"(R6): restore the file, or remove the entry."
            )
        unit = int(entry.group("n"))
        if unit in found.get(address, {}):
            raise CurriculumChanged(
                f"{RECORD} line {number} records a second practice for one unit. A "
                f"unit declares one 'practice_origin', so the second would silently "
                f"replace the first: put the work in one file, or split it across two "
                f"units."
            )
        found.setdefault(address, {})[unit] = href
    if not found:
        raise CurriculumChanged(
            f"{RECORD} records no practice at all, so this corpus would declare "
            f"'exercises: true' and set no work. Record its practices, or declare "
            f"'exercises: false' in corpus.json and remove this module."
        )
    return found


def _starting_code(root: Path, workspace: dict) -> str:
    """Return the file the reader starts from, read from the file itself."""
    return (Path(root) / workspace["main_path"]).read_text(encoding="utf-8")


def document(root: Path, *, address, variant: str, unit: int, title: str, href: str) -> dict:
    """Return the `build` fields for one unit's practice document.

    ⭐ **The WHOLE of the practice file is this one document.** Its headings are
    the practice's headings and nothing else's, which is exactly what
    `practice_origin` promises `check_completeness`: a lesson block smuggled in
    here would be counted against the practice file and read as a short one.
    """
    root = Path(root)
    workspace = WORKSPACES[href]
    code = _starting_code(root, workspace)
    blocks = parse_markdown((root / href).read_text(encoding="utf-8"))
    return {
        "address": address,
        "variant": variant,
        "unit": unit,
        "kind": PRACTICE,
        "ordinal": 1,
        "title": title,
        "blocks": [*blocks, {"type": "code", "lang": LANGUAGE, "text": code}],
        "starting_code": code,
        "exercise": dict(workspace),
    }


def expected(root: Path) -> dict[str, int]:
    """Return `{address key: practices recorded}`, counted from the record.

    ⛔ Counted from the record's own lines rather than from `attachments`, for
    the reason `ingest.read.expected_units` states: a reading compared with
    itself agrees by construction and catches nothing.
    """
    counts: dict[str, int] = {}
    address: str | None = None
    for line in _record(Path(root)):
        label = LABEL.match(line)
        if label is not None:
            address = CONTAINERS.get(label.group("text"), (None,))[0]
            continue
        if address is not None and ENTRY.match(line):
            counts[address] = counts.get(address, 0) + 1
    return counts

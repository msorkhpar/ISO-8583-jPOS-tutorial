"""Read this source. ⛔ THE ONE MODULE IN THIS PACKAGE YOU WRITE BY HAND.

**What it does.** Turns this repository's material into the two things the
archive is made of — one `Container` per container, and one document's worth of
fields per unit. ⛔ Nothing here knows about the archive's layout, its filenames
or its digests; those are the framework's and are already written.

**How you use it.** `containers(root)`, `documents(root, container)` and
`expected_units(root)`. Run `python3 -m pytest tests/ingest` after each change.

**Depends on.** `studyforge.corpus.container` for the `Container` and `Unit`
shapes and `studyforge.archive.markdown` for the block vocabulary.

## ⛔ The curriculum is recorded in `README.md`, and it STOPS at a label

⚠️ **`README.md` is two documents in one file.** Lines 1-310 record this
corpus: three series, their entries, their titles, their ordinals and the file
each one is served from. Everything from line 310 down is an **outline of
`TestCases.md`** — 361 heading lines reproducing that document's heading tree,
which this corpus declares `not_material`.

⛔ **So the region ends at a label, and the label is the only thing that marks
it.** The lines below it carry no `](src/…)` link, so a reader that merely
*ignored* the label instead of *stopping* at it would sweep 361 heading lines
into the last container, read **zero** extra units out of them, and raise
nothing at all. ⭐ **A silent pass, in exactly the shape this corpus already
documents** — and R6 says the answer to a silent pass is a loud refusal.

⭐ **Hence `STOP_TARGET` and `CurriculumChanged`.** If the record ever stops
carrying that label — because somebody renamed it, moved it, or deleted the
outline it introduces — this reader **refuses** rather than guessing where the
curriculum ends. A refusal costs one message; a guess costs an ingestion
nobody re-counts. `tests/ingest/test_curriculum_region.py` asserts the refusal
in both directions, because a refusal nobody has seen fire is a refusal nobody
knows still works.
"""

from __future__ import annotations

import re
from pathlib import Path

from studyforge.address import Address
from studyforge.archive.markdown import parse as parse_markdown
from studyforge.corpus.container import Container, Unit

#: What every unwritten step says. ⭐ One sentence of what to return, and
#: one of why a guess would be worse than a refusal.
UNWRITTEN = (
    "ingest.read.{step} is not written yet. {what} "
    "Until it is, this adapter refuses rather than emitting an archive "
    "that validates and holds the wrong material."
)

#: The document that records this corpus's curriculum: its groups, the entries
#: under each, their ordinals, their titles and the file each is served from.
#: ⛔ It is the record, not a derivation — §6 rules an address recorded.
RECORD = "README.md"

#: The link target of the label the curriculum region **ends at**. ⛔ Not a
#: line number and not a line's full text: a renamed heading should still stop
#: the region, and only a *missing* one should refuse.
STOP_TARGET = "TestCases.md"

#: ⛔ **The addresses, recorded here and derived from nothing.** The record
#: names each series in the author's own words; those words are the container's
#: title, and this map is where the corpus records what each is *addressed* as
#: (§6 — never `slugify(title)`). ⭐ `origin` is the whole-series aggregate the
#: repository ships for that series: the exact ordered concatenation of the
#: series' own files, asserted by digest, which is why `corpus.json` excludes it
#: from ingestion and why it is nevertheless the honest answer to *where did
#: this container come from*.
CONTAINERS: dict[str, tuple[str, str]] = {
    "ISO-8583 for Visa and Mastercard Transactions: A Comprehensive Developer's Guide": (
        "iso-fundamentals",
        "src/ISO.md",
    ),
    "jPOS Server Implementation": ("jpos-server", "src/Server.md"),
    "Client Implementation": ("jpos-client", "src/Client.md"),
}

#: The single variant this corpus declares, and the one kind it holds. ⭐ Zero
#: graded practices is a finished corpus, not a short one (§11.0, C5).
VARIANT = "prose"
KIND = "lesson"

#: A group label: a level-1 heading in the record. ⚠️ Level is not role here —
#: two *entries* are written as level-2 headings — so role stays positional.
LABEL = re.compile(r"^#[ \t]+(?P<text>\S.*?)[ \t]*$")

#: An entry, in **both** shapes this record uses: 36 as list items and 2 as
#: level-2 headings. ⛔ A parser written for the majority shape drops the other
#: two and raises nothing.
ENTRY = re.compile(
    r"^(?:#{1,6}[ \t]+)?(?P<n>\d+)\.[ \t]+\[(?P<title>.+?)\]\((?P<href>[^)]+)\)[ \t]*$"
)

#: A link into the material, used by the independent source-side count.
MATERIAL_LINK = re.compile(r"\]\((?P<href>src/[^)]+)\)")

#: Emphasis the record wraps 22 of its 38 titles in. ⭐ Presentation, not name.
EMPHASIS = re.compile(r"^\*\*(?P<inner>.+)\*\*$")


class CurriculumChanged(RuntimeError):
    """The record no longer has the shape this reader was written against.

    ⛔ Raised rather than worked around. Every case below is one where carrying
    on would produce an archive that **validates** and holds the wrong
    material — which is the failure this corpus was already measured to invite.
    ⚠️ The message names what changed and what would settle it, never a value
    that could carry personal data (R7).
    """


def _record(root: Path) -> list[str]:
    """Return the record's lines, or refuse because there is no record."""
    path = Path(root) / RECORD
    if not path.is_file():
        raise CurriculumChanged(
            f"{RECORD} is the only document that records this corpus's groups, "
            f"ordinals, titles and addresses, and it is not there. Nothing else "
            f"in the repository expresses the grouping, so there is nothing to "
            f"fall back to: restore it, or record the curriculum somewhere this "
            f"reader is taught to look."
        )
    return path.read_text(encoding="utf-8").splitlines()


def _region(lines: list[str]) -> list[tuple[int, str]]:
    """Return the curriculum region: every line above the label it stops at.

    ⛔ **The stop is mandatory.** Below it the record carries an outline of a
    document this corpus declares `not_material`: 361 heading lines with no
    link into the material at all. A reader that ignored the label instead of
    stopping would fold every one of them into the last container, read zero
    units out of them, and report success.
    """
    for number, line in enumerate(lines, start=1):
        match = LABEL.match(line)
        if match is not None and STOP_TARGET in match.group("text"):
            return list(enumerate(lines[: number - 1], start=1))
    raise CurriculumChanged(
        f"{RECORD} no longer carries the group label linking {STOP_TARGET}, which is "
        f"where this corpus's curriculum ends. Everything below it is an outline of "
        f"that document rather than material, and it carries no link into src/ — so "
        f"reading on would add those lines to the last container, find no units in "
        f"them and raise nothing. This reader refuses instead. Settle it by restoring "
        f"the label, or by teaching this reader the new marker the record uses."
    )


def _titled(title: str) -> str:
    """Return a recorded title without the emphasis it is presented in."""
    match = EMPHASIS.match(title.strip())
    return (match.group("inner") if match else title).strip()


def _grouped(region: list[tuple[int, str]]) -> list[tuple[str, list[re.Match[str]]]]:
    """Split the region into `(label, entries)`, in the order the record writes them."""
    groups: list[tuple[str, list[re.Match[str]]]] = []
    for number, line in region:
        entry = ENTRY.match(line)
        if entry is not None:
            if not groups:
                raise CurriculumChanged(
                    f"{RECORD} line {number} records an entry above the first group "
                    f"label. This corpus has one container level, so an entry outside "
                    f"a group has no address. Settle it by giving it a label."
                )
            groups[-1][1].append(entry)
            continue
        label = LABEL.match(line)
        if label is None:
            continue
        text = label.group("text")
        if text not in CONTAINERS:
            raise CurriculumChanged(
                f"{RECORD} line {number} opens a group this reader has no address "
                f"for. An address is recorded, never derived (§6), so a new group "
                f"is a decision: add it to ingest.read.CONTAINERS with the address "
                f"it is served at and the aggregate it was read from."
            )
        groups.append((text, []))
    return groups


def containers(root: Path) -> list[Container]:
    """Return one `Container` per container this source records.

    ⛔ **The address is recorded, never derived** (§6): `CONTAINERS` is the
    record of it and no title is slugified here.

    ⭐ Each container carries `origin` — the path of the file the material was
    read from, verbatim — and one `Unit` per unit, with its declared practice
    count, which is zero for every unit of this corpus.
    """
    root = Path(root)
    found = []
    for label, entries in _grouped(_region(_record(root))):
        address, origin = CONTAINERS[label]
        units = []
        for position, entry in enumerate(entries, start=1):
            ordinal = int(entry.group("n"))
            if ordinal != position:
                raise CurriculumChanged(
                    f"{RECORD} numbers the entries of one group out of sequence: "
                    f"entry {position} of {label!r} is recorded as {ordinal}. The "
                    f"ordinal is the reading order, so a gap or a repeat is a "
                    f"decision about the order rather than a typo to smooth over."
                )
            href = entry.group("href")
            if not (root / href).is_file():
                raise CurriculumChanged(
                    f"{RECORD} records an entry of {label!r} whose material is not "
                    f"in this repository. A broken curriculum link is reported, "
                    f"never skipped (R6): restore the file, or remove the entry."
                )
            units.append(
                Unit(n=ordinal, title=_titled(entry.group("title")), practices=0, origin=href)
            )
        if not units:
            raise CurriculumChanged(
                f"{RECORD} opens the group {label!r} and records no entry under it. "
                f"An empty container reaches the contents page as an empty row."
            )
        found.append(
            Container(
                address=Address.of(address),
                titles=(label,),
                variant=VARIANT,
                # ⛔ Replaced by `emit` with the run's own date (INT-09/3); a
                # date recorded here would be a clock in the reader (R10).
                ingested="1970-01-01",
                units=tuple(units),
                origin=origin,
            )
        )
    return found


def documents(root: Path, container: Container) -> list[dict]:
    """Return the fields for each document of `container`, in reading order.

    ⭐ One `lesson` per unit and nothing else: this corpus declares no
    exercises, so there is no practice path to take (§7's three states, C5).

    ⛔ **Every block comes from the framework's vocabulary**, through its own
    strict reader — a construct it does not recognise raises there rather than
    being dropped here, and a dropped block is absent from the digest and the
    counts alike.
    """
    root = Path(root)
    fields = []
    for unit in container.units:
        text = (root / unit.origin).read_text(encoding="utf-8")
        fields.append(
            {
                "address": container.address,
                "variant": container.variant,
                "unit": unit.n,
                "kind": KIND,
                "ordinal": 1,
                "title": unit.title,
                "blocks": parse_markdown(text),
            }
        )
    return fields


def expected_units(root: Path) -> dict[str, int] | None:
    """Return `{address key: unit count}` counted from the SOURCE.

    ⛔ **This is the check `studyforge validate` cannot make**, so it does not
    reuse `containers`: it counts the record's own links into `src/`, group by
    group, which is a different reading of the same document. ⭐ Two readings
    that disagree is the signal; one reading compared with itself is not.

    ⚠️ It shares one thing with `containers` deliberately — `_region`, and
    therefore the refusal. A count taken past the stop label would be a count
    of the wrong document, and *both* readings have to stop in the same place
    or the audit would quietly bless the failure it exists to catch.
    """
    counts: dict[str, int] = {}
    label: str | None = None
    for _, line in _region(_record(Path(root))):
        opening = LABEL.match(line)
        if opening is not None and opening.group("text") in CONTAINERS:
            label = opening.group("text")
            counts.setdefault(CONTAINERS[label][0], 0)
            continue
        if label is not None and MATERIAL_LINK.search(line) and ENTRY.match(line):
            counts[CONTAINERS[label][0]] += 1
    return counts

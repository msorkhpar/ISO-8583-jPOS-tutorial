"""The curriculum region STOPS at a label, and its absence is a refusal.

**What it does.** Asserts the one failure this corpus's record is actually
shaped to invite: `README.md` is two documents in one file, and the second is
an outline of a document the corpus declares `not_material`. ⛔ The lines below
the label carry **no link into `src/`**, so a reader that ignored the label
instead of stopping at it would fold 361 heading lines into the last container,
read **zero** extra units out of them, and report success.

⭐ **So the refusal is asserted in both directions** — it does not fire on the
record as it stands, and it does fire the moment the label it keys on is gone.
A refusal nobody has watched fire is a refusal nobody knows still works.

**How you use it.** `python3 -m pytest tests/ingest` from the corpus root.

**Depends on.** `ingest.read`, and a copy of the repository in a tmp directory
— ⛔ never the repository itself: R3 says no existing file is rewritten, and a
test that doctored `README.md` in place would be the violation it is checking.

⚠️ **Hand-written, and it is a finding rather than a fixture** (R19): the
adapter scaffold generates the contract tests and cannot generate this one,
because the anchor a reader keys on is source-specific. What the skill could
have generated is the *obligation* — the findings log for this run.
"""

from __future__ import annotations

import shutil
from pathlib import Path

import pytest

from ingest import practices, read

CORPUS_ROOT = Path(__file__).resolve().parents[2]

#: The counts `README.md` records, by address. ⛔ Asserted, never assumed.
RECORDED = {"iso-fundamentals": 16, "jpos-server": 11, "jpos-client": 11}

#: ⭐ **Which unit each practice joins**, recorded in `src/Practice.md`, which
#: `README.md` does not mention at all. ⛔ **A practice adds NO unit** (`W428`):
#: it is a second document on a unit this record already carries, so these
#: ordinals are units of `RECORDED` and never entries beside them.
PRACTICES = {"iso-fundamentals": {2: "src/p2.md", 3: "src/p3.md", 4: "src/p1.md"}}

#: Every container this corpus reads. ⭐ It is `RECORDED` exactly, and that is
#: the reading `W428` changed: this corpus used to carry a fourth container of
#: practices, and now it carries none.
EVERY = dict(RECORDED)

#: The last unit of the last series `README.md` records.
LAST_RECORDED = ("jpos-client", "src/c11.md")


def _authored(address: str, unit: int) -> int:
    """How many exercises the authoring pass committed on `unit`: its bundle directories."""
    return len(list((CORPUS_ROOT / "exercises" / address / "prose" / f"unit-{unit:02d}").glob("practice-*")))


def _material_copy(tmp_path: Path) -> Path:
    """Return a copy of just what the reader reads, so the real tree is untouched."""
    root = tmp_path / "corpus"
    (root / "src").mkdir(parents=True)
    shutil.copy2(CORPUS_ROOT / read.RECORD, root / read.RECORD)
    for path in (CORPUS_ROOT / "src").glob("*.md"):
        shutil.copy2(path, root / "src" / path.name)
    return root


def test_the_region_stops_at_the_label_and_nothing_below_it_is_read():
    found = read.containers(CORPUS_ROOT)
    assert {c.address.key: len(c.units) for c in found} == EVERY
    assert sum(len(c.units) for c in found) == sum(EVERY.values())
    # ⛔ The last container the RECORD carries ends where the record ends it. A
    # reader that ran on would still report exactly this, which is why the count
    # is not the check.
    key, origin = LAST_RECORDED
    last = next(c for c in found if c.address.key == key)
    assert last.units[-1].origin == origin
    assert all(unit.origin.startswith("src/") for c in found for unit in c.units)


def test_every_practice_is_declared_on_the_unit_it_practises():
    # ⛔ `W428`, and it is the user's own clause: a practice is part of its
    # topic page, so it reaches the reader as a second document of that unit
    # and never as a container of its own.
    found = {c.address.key: c for c in read.containers(CORPUS_ROOT)}
    for address, attached in PRACTICES.items():
        for ordinal, href in attached.items():
            unit = found[address].unit(ordinal)
            assert unit.practice_origin == href, (address, ordinal)
            # ⭐ The source's practice is the unit's first; the authoring pass's
            # own exercises (ISO-23) are numbered after it and counted with it.
            assert unit.practices == 1 + _authored(address, ordinal)
            # ⛔ The prose file is untouched: the practice joined the page, it
            # did not replace the material (R3).
            assert unit.origin != href
    attached_units = {(a, n) for a, units in PRACTICES.items() for n in units}
    assert {
        (c.address.key, u.n)
        for c in found.values()
        for u in c.units
        if u.practice_origin is not None
    } == attached_units


def test_a_practice_brings_its_own_file_and_never_edits_the_unit_s():
    # ⭐ The reason `practice_origin` exists. Every prose unit file is INCLUDED
    # by this corpus's own content policy, and `corpus.manifest.edits` refuses
    # an edit to an included file however it is declared — so the practice's
    # material has to arrive beside it.
    origins = {u.origin for c in read.containers(CORPUS_ROOT) for u in c.units}
    for attached in PRACTICES.values():
        for href in attached.values():
            assert href not in origins, "a practice file is no unit's prose"
            assert (CORPUS_ROOT / href).is_file()


def test_the_practice_document_is_the_whole_of_its_own_file():
    # ⛔ What the second declaration promises `check_completeness`: the
    # practice's headings are counted against the practice file, so a lesson
    # block that strayed in here would read as a short read on that file.
    found = {c.address.key: c for c in read.containers(CORPUS_ROOT)}
    for address, attached in PRACTICES.items():
        container = found[address]
        documents = read.documents(CORPUS_ROOT, container)
        for ordinal, href in attached.items():
            kinds = [d["kind"] for d in documents if d["unit"] == ordinal]
            authored = _authored(address, ordinal)
            assert kinds == ["lesson", "practice"] + ["practice"] * authored, (address, ordinal)
            practice = next(
                d
                for d in documents
                if d["unit"] == ordinal and d["kind"] == "practice" and d["ordinal"] == 1
            )
            headings = sum(1 for b in practice["blocks"] if b["type"] == "heading")
            source = (CORPUS_ROOT / href).read_text(encoding="utf-8")
            assert headings == sum(1 for line in source.splitlines() if line.startswith("#"))
            assert practice["exercise"]["main_path"] in practices.WORKSPACES[href]["main_path"]


def test_the_two_readings_of_the_record_agree():
    # ⭐ `expected_units` is the source-side count the framework cannot make.
    # It is a different reading of the same document; agreement is the signal.
    assert read.expected_units(CORPUS_ROOT) == EVERY


def test_the_practices_are_counted_from_their_own_record():
    # ⚠️ `expected_units` no longer counts them at all, because they add no
    # unit — so a corpus whose practices all vanished would still be counted
    # complete there. This is the count that would notice.
    assert practices.expected(CORPUS_ROOT) == {a: len(u) for a, u in PRACTICES.items()}


def test_the_reader_refuses_when_the_label_it_keys_on_is_gone(tmp_path):
    root = _material_copy(tmp_path)
    record = root / read.RECORD
    lines = record.read_text(encoding="utf-8").splitlines()
    kept = [line for line in lines if read.STOP_TARGET not in line]
    assert len(kept) == len(lines) - 1, "exactly one line carries the label"
    record.write_text("\n".join(kept) + "\n", encoding="utf-8")

    with pytest.raises(read.CurriculumChanged) as refused:
        read.containers(root)
    assert read.STOP_TARGET in str(refused.value)

    # ⛔ And the count refuses too. If only one of the two stopped, the audit
    # would compare a short archive with a short expectation and agree.
    with pytest.raises(read.CurriculumChanged):
        read.expected_units(root)


def test_removing_the_label_alone_would_not_have_changed_the_count(tmp_path):
    """⛔ The measurement that makes the refusal necessary rather than tidy.

    With the label gone and no refusal, a link-driven parse reads the same 38
    units: the 361 lines it swallows carry no link into `src/` at all. ⭐ So
    nothing downstream could have noticed — not the count, not `validate`, not
    the audit — and only the refusal can.
    """
    root = _material_copy(tmp_path)
    record = root / read.RECORD
    lines = record.read_text(encoding="utf-8").splitlines()
    below = lines[next(i for i, line in enumerate(lines) if read.STOP_TARGET in line) + 1 :]
    assert len(below) > 300, "the outline below the label is the bulk of the file"
    assert not [line for line in below if read.MATERIAL_LINK.search(line)], (
        "no line below the label links into src/, which is why a link-driven "
        "parse reads zero extra units and raises nothing"
    )
    assert len([line for line in below if line.startswith("#")]) > 300, (
        "and they are headings, so a heading-driven parse would read them as units"
    )


def test_the_reader_refuses_a_group_it_has_no_address_for(tmp_path):
    root = _material_copy(tmp_path)
    record = root / read.RECORD
    text = record.read_text(encoding="utf-8")
    record.write_text(text.replace("# Client Implementation", "# Client Implementations"), "utf-8")
    with pytest.raises(read.CurriculumChanged) as refused:
        read.containers(root)
    assert "address" in str(refused.value)

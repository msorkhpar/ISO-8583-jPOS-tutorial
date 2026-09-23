"""ISO-21: committed exercise bundles reach the archive, and an unproven one never does.

**What it does.** Writes a throwaway authored bundle into a COPY of this corpus,
runs the adapter there and validates what it emitted. ⛔ No bundle is ever
written into this repository: `exercises/` stays empty here until the authoring
pass (ISO-22) fills it.

**How you use it.** `python3 -m pytest tests/ingest/test_exercises.py` from the corpus root.

**Depends on.** `ingest.emit`, `ingest.exercises`, `studyforge.validate`, and the
bundle and gate-record formats. ⭐ The fixture's gate record is digested from the
fixture's real files, as the framework's own bundle tests write theirs. The
gates themselves are not run here: running them is the authoring pass's job,
and this suite tests what the ADAPTER does with a record.
"""

from __future__ import annotations

import json
import shutil
import subprocess
from pathlib import Path

import pytest

from studyforge.corpus.container import CONTAINER_FILENAME
from studyforge.exercise.bundle import bundle_of
from studyforge.exercise.gates import GateRecord, Verdict, record_document, taken_over
from studyforge.validate import validate

from ingest.emit import emit
from ingest.exercises import BundleRefused

CORPUS_ROOT = Path(__file__).resolve().parents[2]
INGESTED = "2026-01-01"

#: The generated site's directory. ⚠️ Left out of the copy only for speed: it is
#: a build's output, and nothing this suite emits or validates reads it.
SITE = "src/study/"

HELD = ("G1", "G2", "G3", "G4", "G5")


def _copy(tmp_path: Path) -> Path:
    """This corpus's tracked files, less the built site, where a test may write."""
    where = tmp_path / "corpus"
    listed = subprocess.run(
        ["git", "-C", str(CORPUS_ROOT), "ls-files", "-z"],
        capture_output=True,
        check=False,
    )
    if listed.returncode != 0:
        pytest.skip("this corpus root is not a git working tree, so there is no file list to copy")
    for name in listed.stdout.decode("utf-8").split("\0"):
        if not name or name.startswith(SITE) or name.startswith("archive/"):
            continue
        target = where / name
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(CORPUS_ROOT / name, target)
    subprocess.run(["git", "init", "-q", str(where)], check=True)
    return where


def _bundle(root: Path, *, address: str, unit: int, ordinal: int, section: str, origin: str) -> Path:
    """Write one complete Java bundle under `root`, and return its directory."""
    declared = {
        "bundle_api": 1,
        "address": [address],
        "variant": "prose",
        "unit": unit,
        "ordinal": ordinal,
        "title": "Echo an MTI",
        "lang": "java",
        "main_file": "Echo.java",
        "test_file": "EchoTest.java",
        "run_command": ["java"],
        "test_command": ["java"],
        "provenance": "generated",
        "trust": "advisory",
        "cases": [
            {"id": "EchoTest#echoes", "kind": "main", "says": "returns the MTI it was given"},
            {"id": "EchoTest#refusesShort", "kind": "edge", "says": "refuses three digits"},
        ],
        "report": {"format": "junit", "path": "target/surefire-reports"},
        "origin": {"path": origin, "section": section},
    }
    workspace = bundle_of(declared, "bundle.json").places.workspace
    declared["run_command"] = ["java", f"{workspace}/Echo.java"]
    declared["test_command"] = ["java", f"{workspace}/EchoTest.java"]
    places = bundle_of(declared, "bundle.json").places
    here = root / places.bundle
    files = {
        "bundle.json": json.dumps(declared, indent=2) + "\n",
        "statement.md": "Return the MTI you are given, and refuse one that is not four digits.\n",
        "starter/Echo.java": "class Echo { static String echo(String mti) { return null; } }\n",
        "reference/Echo.java": "class Echo { static String echo(String mti) { return mti; } }\n",
        "tests/EchoTest.java": "class EchoTest { public static void main(String[] a) {} }\n",
        "plants/edge-1/Echo.java": "class Echo { static String echo(String m) { return m; } }\n",
    }
    for name, text in files.items():
        (here / name).parent.mkdir(parents=True, exist_ok=True)
        (here / name).write_text(text, encoding="utf-8")
    record = GateRecord(
        inputs=taken_over(
            here,
            (
                ("statement", "statement.md"),
                ("starter", "starter/Echo.java"),
                ("reference", "reference/Echo.java"),
                ("tests", "tests/EchoTest.java"),
                ("plant:EchoTest#refusesShort", "plants/edge-1/Echo.java"),
            ),
            places.bundle,
        ),
        origins=(),
        verdicts=tuple(
            Verdict(id=gate, family="code", held=True, says="the gate held", recorded=())
            for gate in HELD
        ),
    )
    (root / places.gates).write_text(
        json.dumps(record_document(record), indent=2) + "\n", encoding="utf-8"
    )
    return here


def _units(root: Path, address: str) -> dict[int, int]:
    """Return `{unit: practices}` as the emitted container map declares it."""
    found = json.loads((root / "archive" / address / CONTAINER_FILENAME).read_text("utf-8"))
    return {unit["n"]: unit["practices"] for unit in found["units"]}


def _two_bundles(tmp_path: Path) -> Path:
    """A copy carrying an authored practice-2 on a unit with the source's practice-1,
    and an authored practice-1 on a unit with none, emitted."""
    root = _copy(tmp_path)
    _bundle(root, address="iso-fundamentals", unit=2, ordinal=2,
            section="2.2. Bitmaps", origin="src/2.md")
    _bundle(root, address="jpos-client", unit=8, ordinal=1,
            section="8.1 jPOS Logging Framework", origin="src/c8.md")
    emit(root, ingested=INGESTED)
    return root


def test_a_unit_with_the_source_practice_gets_practice_2_and_its_count_raised(tmp_path):
    root = _two_bundles(tmp_path)
    unit_2 = root / "archive/iso-fundamentals/raw/prose/unit-02"
    assert (unit_2 / "practice-1.json").is_file() and (unit_2 / "practice-2.json").is_file()
    assert (root / "archive/jpos-client/raw/prose/unit-08/practice-1.json").is_file()
    assert _units(root, "iso-fundamentals")[2] == 2
    assert _units(root, "jpos-client")[8] == 1
    authored = json.loads((unit_2 / "practice-2.json").read_text("utf-8"))
    assert authored["exercise"]["provenance"] == "generated"
    assert authored["exercise"]["main_path"].startswith(
        "practice/iso-fundamentals/prose/unit-02/practice-2/"
    )
    # ⛔ The source's own practice is byte-unchanged by an authored one beside it.
    original = CORPUS_ROOT / "archive/iso-fundamentals/raw/prose/unit-02/practice-1.json"
    emitted = json.loads((unit_2 / "practice-1.json").read_text("utf-8"))
    kept = json.loads(original.read_text("utf-8"))
    assert {**emitted, "ingested": None} == {**kept, "ingested": None}


def test_validate_accepts_the_archive_with_authored_practices(tmp_path):
    # ⭐ ISO-21's acceptance. It was pinned RED on ISO-21/1 until W444
    # (376ebb46) left an authored practice out of source completeness.
    root = _two_bundles(tmp_path)
    report = validate(root)
    assert report.ok, "\n".join(report.lines())
    assert not report.unchecked, "\n".join(report.lines())


def test_with_no_bundle_the_archive_is_what_it_was(tmp_path):
    # ⭐ R10: this module adds nothing to a corpus with no committed bundle.
    root = _copy(tmp_path)
    emit(root, ingested=INGESTED)
    for path in sorted((CORPUS_ROOT / "archive").rglob("*.json")):
        where = path.relative_to(CORPUS_ROOT)
        emitted = json.loads((root / where).read_text("utf-8"))
        kept = json.loads(path.read_text("utf-8"))
        assert {**emitted, "ingested": None} == {**kept, "ingested": None}, where


def test_a_bundle_the_gates_never_cleared_is_refused(tmp_path):
    root = _copy(tmp_path)
    here = _bundle(root, address="jpos-client", unit=8, ordinal=1,
                   section="8.1 jPOS Logging Framework", origin="src/c8.md")
    (here / "gates.json").unlink()
    with pytest.raises(BundleRefused, match="ships no gate record"):
        emit(root, ingested=INGESTED)
    assert not (root / "archive").exists(), "a refused run moved an archive into place"


def test_a_gate_record_that_did_not_clear_is_refused(tmp_path):
    root = _copy(tmp_path)
    here = _bundle(root, address="jpos-client", unit=8, ordinal=1,
                   section="8.1 jPOS Logging Framework", origin="src/c8.md")
    record = json.loads((here / "gates.json").read_text("utf-8"))
    record["gates"][2]["held"] = False
    (here / "gates.json").write_text(json.dumps(record, indent=2) + "\n", encoding="utf-8")
    with pytest.raises(BundleRefused, match="not every gate held"):
        emit(root, ingested=INGESTED)


def test_a_bundle_edited_after_its_gates_is_refused(tmp_path):
    root = _copy(tmp_path)
    here = _bundle(root, address="jpos-client", unit=8, ordinal=1,
                   section="8.1 jPOS Logging Framework", origin="src/c8.md")
    (here / "reference/Echo.java").write_text("class Echo {}\n", encoding="utf-8")
    with pytest.raises(BundleRefused, match="no longer matches"):
        emit(root, ingested=INGESTED)


def test_an_authored_ordinal_that_collides_with_the_source_practice_is_refused(tmp_path):
    # ⛔ W437: unit 2 already carries practice-1, so an authored practice-1 would
    # overwrite it. Refused, naming the numbering it needed.
    root = _copy(tmp_path)
    _bundle(root, address="iso-fundamentals", unit=2, ordinal=1,
            section="2.2. Bitmaps", origin="src/2.md")
    with pytest.raises(BundleRefused, match=r"must be numbered \[2\]"):
        emit(root, ingested=INGESTED)

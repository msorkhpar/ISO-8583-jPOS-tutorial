# ISO-8583 for Visa and Mastercard Transactions: A Comprehensive Developer's Guide

This repository was onboarded by `studyforge`. Everything below is
generated from `corpus.json` and from what the build reads — edit the
manifest, not this file (R19).

## What this corpus declares

- source: `iso-8583-jpos-tutorial`
- 1 container level(s): group
- variants: prose
- placement profile: `sibling`
- graded practices: yes
- media: committed to git

## Running it from a fresh clone

The framework is a checkout beside this repository's main checkout —
this repository itself unless it is a linked worktree — never a submodule
and never installed. Clone it there; it is `../../studyforge` from this
repository's root, and these run from there. They pin the framework,
ingest with this corpus's adapter, check the archive, and say what a
build would write before building:

```
git -C ../../studyforge checkout --detach a5373c1b9a3a4a07f5e15d0a6da95c19f9ea0680
PYTHONPATH=../../studyforge/src python3 -m ingest .
PYTHONPATH=../../studyforge/src python3 -m studyforge.cli validate .
PYTHONPATH=../../studyforge/src python3 -m studyforge.cli plan .
PYTHONPATH=../../studyforge/src python3 -m studyforge.cli build . --out .
```

The adapter stamps today's date as `ingested`; pass a date after `.` to
reproduce an earlier archive byte for byte. Narration needs a running
narration service, so it is not run here; its options are:

```
PYTHONPATH=../../studyforge/src python3 -m studyforge.cli narrate --help
```

## Where it stands

How many units this corpus has, how many are narrated and whether any
needs a container are read from the archive and the narration record,
and they move whenever either does — narrating writes clips, ingesting
again rewrites the archive. Nothing rewrites this file when they move,
so it states no figure: this reads them as they are now.

```
PYTHONPATH=../../studyforge/src python3 -m studyforge.skills.onboarding .
```

## What you get

This corpus declares graded practices, so it reaches the execution
track as well as the reading floor: pages, narration, contents,
navigation and progress offline, plus Run and Submit against a
pinned toolchain.

## The one file that is yours

- `ingest/read.py`

Every other file here is generated. A hand-edit to one is reverted the
next time onboarding runs, so a difference you need is a field the
manifest is missing — which is a finding, not an edit.

## What generation touches

Nothing that already exists. Every artifact is an addition, and
`tests/test_non_destructive.py` fails the build if that stops being true.

## When the media stops fitting in git

Generated media is committed by default. If this corpus crosses
the footprint limits its manifest declares, there are two ways
forward and onboarding will not pick one for you: stop committing
media and regenerate it locally, or raise the declared limits
deliberately. Both are a change to `corpus.json`.

# ISO-29 handoff: the third page-defect fix pass

**Office** `po-int`. **Branch** `int/m10-iso-23`, on top of ISO-28 (`99c607d`). **Register
ruling, 2026-09-23, on ISO-28's findings:** they are fixed. **Runner**
`code-server-toolchain/runner:java-maven-amd64-7a5a2f2aacba`, offline. **Framework:** the release
tip had moved to `39f12d4e` (PO round 174, board commits only; `src/` has no diff from
`de1ea776`). The corpus was re-pinned to it through `reonboard` before the regeneration, so the
pin and the code that ran are the same ref.

| commit | what |
|---|---|
| `4a9566c` | the content: 9 source pages, no heading changed |
| `d14f2ea` | re-pin to `39f12d4e` through `reonboard`; `hand_edited` reads `[]` before and after |
| `71119bf` | re-gate at the pin, keeping the ledger, then re-emit and rebuild |
| this commit | this handoff, and a row in `m10-plan.md` |

## Method

This pass used the same method as ISO-28:

- **Each finding was confirmed first**, by a failing compile, a failing test or `javap` on the
  2.1.7 jar.
- **Each fix is minimal and in the page's voice.**
- **No heading changed**, and no fence was added, removed or reordered: `outline.py` is
  identical for all 9 pages.
- **Every changed fence is proven** offline in `…-7a5a2f2aacba` against `java-build/pom.xml`,
  with JUnit and with real Q2 for descriptors. Each proof injects its fence verbatim, and this
  office re-ran all 8 proof runs: all OK.
- **A factual claim needs a source that can be quoted.** Where there is none, the page no longer
  asserts the claim.

## Every page changed, and the finding each fix addresses

| page | finding | fix | source cited |
|---|---|---|---|
| `src/6.md` | "Void: 02" and "cash advance: 01" were asserted as standard values but never confirmed | This time too they could not be confirmed. The page now says the transaction-type values vary by network, and names these codes as examples; the closing list agrees. Fence 4's unused `BeforeEach` import is removed | jPOS 2.1.7 ships no processing-code table. Its only resources are the packager XMLs (field 3 "Processing Code", n6), `ISOCurrency.properties` and the Q2 files; `org.jpos.rc.CMF` holds result codes, not processing codes |
| `src/7.md` | The 1993 chargeback (`1422`) carried no original data elements | Fence 3 now sets field 56: original MTI (4) + STAN (6) + local date and time (12) + acquirer ID (LL + up to 11). Proven by a pack/unpack round trip with the iso93 packager, and a 36-digit value is refused | `iso93ascii.xml`: 56 "Original data elements" `IFA_LLNUM` 35, 11 STAN 6, 12 local date and time 12, 32 acquirer ID `IFA_LLNUM` 11. The subfield order is ISO 8583:1993's, as the register's dispatch states it; the standard's own text was not available. The fence's `Transaction` getters are implied: the page never defines `Transaction` |
| `src/8.md` | Fence 2 needed an LMK file the page never explained, and on Java 17+ an `--add-exports` flag | One paragraph after fence 2: create the LMK once with `rebuildlmk` set to `true`, and run with `--add-exports java.base/com.sun.crypto.provider=ALL-UNNAMED`. A proof that follows exactly those instructions runs fence 2 end to end, and fails without either. Fence 3's unused `SecretKeySpec` import is removed | 2.1.7 `JCESecurityModule` bytecode (`javap`): it reads `provider`, `lmk` and `rebuildlmk`, writes the LMK when `rebuildlmk` is set, refuses a missing file, and loads `com.sun.crypto.provider.SunJCE` by name when no provider is set. JEP 403 covers Java 17 encapsulation |
| `src/c1.md` | Fence 6's `packager-config` named `cfg/packager/iso87ascii.xml`, which no fence provides | It is now `jar:packager/iso87ascii.xml`, the packager jPOS ships. Proven under real Q2 (the channel's `GenericPackager` loads and packs an 0800); the old path fails with `FileNotFoundException` | the resource list of `jpos-2.1.7.jar` (`packager/iso87ascii.xml`) |
| `src/c3.md` | Three prose phrases called the channel an "SSL channel" | Each now says "a channel over SSL/TLS", in line with ISO-28's renamed headings. Prose only | — |
| `src/s3.md` | Fence 1's `ISOServer.setConfiguration` keys were never checked | `ISOServer` reads only `allow`, `deny`, `backlog`, `ignore-iso-exceptions` and `bind-address`, so `port`, `channel`, `packager` and `max-connections` were dead keys and are removed. `timeout` is a channel key, so the same configuration now also goes to the channel. The prose says the `ThreadPool` caps connections. Proven on loopback: the 500 ms timeout disconnects, an idle client stays without it, `ThreadPool(1,2)` refuses a third client, and 0800 → 0810 still works | 2.1.7 `ISOServer` and `BaseChannel` bytecode (`javap`) |
| `src/s4.md`, `s5.md`, `s6.md` | The Q2 descriptors named `com.example…` / `com.yourcompany…` classes, but the fences declare no package | The descriptors now use the fences' own default-package names, keeping the pages' style. Proven under real Q2 with the classes in the default package. The old descriptors fail to deploy | — |
| `src/s5.md` | The page said `CardTypeIdentifierParticipant` ensures the correct participant is used, yet both network participants ran on every transaction | Option (a), which keeps the lesson's point: each network participant reads `CARD_TYPE` and steps aside for the other network (`PREPARED | NO_JOIN | READONLY`), as s4 teaches. Fence 6's tests set `CARD_TYPE`. Proven under real Q2: a Visa transaction sets no `MASTERCARD_*` value, and the reverse | — |

## Regeneration, at the pin `39f12d4e`

- Only the 9 fixed units' `exercises/` and `practice/` directories were removed. ⛔ **The ledger
  was kept.** The archive was re-emitted first.
- The pass covered all 38 pages and re-gated 8 exercises (jpos-client unit 1 is a zero plan).
  **All cleared, with no shortfall.**
- No fix moved a heading or a basis, and no fix changed what an exercise rests on. Each
  statement was re-read against its fixed page. **Every bundle's statement, starter, reference,
  tests and plants are byte-identical**; only `coverage.json` and `gates.json` changed.
- **Ledger:** 40 sources and 220 entries before and after, with the same keys. Nothing was
  dropped or added. 11 entries and 9 source rows changed, all on the 9 fixed pages, and
  `authored.ledger` listed exactly those as `changed`.

## Proof

- `studyforge validate .` at the pin `39f12d4e`, which is also the release tip: **GREEN, exit
  0**, check 20 included.
- The corpus suite: **GREEN, exit 0.**
- **Site:** the 9 fixed pages changed, and no other.
- **Browser**, on jpos-server unit 5, whose routing code changed (headless Chrome, a spare
  loopback port, never `:8770`, through the scratch `mvn` shim on `…-7a5a2f2aacba`):
  - The page shows the `CARD_TYPE` checks.
  - Submit on the starter: "Main ask: not yet. Edge cases 0 of 2".
  - Submit with the reference: "Main ask: done. Edge cases 2 of 2".
  - The starter was restored afterwards.

## Findings (not edited)

- `src/s3.md` fence 1: `startService()` calls `isoServer()` directly. That builds a second
  `ISOServer` beside the Spring bean and registers it again, so both would bind the same port if
  both were started.
- `src/s3.md` fence 1: `createConfiguration() throws ISOException` declares an exception it
  never throws. This is harmless.
- `src/7.md`: the subfield order of field 56 follows the dispatch's statement of ISO 8583:1993.
  The field's length and type are jPOS's. The standard's own text was not available to quote.

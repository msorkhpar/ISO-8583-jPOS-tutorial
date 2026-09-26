# ISO-28 handoff: the second page-defect fix pass, headings included

**Office** `po-int`. **Branch** `int/m10-iso-23`, on top of ISO-27 (`588e3e5`). **Register
ruling, 2026-09-23:** the recorded defects are fixed. It covers every defect ISO-27 recorded but did not edit, and
the headings too. **Runner** `code-server-toolchain/runner:java-maven-amd64-7a5a2f2aacba`,
offline. **Framework:** re-pinned to `de1ea776` (it carries `W456`), and the regeneration ran at
that same ref, as the register ruled.

| commit | what |
|---|---|
| `a2b6cd5` | the content: 12 source pages and 6 headings. Also, the ISO-23 and ISO-27 handoffs no longer advise deleting the ledger (`W456/3`) |
| `cff1c5d` | re-pin to `de1ea776` through `reonboard`; `hand_edited` reads `[]` before and after |
| `0912093` | re-gate the exercises on the fixed pages at the pin, re-emit the archive and rebuild the site; the ledger is merged in place |
| this commit | this handoff, and a row in `m10-plan.md` |

## Method

This pass used the same method as ISO-27:

- **Each defect was confirmed first**, by a failing compile or test against jPOS 2.1.7.
- **Each fix is minimal and in the page's voice.**
- **No fence was added, removed or reordered.** `outline.py` gives the same fence count on all 12
  pages, and the heading list differs only by the six renames below.
- **Every changed fence is proven** offline in `…-7a5a2f2aacba`, against `java-build/pom.xml`,
  with JUnit wherever it makes a behaviour claim. Where it is feasible offline, a Q2 descriptor
  is started under real Q2.
- **The proofs inject each fence verbatim**, and they were regenerated from the committed pages.
  This office re-ran all 14 proof runs, and all read OK.

## Every page changed, and the defect each fix addresses

| page | defect | fix |
|---|---|---|
| `src/6.md` | Fence 4 used AssertJ | JUnit `Assertions`, with the same values. ⚠️ **Left unedited:** "Void: 02" and "cash advance: 01". Neither could be confirmed against the standard's or Mastercard's text: jPOS carries no transaction-type table, and the standard is not available here |
| `src/7.md` | The 1993 chargeback (`1422`) put its reason in field 39 (the 1993 action code). Field 56 (1993: original data elements) held free text. Approval was checked as `"00"` on an n3 field. Fence 4 used the JUnit 4 `SpringRunner`, `@SpringBootTest`, `@MockBean` and AssertJ | The reason now goes in field 25, the message reason code. The free text in field 56 is removed. Approval is the action code `"000"`. The field meanings come from jPOS's `iso93ascii.xml`/`iso87ascii.xml`; the standard's own text for "000 = approved" could not be quoted, so it rests on the n3 shape and jPOS's CMF codes. Fence 4 now uses JUnit 5 and Mockito. The prose no longer names AssertJ |
| `src/8.md` | Fence 2 (`KeyManager`) used a `JCESecurityModule` constructor and `encryptKey`/`decryptKey` that 2.1.7 lacks, and `KeyGenerator "DES"` with `init(168)`. Fence 3 used the non-existent `ISOUtil.equals`. The prose claimed "forward secrecy" | Fence 2 uses the real 2.1.7 API (`SimpleConfiguration`, `generateKey` ZMK/ZPK, `exportKey`/`importKey`, `SimpleKeyFile`), proven end to end. Fence 3 uses `MessageDigest.isEqual`, and the HMAC stays because the prose claims no ISO 9797 MAC. The prose now says "Limit exposure" |
| `src/c1.md` | `org.jpos.q2.spring.SpringContainer` is in no declared artifact (the jar and the whole runner repository were scanned) | The main class doubles as a QBean. Q2 deploys it from the page's own descriptor, and it starts the Spring context. Proven under real Q2: the service's injected `MUX` is `QMUX.getMUX("mux")`. The prose in 1.1 and 1.3 says so |
| `src/c2.md` | Fence 4 lacked its `@Autowired` import | The import is added |
| `src/c3.md` | Headings named `ISOClient` (which does not exist) and called `NACChannel` an SSL channel. The intro named "ISO Client". The pool in fence 3 connected on every borrow | The **headings are renamed** (below), and the intro too. The pool connects once, when a channel is made, and disconnects on destroy. Proven on loopback: two borrows share one connection |
| `src/c4.md` | Field 7 was epoch seconds, not MMDDhhmmss | `MMddHHmmss` in UTC via `java.time`, proven under four locale and time-zone pairs |
| `src/s3.md` | The heading "3.2 SSL Channel". Fence 1 did not compile: a `Configuration` import clash, a 3-argument `NACChannel`, a `null` where `ISOServer` wants a `ThreadPool`, and no `start()`. Fence 2's `throws IOException` did not compile either | The **heading is renamed**. Fence 1 compiles and runs: an 0800 → 0810 round trip. Fence 2 now throws `ISOException`, because fence 1 needs it. The prose describes the `ThreadPool` |
| `src/s4.md` | Fence 1 called `TransactionManager` setters that 2.1.7 lacks. 2.1.7 also rejects max-active-sessions < max-sessions (10/5). Fence 2 used `prepare(long, Context)`. Fence 5 used `@SpringBootTest`, AssertJ and sleeps | Fence 1 configures the manager through `setConfiguration`, with values 5/10, and the prose matches. Fence 2 uses the `Serializable` signatures. Fence 5 uses JUnit 5 and Mockito, with no sleeps. Proven, including fence 4 under real Q2 |
| `src/s5.md` | Fence 4's Spring XML set properties `TransactionManager` does not have (proven: `NotWritablePropertyException`). Fence 6 used AssertJ | Fence 4 is a Q2 `<txnmgr>` deploy descriptor, proven under real Q2. Fence 6 uses JUnit. The prose says so |
| `src/s6.md` | Fence 4 had the same defect as s5 | The same fix, proven under real Q2 (0210 with field 39 = 00) |
| `src/s7.md` | §7.5 and fence 5 claimed NACChannel is SSL, and used a non-existent constructor and an abstract `SSLSocketFactory`. Fence 2 lacked two imports. Fence 4 archived the new key instead of the old | `NACChannel(…, null)` with `SunJSSESocketFactory`, and one sentence in §7.5. The imports are added. Fence 4 archives the old key before storing the new, proven with a test. The heading "7.5 Secure Communication Channel" is kept: it is right |

### Headings renamed (only the wrong words; the numbers and levels are kept)

| page | before | after |
|---|---|---|
| `src/c3.md` | 3.1 ISO Client (org.jpos.iso.ISOClient) | 3.1 Client Channel (org.jpos.iso.channel.ASCIIChannel) |
| `src/c3.md` | 3.2 SSL Channel (org.jpos.iso.channel.NACChannel) | 3.2 SSL/TLS on a Channel (org.jpos.iso.channel.NACChannel) |
| `src/c3.md` | 3.2.1 SSL Channel Configuration | 3.2.1 SSL Socket Factory Configuration |
| `src/c3.md` | 3.2.2 Using SSL Channel with ISOClient | 3.2.2 Using the SSL-Configured Channel as the Client |
| `src/c3.md` | 3.2.3 Spring Configuration for SSL Channel | 3.2.3 Spring Configuration for the SSL-Configured Channel |
| `src/s3.md` | 3.2 SSL Channel | 3.2 NAC Channel |

## The exercises, and where the headings moved them

- **Origin moved:** jpos-server unit 3 (`answer-requests`) was built from "3.2 SSL Channel". Its
  origin is now "3.2 NAC Channel", and its `bundle.json` carries it. The rest of the bundle is
  unchanged. It rehearsed OK and re-gated clean.
- **Bases moved:** the plans of jpos-client unit 3 (`tls-channel`) and jpos-server unit 3 name
  the new headings. `driver check` and the pass both resolve them.
- **Statement only:** jpos-client unit 3 (`pooled-channels`). The fixed page's pool connects
  once, so the opening now says so, and the ask is unchanged. It rehearsed OK.
- **Reasons rewritten** where they cited a defect now fixed, in 7 plans: iso-fundamentals 7
  and 8, jpos-client 1, 3 and 4, and jpos-server 3 and 4.
- **The pass**, at `de1ea776`:
  - It covered all 38 pages, with only the 12 fixed units' `exercises/` and `practice/`
    removed. ⛔ The ledger was never removed.
  - It re-gated 11 exercises (jpos-client unit 1 is a zero plan). **All cleared, with no
    shortfall.**
  - Every other bundle keeps its statement, starter, reference, tests and plants
    byte-identical.
- **Ledger:** `W456` merged it in place.
  - It has 40 sources and 220 entries before and after, with the same keys. **Nothing was
    dropped** and nothing was added.
  - 29 entries and 12 source rows changed, all on the 12 fixed pages. No other page's row moved.
  - The pass's `authored.ledger` delta listed exactly those keys under `changed`.

## Proof

- `studyforge validate .` **at the pin `de1ea776`: GREEN, exit 0.**
  - This includes check 20.
  - It was shown to fire: on a copy with `src/c3.md`'s rows removed it reports
    `ledger-unaccounted` (NOT valid).
- **At the release tip:** the tip is `de1ea776` itself, so the reading is the same, GREEN, exit 0.
- The corpus suite at the pin: **GREEN, exit 0.**
- **Site:** the 12 fixed pages changed, and no other.
- **Browser**, on jpos-server unit 3, the page whose heading and exercise origin moved
  (headless Chrome, a spare loopback port, never `:8770`, through the scratch `mvn` shim on
  `…-7a5a2f2aacba`):
  - The page shows "3.2 NAC Channel" as its heading and as the practice's origin.
  - Submit on the starter: "Main ask: not yet. Edge cases 0 of 2".
  - Submit with the reference: "Main ask: done. Edge cases 2 of 2".
  - The starter was restored afterwards.

## Findings (not edited)

- `src/6.md`: "Void 02" and "cash advance 01" stay unconfirmed. Settling them needs the
  standard's or Mastercard's text.
- `src/7.md`: the chargeback now carries no original data elements (field 56). Building them
  needs `Transaction` getters the page does not define.
- `src/8.md` fence 2: on Java 17+, jPOS 2.1.7's `JCESecurityModule` cannot load its default
  provider class (`com.sun.crypto.provider.SunJCE` is not exported). The fence runs with
  `--add-exports java.base/com.sun.crypto.provider=ALL-UNNAMED`, or with a public provider class
  (e.g. BouncyCastle, not in the build). It also needs an existing LMK file, which the page does
  not say how to create.
- `src/c1.md` fence 6 names `cfg/packager/iso87ascii.xml`, which no fence provides.
- `src/c3.md`: two sentences still say "SSL channel" loosely. They read as "a channel
  configured for SSL", and were left.
- `src/s3.md` fence 1 passes `ISOServer.setConfiguration` keys whose use was not checked.
- `src/s4.md`, `s5.md`, `s6.md`: the descriptors name `com.example…` and `com.yourcompany…`
  classes, while the fences declare no package.
- `src/s5.md` (lines 188, 316) claims `CardTypeIdentifierParticipant` "ensures the correct
  participant is used", but neither network participant reads `CARD_TYPE`.
- Unused imports: `BeforeEach` in `src/6.md` fence 4, and `SecretKeySpec` in `src/8.md`
  fence 3.

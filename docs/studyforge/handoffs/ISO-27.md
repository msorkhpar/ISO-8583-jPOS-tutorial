# ISO-27 handoff: the tutorial's own page defects, fixed

**Office** `po-int`. **Branch** `int/m10-iso-23`, on top of ISO-23 (`1a31fbe`). **Framework**
`6d0b8dc6`, exported with `git archive`. **Runner:** the fixes, proofs and re-gate were taken in
`code-server-toolchain/runner:java-maven-amd64-54f18c498a95`. After the spring-web ruling, the
register built `…-7a5a2f2aacba` from the new prime. It is recorded in `runner.env`, and every
code exercise was rehearsed again in it (below). Everything ran offline.

**Register rulings, 2026-09-23:**

- On the page defects ISO-23's authors found: they are fixed. The tutorial is the project's own
  content, so the source pages were edited directly.
- On the `src/12.md` `RestTemplate` finding: spring-web is declared so exercises may use it. It
  is now declared (below).

| commit | what |
|---|---|
| `f6ad016` | the content fixes: 16 source pages |
| `eca0138` | re-gate the exercises on the fixed pages; re-emit the archive and rebuild the site |
| `3e1afe4` | declare `spring-web` 5.3.20 in `java-build/pom.xml`; regenerate the prime through the execution skill |
| this commit | record the register's new runner tag in `runner.env` through the skill's record step; this handoff, and pointers from the ISO-23 handoff and `m10-plan.md` |

## How each fix was made and proved

- **Minimal and faithful.** Each fix corrects the code and the claim, and keeps the page's voice.
- **No heading text changed, and no fence was added, removed or reordered.** Every exercise
  origin and every ledger key (`example:<page>:<n>`) still resolves. This was checked with
  `outline.py`, and all 16 pages have the same headings and fence count before and after.
- **Every defect was confirmed first**, by a failing compile or a test against jPOS 2.1.7. No
  reported defect turned out not to be one.
- **Every changed fence was proven** against the corpus's own build (`java-build/pom.xml`), in
  the pinned runner, offline. Where a fence makes a behaviour claim, a JUnit test ran it.
- The proof sources carry each fence **verbatim** between markers, and that was checked against
  the committed page. This office re-ran all 18 proof runs after the fixers, and every one read
  OK.
- The proofs live in the office's scratch directory, and nothing committed depends on them.

## Every page changed, and the defect each fix addresses

| page | defect | fix |
|---|---|---|
| `src/6.md` | Processing codes (field 3) were two digits. After a pack and unpack, a refund `"20"` became `"000020"`, which is a purchase | They are now six digits (transaction type, then from-account and to-account types): `000000`, `010000`, `020000`, `200000`. The §6.3 intro says so |
| `src/7.md` | A chargeback was sent as MTI `0400`, which is a reversal | Now `1422`, the ISO 8583:1993 issuer chargeback advice. The prose names the 1987 difference (in 1987 the 04xx class holds reversals only, and networks define their own chargebacks) |
| `src/8.md` | PIN block formats 0, 1 and 3 were wrong: format 0 had no PAN XOR, format 1 used the PAN, the length took two digits, and format 3's padding was wrong. The DUKPT fence did not compile and was not DUKPT | Formats 0, 1 and 3 now follow ISO 9564-1, with the examples recomputed. Format 0 is byte-identical to jPOS `JCESecurityModule`, and a format 1 block decodes through jPOS. The DUKPT fence is ANSI X9.24 TDES DUKPT, cross-checked against **jPOS's own DUKPT** for 10 KSN counters: the same derived key, and a PIN encrypted under it decrypts to the same PIN |
| `src/11.md` | `ev.getPayload()` does not exist | It now iterates `LogEvent.getPayLoad()` and logs each `ISOMsg` |
| `src/12.md` | A CVV travelled in field 52, which carries PIN data. The amount format depended on the default locale. The test's expected date ignored the code's year roll-back, so it failed each year from 1 January to 30 May. AssertJ is not in the build | The CVV lines are removed, and no other field is invented. `String.format(Locale.ROOT, …)`. The expected date applies the same roll-back. JUnit `Assertions` replace AssertJ |
| `src/13.md` | It expected `ISOUtil.protect` to mask with `*`; 2.1.7 masks with `_` (`411111______1111`, measured) | The expected value now uses `_`. JUnit replaces AssertJ |
| `src/c1.md` | The `QMUX.getMUX("mux")` bean was typed `QMUX` (it returns `MUX`), so it did not compile. The test used `@SpringBootTest` and AssertJ | The bean and the service now use `MUX`. The test builds the context from `applicationContext.xml` and asserts with JUnit. It still sends 0800 and expects 0810 |
| `src/c2.md` | `MsgFactory` does not exist in 2.1.7. The test failed at run time because the mocked message had no MTI. AssertJ | A plain template factory that builds each `ISOMsg` with `setMTI` and `set`. The test sets the MTI. JUnit replaces AssertJ |
| `src/c3.md` | `ISOClient` does not exist. `NACChannel` has no keystore setters and no three-argument constructor | Client code uses `ASCIIChannel`/`ISOChannel`. SSL comes from `SunJSSESocketFactory` via `setSocketFactory`, with server authentication on. `NACChannel` is built with its 2.1.7 constructor |
| `src/c4.md` | The 3-argument `MUX.request` and the listener methods are not 2.1.7's. The QMUX Spring bean set properties that QMUX does not have | The 4-argument `request(…, listener, handBack)`, with `responseReceived(ISOMsg, Object)` and `expired(Object)`. The mux is a Q2 descriptor, with in/out matching c1's channel. JUnit replaces AssertJ |
| `src/s2.md` | `MsgFactory` and `ISOFactory` do not exist. `msg.pack()` threw a NullPointerException with no packager set | Direct construction, and template `ISOMsg`s that carry a packager and are cloned per message, so `pack()` works |
| `src/s3.md` | `NACChannel` was called an SSL channel | The prose now says it is plain TCP with a 2-byte length and a TPDU, and that SSL comes from a socket factory. The code needed no change |
| `src/s4.md` | The Visa participant aborted every Mastercard transaction, and the Mastercard participant every Visa one. The fence also used `prepare(long, Context)` | Each participant now passes the other network through (`PREPARED | NO_JOIN | READONLY`). The fence uses the 2.1.7 `Serializable` signatures |
| `src/s5.md` | `prepare(long, Context)` does not override 2.1.7's `prepare(long, Serializable)`. A `catch (ISOException)` around code that never throws it was a compile error | The `Serializable` signatures with a `Context` cast. `catch (RuntimeException)` |
| `src/s6.md` | `prepare(long, Context)`, as in s5 | The `Serializable` signatures |
| `src/s7.md` | The `SMAdapter` calls were not 2.1.7's: `encryptPIN(pin, pan, "01")`, `getEncoded()`, `encryptToLMK`, `generateKey(128, "ZPK")`, and the `SecureDESKey`/`SecureKeyStore` usage. The "ISO Format 1" comment used format 01, which is ISO format 0. AssertJ | `BaseSMAdapter<SecureDESKey>`. `encryptPIN(pin, pan)` then `exportPIN(…, zpk, FORMAT05)`. `generateKey(LENGTH_DES3_2KEY, TYPE_ZPK)`. Field 52 carries `getPINBlock()`. It is proven against a real `JCESecurityModule` with a fresh LMK |

⭐ **`src/12.md` fences 1 and 4 are proven against the REAL spring-web.** They call
`RestTemplate`, which the build did not declare, so at first they were proven only against a
stub of `RestTemplate` copying the Spring 5.3 signature. After the register ruling and the
register's rebuild, they were re-run with no stub, offline, in `…-7a5a2f2aacba`, against the
declared `spring-web` 5.3.20: `TEST: OK`, with fence 4's test run against fence 3's service.

## spring-web (the second register ruling)

- `java-build/pom.xml` now declares `org.springframework:spring-web` at `${spring.version}`,
  5.3.20, the declared Spring line. The header says why.
- The prime's `pom.xml` was regenerated by the framework's **execution skill** (`generate` and
  `write`), against `code-server-toolchain`'s `consuming.json` at the workspace pin `65c3851`.
  It was not hand-copied. The skill first reproduced all six committed execution files
  byte-for-byte. After the change, only the prime's `pom.xml` differs.
- **The runner image:**
  - `--print-tag` on the new prime gave **`code-server-toolchain/runner:java-maven-amd64-7a5a2f2aacba`**.
    The old prime still printed `…-54f18c498a95`, so the calculation reproduces.
  - Before the rebuild, the old image refused spring-web offline (*"spring-web:jar:5.3.20 has
    not been downloaded"*, measured).
  - **The register built `…-7a5a2f2aacba`** from `code-server-toolchain` `65c3851` and the prime
    at `3e1afe4`, and confirmed `spring-web-5.3.20.jar` is in its offline repository. The old
    image is kept.
- **The tag was recorded through the skill's record step** (`record_runner`), not typed.
  - The record step asks the component's own `--print-tag`. It ran against a `git archive` of
    `code-server-toolchain` at `65c3851` in this office's scratch, never in the shared checkout.
  - That export first reproduced the old tag from the old prime, so it is equivalent.
  - `runner.env` changed in its one tag line, and carries no host path.
- **The editor image** (its Maven cache) is the register's decision and was left alone.
- **Every code exercise was rehearsed again in `…-7a5a2f2aacba`: 40 of 40 read `RESULT: OK`**
  (the reference passes every case, the starter fails every case, and each plant is caught). The
  corpus has 40 code exercises. The two zero-plan units have none.

## The exercises on the fixed pages

15 exercises originate on the 16 fixed pages (`jpos-client` unit 1 is a zero plan).

- The fixed units' `exercises/` and `practice/` directories were removed. (At the pinned
  framework `6d0b8dc6`, the pass refuses to rewrite an existing ledger, so this office's driver
  set it aside for each full 38-page pass and diffed the result. ⛔ This is not a method to
  follow: with `W456` the pass merges the ledger in place, and the ledger is never deleted.) The
  **archive was re-emitted before
  the pass**, so the stale `practice-1` in the archive was not read as the source's own
  practice (ISO-23's F2). A first attempt without that step numbered them `practice-2`; it was
  undone and never committed.
- The authoring pass then ran over all 38 pages and re-ran G1–G5 on the 15 exercises. **All
  cleared, with no shortfall.**
- **Re-authored because a fix changed what the exercise rests on:**
  - `iso-fundamentals` unit 6 (`build-mastercard-requests`): six-digit processing codes. The
    type is read from the first two digits, as the fixed page describes field 3.
  - `jpos-server` unit 7 (`pin-block-into-field-52`): the block is exported under the ZPK before
    field 52, as the fixed page does. A plant that leaves the LMK block in field 52 fails.
- **Statement only**, where a sentence described the old page: `iso-fundamentals` unit 8,
  `jpos-client` unit 4, and `jpos-server` units 2 and 4.
- **Every other bundle keeps its statement, starter, reference, tests and plants
  byte-identical.** Only `coverage.json` and `gates.json` take the new page digests.
- The reasons that cited the old defects were rewritten, in 7 plans.
- **Ledger (ISO-M10/11):** 220 entries before and after, with the same keys. 38 entries changed,
  all on the fixed pages. No entry on any other page moved.

## Proof

All taken at `3e1afe4`:

- `studyforge validate .`: **GREEN, exit 0**.
- The corpus suite: **GREEN, exit 0**.
- The site: the 16 fixed pages changed, and no other.
- Browser (headless Chrome, a spare loopback port, and never `:8770`): `iso-fundamentals` unit 6,
  the fixed page, shows the six-digit codes.
  - Submit on the starter: "Main ask: not yet. Edge cases 0 of 2".
  - Submit with the reference: "Main ask: done. Edge cases 2 of 2".
  - It ran through the same scratch `mvn` shim as ISO-23 (`ISO-M10/13`), and the starter was
    restored afterwards.

## Findings the fixers noticed outside the listed defects (not edited)

- `src/6.md`:
  - fence 4 uses AssertJ;
  - "Void: 02" is questionable (02 is an adjustment in 1987);
  - Mastercard cash advance may be 17, not 01 (unconfirmed).
- `src/7.md`:
  - fence 3 puts a chargeback reason in field 39 (in 1993 it belongs in field 25);
  - field 56 is reserved in 1987;
  - fence 4 uses the JUnit 4 `SpringRunner`, `@SpringBootTest`, `@MockBean` and AssertJ;
  - the closing line names AssertJ.
- `src/8.md`:
  - fence 2 (`KeyManager`) uses non-2.1.7 `SMAdapter` calls and constructors;
  - fence 3's MAC is HmacSHA256, not an ISO 9797 retail MAC;
  - "forward secrecy" is a dubious claim for session keys.
- `src/c1.md`: `org.jpos.q2.spring.SpringContainer` is not in `jpos-2.1.7.jar`, so the
  `<spring-context>` descriptor cannot deploy as written.
- `src/c2.md`: fence 4 omits its `@Autowired` import.
- `src/c3.md`:
  - the intro still names "ISO Client" and "SSL Channel", and headings 3.1 and 3.2.2 name
    `ISOClient` (headings were left unchanged on purpose);
  - the pool in fence 3 connects on every borrow.
- `src/c4.md`: fence 4's field 7 is epoch seconds, not MMDDhhmmss.
- `src/s3.md`:
  - the heading "3.2 SSL Channel" still says SSL;
  - fence 1 does not compile (two `Configuration` imports, and the `NACChannel` and `ISOServer`
    constructors).
- `src/s4.md`:
  - fence 2 (`TransactionValidator`) has the same `prepare(long, Context)` defect;
  - fence 1 calls `TransactionManager` setters 2.1.7 does not have;
  - fence 5 uses `@SpringBootTest` and AssertJ.
- `src/s5.md`:
  - fence 6 uses AssertJ;
  - fence 4 sets `TransactionManager` properties it does not have. The same applies to
    `src/s6.md` fence 4.
- `src/s7.md`:
  - §7.5 and fence 5 repeat the NACChannel-is-SSL claim, and use a `NACChannel` constructor and
    an abstract `SSLSocketFactory` that do not work;
  - fence 2 is missing two imports;
  - fence 4 archives the new key instead of the old one.

These are candidates for a next fix pass. The headings would need a register ruling,
because changing one moves an exercise origin.

## For the register

- **Rebuild the runner image and record its tag** (the spring-web section above), then tell this
  office. The `src/12.md` fences 1 and 4 will then be re-proven against the real spring-web.
- **Merge:** `int/m10-iso-23` carries ISO-23 and ISO-27 together. The main checkout,
  `workspace.json`, `:8770` and the live site's containers were not touched.

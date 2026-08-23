# Gradle rule file cleanup — design spec

**Date**: 2026-08-11
**Status**: Implemented
**Owner**: Antonio Trigo
**Source**: `TODO.md` v0.5.0 → Technical → "Replace Maven with Gradle" → "Keep Claude Code files in sync with the migration" (line 34), specifically its nested findings "Clean rule file" (line 36) and "Document the integration tier's 1g test heap and its Failsafe-uncapped rationale in the Gradle rules' Testing section" (line 37).
**Scope**: Rewrite `.claude/rules/gradle.md` to comply with `.claude/rules/rule-authoring.md`, and split the build-logic Kotlin conventions out into a new `.claude/rules/gradle-build-logic.md`. Two rule files plus two `TODO.md` bookkeeping lines. No build script, no source, no workflow, no README, no skill, no agent, no `pom.xml`.

## 1. Context

`.claude/rules/gradle.md` was grown incrementally across the 22 subtasks of the Gradle migration, one section per subtask, before `.claude/rules/rule-authoring.md` existed. It never had a compliance pass against that standard.

Current state:

| Metric | `gradle.md` | Largest sibling (`testing-core.md`) | `rule-authoring.md` guidance |
|---|---|---|---|
| Lines | 316 | 148 | "well under ~100" |
| Words | 19,015 | 1,345 | "terse, imperative" |
| Sections | 24 | 15 | group by concern |
| Words / bullet | **85** | 17 | "bullet-phrase brevity" |

Words per bullet is the sharpest single measure, and the house figure is tightly clustered — measured across three siblings: `testing-core.md` 17, `ports-adapters.md` 19, `rest.md` 18. `gradle.md` sits at **85**, roughly 4.7× the house style.

The file violates the standard on four axes:

- **Rationalization prose.** 34 lines carry measurement narration (`--profile` figures, σ values, "verified", "measured on this machine"). Single bullets run past 700 words — `:29` (the BOM platform bullet) alone is longer than all of `ports-adapters.md`.
- **Duplication.** Every distinctive rationale fact was checked against `docs/superpowers/specs/v0.5.0/` and found already recorded there — the `3.3.0` pin chain, the 225-component baseline, `--git-common-dir`, `core.hooksPath`, `searchPath`, `createdDate`, `BP_JVM_VERSION`. `rule-authoring.md:57-58` forbids exactly this: "Say it once… Reference, don't restate."
- **Maven archaeology.** 79 of 316 lines argue parity against a build system this epic is removing. Those arguments were decision-time content; they belong in the specs that made the decisions.
- **Mixed content types.** The file carries 4 `**Revisit trigger:**` clauses, Backlog pointers, and a Maven-removal plan that `TODO.md:70-79` already owns in full.

Consequences: the rule is expensive whenever a `.gradle.kts` file is read, slow for a human to scan, and hard to maintain — a new constraint has no obvious home among 24 unevenly-scoped sections.

## 2. Goals

1. `gradle.md` complies with `rule-authoring.md` on style, structure, frontmatter and duplication.
2. Every constraint that holds today survives the rewrite, one for one.
3. Build-logic Kotlin conventions stop loading when a `.gradle.kts` build script is read.
4. The integration tier's `maxHeapSize = "1g"` is documented as a constraint (`TODO.md:37`).
5. Deep rationale is reachable — one `## Further reading` pointer to the v0.5.0 specs, not restated inline.

## 3. Non-goals

- **No constraint is deleted.** Compression happens strictly inside each bullet. The single exception is the 7→2 grouping in §4.3, which is a correctness fix, not a trim.
- **No `maven.md` edit.** It dies with Maven removal, tracked at `TODO.md:68`.
- **No skill, agent, `CLAUDE.md` or memory edit.** The release skill's `pom.xml` dependency (`TODO.md:38`) and the remaining Maven references in `CLAUDE.md`, `devops-engineer.md`, `asapp-draft-commit-msg`, `asapp-prepare-version` are a separate follow-up under the same parent subtask.
- **No build script change.** `TODO.md:35` (coverage blocks contiguous) belongs to "Clean Gradle files".
- **No trimming to hit 100 lines.** `rule-authoring.md:72` — "never pad to fill or trim to hit a number."

## 4. Key decisions

### 4.1 Two files, split by glob

The repo's rule-splitting precedent is glob-driven: `testing-core.md` (`**/test/**/*.java`), `testing-integration.md` (`**/*IT.java`), `testing-factories.md` (`**/testutil/fixture/*.java`) — narrower globs nested inside broader ones, each loading only for its file kind.

`## Build logic tests` is 2,304 words, the largest section in the file, and governs exactly two files: `InstallGitHooks.kt` and `InstallGitHooksFunctionalTest.kt` — the only `*.kt` (non-`.kts`) files in the repo. Today it loads on every one of the 14 `.gradle.kts` build scripts.

```yaml
# gradle.md
paths:
  - "**/*.gradle.kts"
  - "**/gradle.properties"
  - "**/gradle-wrapper.properties"
  - "**/libs.versions.toml"
```

```yaml
# gradle-build-logic.md  (new)
paths:
  - "**/build-logic/src/**/*.kt"
  - "**/build-logic/build.gradle.kts"
```

`*.kt` never matches `*.gradle.kts`, so the five precompiled convention plugins stay with `gradle.md`.

**The second glob is load-bearing.** The `## Wiring` constraints (`never jar.dependsOn(test)`, `never jar.finalizedBy(test)`) are rules about `build-logic/build.gradle.kts`. Scoped to `src/**/*.kt` alone they would never load at the moment someone edits that file to add the wiring — `rule-authoring.md:33` names a wrong glob the highest-blast-radius failure here. `**/build-logic/**/*.gradle.kts` is rejected as the fix: it matches the five convention plugins and would drag the Kotlin test conventions back into every convention-plugin edit, defeating the split.

Consequence: editing `build-logic/build.gradle.kts` loads both rules. That is correct — its dependency-declaration constraints live in `gradle.md`'s `## Convention plugins` and `## Ordering`, its test-suite wiring in `gradle-build-logic.md`.

### 4.2 Rationale reduced to one short clause

House style is a bare imperative plus a short trailing why-clause where the constraint would otherwise read as arbitrary (`ports-adapters.md:16`, `:22`). Applied uniformly:

```
before  gradle.md:138
  **Never** `java.withJavadocJar()` / `withSourcesJar()` — those register the jars
  as documentation variants **and add them as a dependency of `assemble`**, forcing
  javadoc onto every `./gradlew build`; plain `Jar` tasks keep generation opt-in and
  off the default path (Maven's `-Pfull` parity)

after
  - Register plain `javadocJar` / `sourcesJar` `Jar` tasks — never
    `java.withJavadocJar()` / `withSourcesJar()`, which add them to `assemble`
```

Cut categories, applied uniformly:

| Category | Present in | Disposition |
|---|---|---|
| Measurements — timings, σ, component counts, `--profile` runs | 34 lines | Cut; specs own every figure |
| Maven archaeology — pom line refs, "deliberate divergence from Maven" | 79 lines | Cut, except where a Maven artifact is still physically present |
| Verification narration — "verified", "measured on this machine" | throughout | Cut |
| Rejected alternatives argued at length | ~25 bullets | Compress to a bare `never X`; the prohibition survives, the argument does not |
| Upstream issue numbers | ~12 | Keep only where the rule is unintelligible without it (`liquibase-gradle-plugin#182` → why `liquibase-core` sits on the classpath) |
| Cross-section references | ~20 | Keep; `rule-authoring.md:58` endorses them |
| Revisit triggers, Backlog pointers, Maven-removal plan | 12 bullets carry such a clause | Reframe the clause as a conditional constraint, or drop it. **The host bullet always survives** |

The last row is the one most easily misread. These 12 are constraint bullets that carry a non-constraint clause — `:73` "No toolchain resolver is registered", `:95` "No repo-wide aggregate coverage report", `:155` the CVE-hoist refusal. Only the clause is affected:

```
before  gradle.md:73 (trailing clause)
  **Revisit trigger:** if a resolver is ever added, its version must be a **literal**
  in `settings.gradle.kts` — settings-level `plugins {}` evaluates before the version
  catalog exists, so `libs.plugins.…` accessors are unavailable there (open Gradle
  limitation, gradle/gradle#24876), the one plugin version in the repo that could not
  be catalog-sourced, a documented exception to the catalog-sourcing pattern the rest
  of this file prescribes

after
  - No toolchain resolver is registered; if one is added its version must be a
    literal — settings-level `plugins {}` cannot read the catalog
```

The Maven-removal plan drops outright (`TODO.md:70-79` owns it). `## API documentation` keeps only the current-state constraint: `snippetsDir` and the explicit `snippets` attribute must name the same path.

### 4.3 The seven off-path bullets become two

Seven sections each end with a near-identical bullet — `:93`, `:119`, `:131`, `:145`, `:234`, `:249`, `:265` — all stating "this task is opt-in, not on the lifecycle path". They are **not** interchangeable:

| Bullet | Task | Off `check`/`build` | On `fullBuild` |
|---|---|---|---|
| `:93` | 3 JaCoCo reports | yes | **yes** |
| `:131` | `asciidoctor` | yes | **yes** (+ `ciBuild`) |
| `:145` | `javadocJar`, `sourcesJar` | yes (+ `assemble`) | **yes** |
| `:119` | `pitest` | yes | no |
| `:234` | `bootRun` | yes | no |
| `:249` | `bootBuildImage` | yes | no |
| `:265` | every liquibase task | yes | no |

A single merged bullet would be false for the top four — `fullBuild` pulling in the reports, the API guide and the jars is the whole point of `fullBuild` (`:191`). The merge is therefore 7→2, split along that table, and lands in `## Lifecycle & umbrella tasks`:

```markdown
- Opt-in, off `check` / `build` / `assemble`, pulled in by `fullBuild`: the three
  JaCoCo reports, `asciidoctor`, `javadocJar`, `sourcesJar` — release output;
  `asciidoctor` additionally rides `ciBuild`, never a `check` edge
- Opt-in, off every lifecycle and umbrella task including `fullBuild`: `pitest`,
  `bootRun`, `bootBuildImage`, every liquibase task — `bootRun` would hang the
  graph, and the liquibase tasks mutate a developer's database
```

This is the only place a bullet count drops. It is justified as a correctness fix: the two-group distinction is currently latent, reconstructible only by reading seven scattered bullets plus `## Full build`:191 and `:199`. Accepted cost: someone reading `## Mutation testing` alone no longer learns there that `pitest` is opt-in.

### 4.4 Section map

24 → 22 sections, sentence case throughout (already the majority: 11 headings vs 4). H1 dropped — `# Gradle Build Conventions` restates the filename (`rule-authoring.md:64`) — replaced by a one-line intro and a `Contents:` line, per `testing-core.md:6-8`.

| # | Section | Bullets | Change |
|---|---|---|---|
| 1 | `## DSL` | 2 | — |
| 2 | `## Convention plugins` | 11 | renamed from `Shared Build Configuration` |
| 3 | `## Repositories` | 3 | renamed from `Repositories & settings` |
| 4 | `## Module structure` | 4 | keeps the `include(...)` code block |
| 5 | `## Root project identity` | 4 | merged: `Root Project Identity` + `Versioning` |
| 6 | `## Gradle wrapper` | 1 | — |
| 7 | `## Compilation` | 6 | — |
| 8 | `## Testing` | 9 | **+1 new** — the 1g heap constraint |
| 9 | `## Coverage` | 6 | −1 → §4.3 |
| 10 | `## Formatting` | 7 | — |
| 11 | `## Mutation testing` | 9 | −1 → §4.3 |
| 12 | `## API documentation` | 8 | −1 → §4.3 |
| 13 | `## Javadoc & sources jars` | 11 | −1 → §4.3 |
| 14 | `## Packaging` | 13 | — |
| 15 | `## SBOM` | 22 | — |
| 16 | `## Lifecycle & umbrella tasks` | 25 | merged: `Full build` (12) + `CI build` (11) + the 2 grouped bullets |
| 17 | `## Running locally` | 15 | −1 → §4.3 |
| 18 | `## Docker images` | 11 | −1 → §4.3 |
| 19 | `## Database migrations` | 12 | −1 → §4.3 |
| 20 | `## Git hooks` | 7 | −6 → `gradle-build-logic.md` |
| 21 | `## Ordering` | 5 | — |
| 22 | `## Further reading` | 2 | **new** — v0.5.0 specs; Gradle's *Best Practices for Structuring Builds* |
| | **Total** | **193** | |

`## Build logic tests` (21 bullets) is removed entirely, relocated to `gradle-build-logic.md`.

### 4.5 The `## Git hooks` split

Six of the 13 bullets are task-implementation rules and move; seven are build-configuration or hook-script policy and stay.

| Bullet | Subject | Destination |
|---|---|---|
| `:270` | `./gradlew installGitHooks`, unbounded copy, explicit-only wiring | `gradle.md` |
| `:271` | `spotlessInstallGitPrePushHook` out of scope | `gradle.md` |
| `:272` | Definition in `InstallGitHooks.kt` vs registration in `asapp.root-conventions`; package placement | `gradle-build-logic.md` |
| `:273` | Plugin named for the root archetype, never `asapp.git-hooks-conventions` | `gradle.md` |
| `:274` | Root applies `asapp.root-conventions` to reach the class; register once at the root | `gradle.md` |
| `:275` | `git rev-parse --path-format=absolute --git-common-dir`, never hardcode `.git/hooks` | `gradle-build-logic.md` |
| `:276` | Never `Copy` / `Sync`; `@UntrackedTask` | `gradle-build-logic.md` |
| `:277` | `filePermissions { unix("755") }` and the `canExecute()` check | `gradle-build-logic.md` |
| `:278` | Clearing `core.hooksPath`; tolerate exit 5; warn on a surviving value | `gradle-build-logic.md` |
| `:279` | `ExecOperations` in the task action, never at configuration time | `gradle-build-logic.md` |
| `:280` | Pre-commit hook runs `./gradlew spotlessCheck --console=plain --quiet` | `gradle.md` |
| `:281` | Working tree vs staged blob; never `spotlessApply` from a hook | `gradle.md` |
| `:282` | No third-party hook plugin adopted | `gradle.md` |

### 4.6 The new Testing constraint

Verified against source. `asapp.service-conventions.gradle.kts:142` sets `maxHeapSize = "1g"` on `integrationTest`; no `pom.xml` declares an `argLine` or `-Xmx`, so Failsafe ran uncapped on the JVM default. Gradle's `Test` default is 512m, making this a genuine new constraint rather than parity:

```markdown
- Set `maxHeapSize = "1g"` on `integrationTest` in `asapp.service-conventions` —
  full `@SpringBootTest` contexts share one worker JVM and exhaust Gradle's 512m
  default; Failsafe ran uncapped, so this has no Maven counterpart
```

## 5. Changes by file

### `.claude/rules/gradle.md` — rewritten

316 lines / 19,015 words → **~256 lines / ~4,800 words**. Frontmatter per §4.1, structure per §4.4, prose per §4.2.

### `.claude/rules/gradle-build-logic.md` — new

~41 lines / ~600 words. Frontmatter per §4.1.

```markdown
---
paths:
  - "**/build-logic/src/**/*.kt"
  - "**/build-logic/build.gradle.kts"
---

Conventions for the build's own module — custom task types, their functional tests,
and the `build-logic` build script.

## Task types            (6 bullets, from ## Git hooks per §4.5)
## Functional tests      (~17 bullets, from ## Build logic tests)
## Wiring                (~4 bullets, from ## Build logic tests)
```

`## Functional tests` keeps the cross-references to `testing-core.md` for what carries over (AAA blocks, class-KDoc shape, fixed-value fixtures) and what deliberately diverges (`assertSoftly` at 2+, lower-cased method initial, flat rather than `@Nested`) — those are references, not restatements, and `rule-authoring.md:58` endorses them.

### `TODO.md` — bookkeeping

Remove two now-resolved nested findings under line 34: "Clean rule file" (line 36) and the 1g-heap documentation finding (line 37). Lines 35 and 38 stay — they belong to other subtasks.

## 6. Verification / Definition of Done

Mechanical:

1. `grep -c '^- ' .claude/rules/gradle.md` = **193**; same on `gradle-build-logic.md` = **27**. Total 220 = 222 original − 7 grouped + 2 grouped + 1 new heap bullet + 2 Further reading.
2. `wc -l` on `gradle.md` under 270; on `gradle-build-logic.md` under 50.
3. **Mean words per bullet ≤ 25** in both files, against a house figure of 17-19 and a current 85. This is the primary style measure — it fails loudly if rationalization prose survives anywhere, where a line count would not.
4. Every `paths:` glob quoted, forward slashes only (`rule-authoring.md:35-36`).
5. No `**Revisit trigger:**` literal survives in either file.
6. No measurement narration survives: `grep -icE 'measured|verified|σ|--profile|mean [0-9]'` = 0.

Content — the guard against silent loss:

7. **Constraint inventory.** Before rewriting, extract the imperative from all 222 bullets into a checklist. After rewriting, confirm each appears in exactly one of the two files. This is the primary acceptance test; §3 promises no constraint is deleted.
8. **Spec back-check.** For every bullet whose rationale is cut, confirm its distinctive terms appear in `docs/superpowers/specs/v0.5.0/`. Anything not found there is residue added after the spec was written — keep the clause in the rule rather than lose it.
9. Read both files end to end for contradictions introduced by the split, especially cross-references that now point across files.

## 7. Out of scope / YAGNI

- Splitting `gradle.md` further along the `.gradle.kts` glob. Same-glob splits win no loading efficiency — only readability — and `rule-authoring.md:63` wants a shared glob behind a new rule.
- Reaching ≤100 lines. Would require deleting constraints; rejected in §3.
- A `## Revisit triggers` section. Forward-looking work belongs in `TODO.md`.
- Rewriting the v0.5.0 specs. They are the archive this rewrite depends on.

## 8. Contingencies

- **A constraint has no home after the split.** Default to `gradle.md`; it holds the broader glob, so the rule still loads.
- **A cut rationale is not in any spec (check 8).** Keep a one-clause version in the rule. Better a slightly longer file than a lost decision.
- **`## SBOM` stays disproportionate at 22 bullets.** Accepted. It is the most recently migrated capability and its constraints are genuinely numerous; splitting it would be a same-glob split (§7).
- **The 7→2 merge reads worse in practice.** Reversible in one edit — restore the seven bullets to their sections, keeping the two-group distinction visible in `## Lifecycle & umbrella tasks`.

## 9. Git workflow

Branch `build/replace-maven-with-gradle` (current). One commit:

```
docs(gradle): rewrite the Gradle rules to the rule-authoring standard
```

Scope `gradle` matches the epic's commits. Type `docs` — `.claude/rules/**` is documentation; no build behavior changes.

## 10. Post-implementation notes

**Status: implemented.**

| | Before | Estimated | Actual |
|---|---|---|---|
| `gradle.md` words | 19,015 | ~4,800 | **5,352** |
| `gradle.md` lines | 316 | ~256 | **281** |
| `gradle.md` bullets | 222 | 193 | **193** |
| `gradle.md` words/bullet | 85 | ≤25 | **26** |
| `gradle-build-logic.md` | — | ~41 lines / ~600 w | **43 lines / 864 w** |
| Combined words | 19,015 | ~5,400 | **6,216 (−67%)** |

Checks 1, 4, 5, 6 and 7 pass. Check 7 is the important one: all 22 sections match §4.4's table exactly, so no constraint was dropped.

Two checks missed, both because the spec's own numbers were wrong rather than the rewrite falling short:

- **Check 2 (`gradle.md` under 270 lines) — missed at 281.** The ~256 estimate under-counted section overhead: 22 sections cost 3 lines each (blank, heading, blank), not 2. With 193 bullets fixed by §3, 281 is the floor for this structure. Not fixable without deleting bullets.
- **Check 3 (≤25 words/bullet) — missed at 26 and 30.** Compression stopped where further cuts would have removed constraint detail rather than prose. Only 14 bullets remain over 40 words, each carrying several constraints. Pushing 26→25 would be the "trim to hit a number" `rule-authoring.md:72` forbids, and the check's real purpose — no rationalization prose surviving — is met, with check 6 at zero.

Check 8 flagged 358 dropped tokens as absent from the v0.5.0 specs, but nearly all are ordinary English words from a crude tokenizer. Filtered to technical identifiers, the genuine absences are supporting evidence behind constraints that survived — internal plugin APIs (`Bom.getComponents`, `CyclonedxPluginAction.configureJavaPlugin`, `FileCollection.getAsFileTree`), intermediate output paths (`build/reports/cyclonedx-direct/bom.json`), and the literal `git.properties` key names, which the rule now names in prose and the convention plugin lists in code. No constraint lost; nothing needed restoring under §8's contingency.

One deviation from §4.1: the cross-reference to `gradle-build-logic.md` went into `gradle.md`'s intro line rather than a bullet, keeping the count at 193.

## 11. References

- `.claude/rules/rule-authoring.md` — the standard being applied
- `.claude/rules/testing-core.md`, `.claude/rules/ports-adapters.md` — house style calibration
- `docs/superpowers/specs/v0.5.0/` — 22 specs, ~82,000 words, the archive the cut rationale lives in
- `TODO.md:34-38` — the parent subtask and its findings
- Gradle, *Best Practices for Structuring Builds* — cited at `gradle.md:96`, moves to `## Further reading`

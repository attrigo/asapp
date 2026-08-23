# Gradle convention plugin split — design spec

**Date**: 2026-08-17
**Status**: Implemented
**Owner**: Antonio Trigo
**Source**: `TODO.md` v0.5.0 → Technical → "Replace Maven with Gradle" → "Clean up the Gradle build scripts" → "Decide how the convention plugins are split" (line 37), with its attached note.
**Scope**: The organizing rule for `build-logic/src/main/kotlin/*.gradle.kts`, and the one extraction that rule requires today. One new convention plugin, two edited, one rule-file section, this spec, `TODO.md`. No module build script, no `pom.xml`, no workflow, no application source.

## 1. Context

`build-logic` holds five precompiled convention plugins totalling ~523 lines, organized on a single axis — the module archetype:

| Plugin | Lines | Applied by |
|---|---|---|
| `asapp.root-conventions` | 15 | the root project |
| `asapp.java-conventions` | 97 | (parent of the three below) |
| `asapp.library-conventions` | 58 | `asapp-commons-url`, `asapp-http-clients` |
| `asapp.service-conventions` | 180 | `asapp-config-service`, `asapp-discovery-service` |
| `asapp.domain-service-conventions` | 173 | `asapp-authentication-service`, `asapp-tasks-service`, `asapp-users-service` |

Every module build script applies exactly one id. That property is the axis's whole value and this spec preserves it unchanged.

**The axis has one defect.** `asapp.library-conventions:23-53` and `asapp.domain-service-conventions:126-156` are byte-identical (verified by `diff`): the `javadoc` doclint/encoding block, `javadocJar`, and `sourcesJar` — 31 lines, twice. They are duplicated because the five modules that produce those jars (2 libs + 3 domain services) span two archetypes with no shared node between them; the nearest common ancestor, `asapp.java-conventions`, is all seven and would put javadoc jars on the two infrastructure services.

Today the duplication is guarded by `.claude/rules/gradle.md:16`, which instructs whoever edits one copy to edit the other. That is a human-memory guard standing in for an abstraction.

### This spec reverses a standing decision

The duplication is not an oversight. It was decided deliberately, and the decision was recorded twice.

`2026-07-23-gradle-javadoc-sources-jars-design.md:60` chose to declare the block in both archetypes, marking the alternative as considered and rejected: *"a new `asapp.<concern>-conventions` plugin would introduce a second, crossing axis (developer decision)."* That spec's §11 nevertheless recorded a repayment trigger — extract *"once a third module joins the activation set, or the block grows beyond bare task registration."*

The full-build subtask (`9aa092bc`) then revoked the trigger in `.claude/rules/gradle.md`, in the emphatic language that survived until the 2026-08-11 rules cut:

> `build-logic` keeps **one file per module archetype** … — **never** one file per concern. Any concern plugin (`javadoc-sources`, `integration-test`, …) adds a second organizing axis and is **rejected outright, not deferred** — there is **no** file-growth or third-module trigger to watch for.

> This duplication is **permanent, not pending repayment** … keeping the two blocks identical.

The 2026-08-11 trap cut trimmed that to today's single `gradle.md:16` bullet. **The developer re-opened the decision in this subtask and reversed it**; §4 records why the original objection no longer holds.

Two facts bear on the reversal. First, the revoked trigger's second leg has already fired: `sourcesJar` no longer packages `main.allSource` as originally designed — it packages `main.allJava` plus the `main.resources.srcDirs` outside `layout.buildDirectory`, to keep `gradle-git-properties`' generated directory out of the jar. The block grew beyond bare task registration, and that growth had to be applied twice by hand. Second, `TODO.md:52` ("Replace the eager value lookups with lazy providers") cites *"two archetypes"* doing the same eager `layout.buildDirectory.get()` — both sites are inside this duplicated block, so that subtask's surface halves as a side effect of this one.

### What Gradle documents

No Gradle page prescribes archetype-versus-concern. What the docs do carry is a worked example, and it is a hybrid of both.

[Best Practices for Structuring Builds](https://docs.gradle.org/current/userguide/best_practices_structuring_builds.html) → *Use Convention Plugins for Common Build Logic* ships three plugins: `my.base-java-library` (base configuration), `my.java-use-junit5` (one concern), and `my.java-library`, whose entire body is `plugins { id("my.base-java-library"); id("my.java-use-junit5") }`. Both example projects apply only `my.java-library`. The stated rationale is *"Convention plugins can apply other convention plugins, allowing you to orchestrate your build logic from small pieces."* Note also that `my.java-use-junit5` declares `` `java-library` `` in its own `plugins` block rather than assuming the caller applied it — the concern plugin restates what it needs (§4).

Two supporting data points: [Sharing Build Logic Between Subprojects](https://docs.gradle.org/current/userguide/sharing_build_logic_between_subprojects.html) uses `java-common-conventions` → `java-library-conventions` / `java-application-conventions`, structurally identical to ASAPP's `java` → `library` / `service`; and the [Convention Plugins](https://docs.gradle.org/current/userguide/implementing_gradle_plugins_convention.html) page's single example is named `myproject.publishing-conventions` — a concern name, confirming concern-named plugins are idiomatic even though they are not the applied surface.

### Every other concern already has exactly one consumer

Swept before deciding, so the rule's practical reach is known rather than assumed:

| Concern | Lives in | Archetypes needing it |
|---|---|---|
| **javadoc + sources jars** | `library` **and** `domain-service` | **2 — duplicated** |
| Formatting, compilation, toolchain, unit tier, unit coverage, `fullBuild`/`ciBuild` umbrellas | `java` | 3 (already single-homed by inheritance) |
| Integration tier, its two coverage reports, SBOM, git properties, build info, classpath normalization, local run, Docker image, plain-jar disable | `service` | 1 |
| Mutation testing, API documentation, database migrations | `domain-service` | 1 |
| Git hook installation | `root` | 1 |
| Boot BOM import + Jackson CVE override | `library` and `service` | 2 archetypes, **different mechanisms** — `bomProperty` for libraries, a Gradle `extra` property for services, because the Boot plugin's auto-imported BOM ignores `bomProperty` (`dependency-management-plugin#219`). Similar-looking, not duplicated; nothing to extract. |

One row qualifies. A concern plugin for any other row would be indirection with a single consumer.

## 2. Goals

- A stated rule that answers "which file does this go in?" for every future addition to `build-logic`, with no room for taste.
- The 31 duplicated lines single-sourced, so `gradle.md` no longer asks a human to keep two blocks in sync.
- Module build scripts unchanged: still exactly one convention plugin id each.
- No behaviour change — same tasks, same task graph, same jar contents, in the same five modules.

## 3. Non-goals

- Splitting anything else. The §1 sweep found one qualifying concern; the rule is written so the next one qualifies on evidence, not on appetite.
- The sibling layout subtasks under "Clean up the Gradle build scripts" (block order, plugin-block ordering, dependency-block splits, blank lines, formatter anchors, lazy providers, deprecations, problems report, IDE warnings) — this spec only fixes the file *set*; the new file is swept by each of those in turn like every other script.
- Extracting the `val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")` accessor duplicated across four plugins. It is a backlog item of its own, and precompiled-script-plugin scoping *forces* that duplication rather than eliding it — the javadoc block's duplication is elective, which is what makes it fixable here.
- Unifying the Jackson CVE override (§1 — two mechanisms, one deliberate reason).
- Any binary-plugin conversion. `InstallGitHooks` stays where it is; `gradle-task-types.md` governs it.

## 4. Key decisions

| Decision | Choice | Why |
|---|---|---|
| Split axis | **Archetype plugins are the applied surface; concern plugins compose beneath them** | The shape Gradle's own Best Practices example ships (§1). Keeps "one id per module" — the property the archetype axis exists for — while giving cross-archetype configuration a home. |
| The July objection ("a second, crossing axis") | **Answered, not overruled** | Two axes cross only if both are visible to callers. A concern plugin that no module script may apply is not an axis — it is composition inside one. The rule (§5) makes that a constraint, not a convention. |
| Trigger for a concern plugin | **Exists only where two or more archetypes need it identically and no single archetype already covers them** | Reinstates the trigger `9aa092bc` revoked, with a sharper test than the revoked one ("a third module" / "grows beyond bare registration"): archetype count is countable from the code, module count and block size are judgement calls. |
| What gets extracted today | **The javadoc/sources block only** | The only row in §1's sweep with two archetype consumers. |
| Name | **`asapp.javadoc-sources-conventions`** | Already the name used by `2026-07-23`'s §11 and by the revoked rule text, so the reversal lands on the name the record anticipated. `asapp.documentation-conventions` was rejected: the Asciidoctor API guide is also documentation and stays in `asapp.domain-service-conventions`. |
| What the new plugin declares | **`plugins { id("asapp.java-conventions") }`** | It needs `java` (the `javadoc` task, `SourceSetContainer`) *and* the `fullBuild` task, both from `asapp.java-conventions`. Bare `plugins { java }` would compile but leave `tasks.named("fullBuild")` dependent on the caller's plugin-block ordering. Restating the dependency matches Gradle's `my.java-use-junit5` and the layering `library`/`service`/`domain-service` already use; plugin application is idempotent, so the callers keep their own explicit `id("asapp.java-conventions")`. |
| `fullBuild` wiring | **Moves into the new plugin** | The plugin that registers a task owns wiring it. `fullBuild` is already extended from three files; a fourth is not a new pattern, and it is the reason the plugin declares `asapp.java-conventions` rather than bare `java`. |
| `gradle.md:16` | **Rewritten, not deleted** | The bullet carries three constraints: keep the copies identical, never `withJavadocJar()`/`withSourcesJar()`, never `from(main.allSource)`. Only the first dissolves; the two traps are live and move to the new single home. |
| Where the split rule is recorded | **One new `## Structure` bullet in `gradle.md`, plus this spec** | Passes the three-leg keep-test of `2026-08-11-gradle-rules-trap-cut-design.md` §3: Claude would plausibly create a single-consumer concern plugin or inline shared config into the wrong archetype (1); the code shows the current layout but not the placement rule for a new addition (2); a misplaced convention plugin builds green (3). |
| Line count | **Not a goal; roughly neutral** | Two files shrink by ~34 each, one file of ~44 appears. The win is single-sourcing, not size. |

## 5. The rule

> **One convention plugin per module archetype** — `root`, `java`, `library`, `service`, `domain-service` — and an archetype id is the **only** id a module build script applies.
>
> **A concern plugin** holds one concern, exists **only** where two or more archetypes need it identically **and no single archetype already covers them**, **declares the archetype plugin it builds on**, and is **never** applied by a module build script.

The first paragraph is today's structure, stated. The second is what this spec adds, and it is what makes the reversal safe: the concern plugin is invisible from a module, so the archetype axis remains the only one a caller sees.

## 6. Changes by file

**New — `build-logic/src/main/kotlin/asapp.javadoc-sources-conventions.gradle.kts`**

```kotlin
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.StandardJavadocDocletOptions

plugins {
    id("asapp.java-conventions")
}
```

then the 31 lines lifted verbatim from `asapp.library-conventions:23-53` — the `javadoc` doclint/encoding block, `javadocJar`, `sourcesJar`, comments included — followed by the edge those tasks now own:

```kotlin
// Add the javadoc and sources jars to the full build of every archetype that applies this plugin.
tasks.named("fullBuild") {
    dependsOn("javadocJar", "sourcesJar")
}
```

Nothing is rewritten while moving. Any improvement to the block (the eager `layout.buildDirectory.get()` among them) belongs to the sibling subtask that owns it, against a single copy.

**`build-logic/src/main/kotlin/asapp.library-conventions.gradle.kts`**

- `plugins`: add `id("asapp.javadoc-sources-conventions")`; keep the explicit `id("asapp.java-conventions")` and `` `java-library` ``.
- Delete `:23-53`.
- Drop the three now-unused imports (`Jar`, `Javadoc`, `StandardJavadocDocletOptions`); keep `VersionCatalogsExtension`, still used by the BOM block.
- `fullBuild` edge becomes `dependsOn("jacocoTestReport")`.

**`build-logic/src/main/kotlin/asapp.domain-service-conventions.gradle.kts`**

- `plugins`: add `id("asapp.javadoc-sources-conventions")`.
- Delete `:126-156`.
- Drop the same three imports; keep `AsciidoctorTask` and `VersionCatalogsExtension`.
- `fullBuild` edge drops `"javadocJar"` and `"sourcesJar"`, keeping the three coverage reports and `asciidoctor`.

Plugin-block *position* of the new id follows today's shape (immediately after the parent archetype id); ordering is formalized by `TODO.md:41`, not here.

**`.claude/rules/gradle.md`** — `## Structure`. `:16` becomes, with its "only home for" clause dropped — the plugin's filename carries that, and the placement bullet below governs it:

```markdown
- Javadoc and sources jars are plain `Jar` tasks — never `java.withJavadocJar()` / `withSourcesJar()`, which add them to `assemble`; never `from(main.allSource)`, which drags `gradle-git-properties`' generated dir into a sources jar
```

and one bullet is added above it, carrying §5 — stripped of the archetype name list (derivable from `build-logic/src/main/kotlin/`, and listing `java` there wrongly implied a module applies it) and of the "never applied by a module build script" clause (the same constraint as "applies exactly one", said from the other side), and led with the module-facing constraint, naming the archetype plugin rather than pointing at it, so no reading permits a module to apply a concern plugin instead:

```markdown
- A module build script applies exactly one plugin — its archetype convention plugin, never anything else. `build-logic` also holds concern plugins: one concern each, only where two or more archetypes need it identically and none already covers the others, each declaring the archetype plugin it builds on
```

No frontmatter change: the existing `"**/src/main/kotlin/*.gradle.kts"` glob already matches the new file.

**`TODO.md`** — line 37 ticked, its note dropped, matching how lines 12–35 were closed.

**This spec.**

## 7. Verification / Definition of Done

Capture the two "before" artifacts first, on the unmodified tree — they are the parity baseline and cannot be reconstructed afterwards.

1. **Baseline captures.** `./gradlew fullBuild --dry-run --console=plain`, and `unzip -l` over the four jars from step 3. Both to the scratchpad.
2. **`build-logic` compiles and its guard passes** — `./gradlew :build-logic:check`. It never runs under `build` (`gradle.md`), and this change alters what compiles there.
3. **The jars still build** — `./gradlew :libs:asapp-commons-url:javadocJar :libs:asapp-commons-url:sourcesJar :services:asapp-tasks-service:javadocJar :services:asapp-tasks-service:sourcesJar`. One library and one MapStruct domain service, the same pair `2026-07-23` §8 used as its doclint spike.
4. **Jar contents are byte-for-byte the same set** — re-run step 1's `unzip -l` and diff against the baseline. Empty diff required. This is the check that matters: it catches an `allJava`→`allSource` regression and any generated-directory leak, neither of which fails a build.
5. **The task graph is unchanged** — re-run `./gradlew fullBuild --dry-run --console=plain` and diff against the baseline. Empty diff required; a task appearing or disappearing means the `fullBuild` edge moved wrong.
6. **No surface widening** — `./gradlew :services:asapp-config-service:javadocJar` must still fail with `Task 'javadocJar' not found`, and the same for `:services:asapp-discovery-service`. Proves the extraction did not hand the two infrastructure services jars they never had.
7. **Every module still configures** — `./gradlew spotlessCheck` across all seven.
8. **Developer-run** — `./gradlew fullBuild` end to end, green.

**Done when** 2–7 pass with both diffs empty, 8 is developer-confirmed, `gradle.md` carries the rewritten and the new bullet, and `TODO.md:37` is ticked.

## 8. Out of scope / YAGNI

Every other sibling subtask under "Clean up the Gradle build scripts" · the version-catalog accessor extraction (backlog) · the Jackson override unification (§3) · formatting and license headers on `build-logic` sources (backlog) · configuration-level tests for the conventions' task wiring (backlog) · converting any convention plugin to a binary plugin · publishing configuration of any kind — nothing here is published · module build scripts · `pom.xml` · workflows · application source.

## 9. Contingencies

- **`tasks.named("fullBuild")` fails at configuration time.** The new plugin was applied somewhere `asapp.java-conventions` is not. The fix is its declared `id("asapp.java-conventions")` (§4), never reordering a caller's `plugins` block — an ordering fix would work and would silently re-introduce exactly the fragility that declaration exists to remove.
- **Step 4's jar diff is non-empty.** Stop. The move was supposed to be verbatim; a content change means something was rewritten in transit. Restore the block byte-for-byte before continuing.
- **IntelliJ does not resolve `asapp.javadoc-sources-conventions`.** Re-sync; the type-safe accessors for a precompiled plugin are generated by the included build's `jar`. Not a design problem, and not a reason to change the id.
- **A future concern qualifies under §5 but reads badly as its own file.** Two or more archetypes needing it identically, with no single archetype already covering them, is the trigger, not an obligation to a specific shape — folding it into `asapp.java-conventions` is correct instead whenever *all three* Java archetypes need it, since that is inheritance rather than composition.

## 10. Git workflow

Lands on the current branch, `build/replace-maven-with-gradle-25-clean-build-scripts`. **One** commit — `build(gradle)` — carrying this spec, the three `build-logic` files, the `gradle.md` edit and the `TODO.md` tick together, matching the epic's per-subtask shape.

Stage those six paths explicitly. The working tree carries ~30 modified `.claude/` files from unrelated in-flight authoring work; `git commit -a` would sweep them in.

Per the compressed flow used by every subtask since the coverage one, implementation proceeds without a separate plan document.

## 11. Post-implementation notes

The canonical implementation is build-logic's javadoc-sources, library, and domain-service convention plugins, plus `.claude/rules/gradle.md`, not this document.

Notable deltas:

- **The split's artifacts were reshaped by later cleanup subtasks (revises §6).** The plugin grew 44→51 lines across six commits: origin markers, eager→lazy rewrite, twelve dropped imports, comment-style pass, and heading comments.
- **The eager `buildDirectory.get()` surface went to zero (revises §1).** Not the predicted one hit; now zero repo-wide. Successor is line 43's lazy `layout.buildDirectory.map`, in the new plugin.
- **Line counts landed net −7, not −24 (revises §4).** `library-conventions` 58→28, `domain-service-conventions` 173→145, new plugin 51 lines. §4's "roughly neutral" conclusion still holds.
- **Jar parity held as an entry-set invariant (revises §7).** Compared `unzip -Z1 | sort` output, since entry timestamps change on every rebuild; entry set is the real invariant.
- **Both `fullBuild` comments needed rewording (revises §6).** §6 specified only the `dependsOn` change. Now: "jacoco coverage report" (`:25`) and "domain services' extra artifacts" (`:132`).
- **The new `gradle.md` rule bullet was relocated (revises §5).** Landed under `## Structure`; a later commit moved it into a new `## Convention plugins` section. File grew 49→94 lines.

## 12. References

- Gradle — [Best Practices for Structuring Builds](https://docs.gradle.org/current/userguide/best_practices_structuring_builds.html): the `my.base-java-library` + `my.java-use-junit5` → `my.java-library` example, *"orchestrate your build logic from small pieces"*, and the concern plugin restating its own plugin requirement.
- Gradle — [Sharing Build Logic Between Subprojects](https://docs.gradle.org/current/userguide/sharing_build_logic_between_subprojects.html): `java-common-conventions` → `java-library-conventions` / `java-application-conventions`, the archetype layering this build already mirrors.
- Gradle — [Convention Plugins](https://docs.gradle.org/current/userguide/implementing_gradle_plugins_convention.html): `myproject.publishing-conventions`, a concern-named convention plugin in Gradle's own documentation.
- `docs/superpowers/specs/v0.5.0/2026-07-23-gradle-javadoc-sources-jars-design.md` — the duplicated block, its two traps, and §4's original rejection of a concern plugin that this spec reverses.
- `docs/superpowers/specs/v0.5.0/2026-07-16-gradle-project-module-structure-design.md` — where the archetype taxonomy was introduced, ahead of its own schedule.
- `docs/superpowers/specs/v0.5.0/2026-08-11-gradle-rules-trap-cut-design.md` — the three-leg keep-test §4 measures the new rule bullet against, and the pass that trimmed the original archetype-only rule to today's `gradle.md:16`.

# Gradle release workflow — design spec

**Date**: 2026-08-09
**Status**: Implemented
**Owner**: Antonio Trigo
**Source**: `TODO.md` v0.5.0 → Technical → "Replace Maven with Gradle" → "Migrate the release workflow to Gradle" (line 33), with its six attached notes.
**Scope**: Rewrite `.github/workflows/release.yml` onto Gradle, collapsing its three jobs into two so the jar attached to the GitHub Release is the jar inside the published images. One workflow file, plus two `.claude/rules/gradle.md` bullets and three `TODO.md` edits. No build-script change, no new task, no new test, no `pom.xml` edit, no README edit; `ci.yml` untouched.

## 1. Context

`release.yml` fires on a `v*` tag push and runs three sequential jobs:

```yaml
build-and-test:   mvn install -B --no-transfer-progress -Pfull
                  upload services/*/target/*.jar
publish-docker:   mvn spring-boot:build-image -B --no-transfer-progress -DskipTests
                  for SERVICE_DIR in services/*/; do docker push …; done
create-release:   git-cliff --latest --strip header
                  gh release create … release-artifacts/*/target/*.jar
```

It is the last Maven invocation left in the repository: `ci.yml` migrated at `705cf916`, and every build capability the workflow needs — `fullBuild`, `bootBuildImage`, the versioned image name — has been on Gradle since `9aa092bc` and `e1a08f47`.

**What the three jobs cost, beyond the obvious.** `publish-docker` re-checks-out and rebuilds, because `spring-boot:build-image` needs a packaged jar and Maven has no way to consume the artifact `build-and-test` uploaded. So every release compiles twice. It also means the jars in the images are **not** the jars attached to the Release: a second build restamps `build.time` in `build-info.properties` and — since the SBOM work landed at `8311ea57` — mints a fresh `urn:uuid` serial number and `metadata.timestamp` in `META-INF/sbom/application.cdx.json`, both recorded as unpinnable in `gradle.md` `## SBOM`. `ghcr.io/attrigo/asapp-users-service:0.5.0` therefore carries a different jar, with a different bill of materials, than `asapp-users-service-0.5.0.jar` on the same Release. Nobody chose that; it fell out of the job split.

Gradle removes the reason for the split — `bootBuildImage` depends only on `bootJar`, so it can run in the same job on the same working tree — but **not** the divergence itself, which is the one non-obvious finding of this design.

**The restamp survives the merge, and dictates step order.** `bootBuildInfo` writes `build.time`, which is one of its own inputs, so it can never be `UP-TO-DATE`; `bootJar` consumes `build/resources/main` with `PathSensitivity.RELATIVE` rather than `@Classpath`, so it repacks whenever that timestamp moves. Both facts are measured and recorded — `gradle.md` `## Packaging`, and the CI-aggregate spec's §10, where a warm `ciBuild` re-executed exactly ten tasks: `bootBuildInfo` and `bootJar` on the five services. A second Gradle invocation in the same job therefore produces a **new** jar before `bootBuildImage` reads it. Uploading the artifact after the image build, not before, is what makes the released jar and the imaged jar the same bytes. Ordering is load-bearing here, which is why it carries a comment in the file and a bullet in the rules.

**One dead output.** `publish-docker` declares `outputs.version`, and nothing reads it — `create-release` builds both the tag and the title from `${{ github.ref_name }}`. It goes with the job.

**Note 39's cache problem resolves itself.** The note observes that dropping `cache: maven` from `ci.yml` left `release.yml`'s Maven cache with nothing populating it, since Actions caches restore from the creating ref or the default branch and `ci.yml` on `main` was the only writer. On Gradle the writer is back: `setup-gradle`'s `cache-read-only` defaults to `ref_name != default_branch`, so `main` runs write and a tag run restores. Nothing to configure — the default is the correct scoping, exactly as it was for `ci.yml`.

**What the release skill still does, and why it is not fixed here.** `asapp-release` drives the version through Maven — Step 2 reads the root `pom.xml`, Steps 5 and 9 run `mvn versions:set`, Steps 8 and 10 read it back with `mvn help:evaluate`, and Step 7's local pre-flight is `mvn clean test`. It never writes `gradle.properties`, which is where `project.version` comes from once this lands. A tag pushed in that state would build `ghcr.io/attrigo/<svc>:0.5.0-SNAPSHOT` images and fail at `docker push …:0.5.0`. Developer decision: that migration belongs to *Keep Claude Code files in sync with the migration* (line 40), recorded there as a Warning. Nothing breaks meanwhile — 0.5.0 is released only once every subtask of this epic has landed, which includes line 40.

## 2. Goals

- **No Maven in CI at all.** `release.yml` is the last invocation; after this, nothing in `.github/` runs `mvn`.
- **The released jar is the imaged jar.** One build, one `bootJar`, one SBOM per service per release.
- **No change to what is published.** The same 11 jars, the same five image names and tags, the same git-cliff release notes. This is a migration, not a change to the release's contents.
- **One build per release instead of two.** Drop a checkout, a JDK install, a cache restore and a full recompile.
- **Reuse `ci.yml`'s setup verbatim** — same actions, same versions, same `cache-provider`, same wrapper validation — so the two workflows do not drift into two ways of setting Gradle up.

## 3. Non-goals

- **The `asapp-release` skill.** Routed to line 40 (§1). This spec adds the Warning that records the dependency; it changes no skill file.
- **Removing `pom.xml`.** Line 73's entry. The POMs stay until then; this workflow simply stops invoking them.
- **`ci.yml`.** Untouched, including its two `TEMPORARY (Maven→Gradle epic)` scaffolds, which line 82's Warning reverts.
- **The README's Continuous Integration and release documentation.** Deferred to *Migrate build documentation to Gradle* (line 58), which already carries the CI half.
- **Gating `fullBuild`-only tasks at merge time.** Note 38 observes that a javadoc doclint failure lands green in CI and first appears at release. That stays true and stays deliberate — `gradle.md` `## CI build` forbids widening `ciBuild` to `fullBuild`'s artifacts, and this spec does not revisit it. What changes is only that the release-time discovery now happens under Gradle rather than Maven.
- **Publishing to a Maven repository, artifact signing, byte-reproducible jars.** The last is a Backlog item and the remaining variable behind the fixed image `createdDate` (`## Docker images`).
- **Pinning actions to commit SHAs, path-filtered triggers, patch-version release support.** All already Backlog entries under `#### ci`.

## 4. Key decisions

| Decision | Choice | Rationale |
|---|---|---|
| Job graph | **Two jobs: `build-and-image`, then `create-release`** | Gradle can image the jars it just built, so the second job's reason to exist is gone. Merging removes a checkout, a JDK install, a cache restore and a full recompile per release, and is the precondition for jar/image identity. |
| Rejected: keep three jobs | **No** | It preserves the divergence §1 describes and pays a second full build for it. A separate job cannot consume the uploaded jars — placing files into `build/libs` and persuading Gradle they are current is fragile enough that nobody should build it. |
| Rejected: collapse into one job | **No** | `create-release` needs `fetch-depth: 0` for git-cliff to reach the previous tag. Folding it in makes the build job carry a full-history checkout for a step that runs twenty minutes later, and makes any failure re-run everything. |
| Verification command | **`./gradlew fullBuild --console=plain --stacktrace --continue`** | `fullBuild` is the `mvn install -Pfull` analog by construction (`## Full build`). The two console flags carry over from `ci.yml` unchanged. |
| No `clean` | **Never** | Note 1, and `## Full build` forbids it outright: translating `mvn clean install -Pfull` literally discards the incremental state and build cache this version exists to buy. A from-scratch build stays a separate `./gradlew clean`. |
| `--continue` on `fullBuild` | **Yes** | The release commit (`chore: release version X.Y.Z`) is new — the skill pushes it and the tag atomically, so this is the first, concurrent full verification of that commit, and `fullBuild` covers what `ciBuild` deliberately does not (javadoc doclint, the jars, the three JaCoCo reports). If two of those break, one run should say so. Same reasoning and same flag as `ci.yml`. |
| Image command | **`./gradlew bootBuildImage --console=plain --stacktrace`**, unqualified | The task selector reaches all five services and neither lib, which have no `bootBuildImage` at all (`## Docker images`). No `--continue`: a half-built image set is not a partial verdict worth collecting. |
| Rejected: one invocation, `fullBuild bootBuildImage` | **No** | It would save one configuration phase and one `bootJar` repack, but it merges two failures into one step and lets `--continue` build five images for a build whose tests already failed. Two steps keep the failure attributable and keep `--continue` scoped to verification. |
| **Upload after the image build** | **Load-bearing, not stylistic** | `bootBuildInfo` restamps `build.time` and `bootJar` repacks (§1, measured). Uploading before `bootBuildImage` would attach a jar the images do not contain — reintroducing the exact divergence the merge exists to remove, in a form that looks correct. `gradle.md` `## Docker images` is where that is recorded, and — per the comment-style row below — its **only** marker; the workflow file says what the step does and not why it sits there. |
| Comment style in the workflow | **Say what each step does, never why** | Developer decision, and it matches `release.yml`'s existing comments (`# Runs the full build profile`, `# Pushes images to ghcr.io/attrigo/<service-name>:<version>`). Rationale lives in this spec and in `gradle.md`; restating it in YAML is where the two copies drift apart. Concretely: no `clean`, `--continue` or upload-ordering justification in the file. |
| Push mechanism | **The explicit `docker push` loop, `VERSION` from `${GITHUB_REF_NAME#v}`** | Developer decision. Registry credentials stay in the workflow, where the secret already lives, and the push stays a visible step with per-image output. |
| Rejected: `--publishImage` + `docker { publishRegistry { … } }` | **No** | It would drop the login action, the loop and the version derivation, but moves registry credential plumbing into `asapp.service-conventions`, which every local `bootBuildImage` also configures, and turns two `## Docker images` "stays at its default" entries (`publish`, the `docker { }` registries) into configured ones for a CI-only need. |
| `Extract version from tag` as its own step | **Folded into the push loop** | It existed to feed `outputs.version`, which nothing read. One `VERSION="${GITHUB_REF_NAME#v}"` line inside the loop's `run:` replaces the step, its `id:`, and the job's `outputs:` block. |
| Rejected: a tag-vs-`gradle.properties` guard | **No** | Considered, because the skill drift in §1 makes the mismatch real today. Declined: no release happens before the epic completes, and the migrated skill will derive the tag from the version file (`git tag v${RELEASE_VERSION}`), so the two cannot disagree by construction. A guard against a state that cannot occur is machinery to maintain for nothing. |
| Rejected: `ciBuild` alongside `fullBuild` | **No** | `fullBuild` already contains `build`; the only delta is the root's `:build-logic:check` edge, and `ci.yml` gates that on `main` — including on the release commit, pushed atomically with the tag. |
| `-DskipTests` analog | **None exists** | Note 3: `mvn spring-boot:build-image` forked the `package` lifecycle, hence the flag and Boot's separate `build-image-no-fork` goal; `bootBuildImage` depends only on `bootJar` (`## Docker images`). Nothing to skip and nothing to translate. |
| `cache: maven` on `setup-java` | **Removed from both jobs** | Redundant once Maven is not invoked, and it conflicts with the Gradle caching mechanism. `setup-java` itself stays for the JDK 25 toolchain, which `## Compilation` records as having no fallback. |
| Gradle setup | **`gradle/actions/setup-gradle@v6` with `validate-wrappers: true` and `cache-provider: 'basic'`** | Copied from `ci.yml` deliberately. Note 2: wrapper validation rides this action on the tag path too, so no standalone validation step. `'basic'` is the MIT provider; `'enhanced'` is closed-source under Gradle's Terms of Use — the same licensing call `ci.yml` recorded, and none of the features `'basic'` drops are used here. |
| `cache-read-only` | **Unset — the default is correct** | The default is `ref_name != default_branch`, so a tag run restores `main`'s cache and writes none. Setting it would either make release runs pollute the cache from a one-off ref or turn an already-correct default into a maintained line. Resolves note 39. |
| Artifact glob | **`services/*/build/libs/*.jar`** → download side `release-artifacts/*/build/libs/*.jar` | The same 11 jars as today: 5 boot jars, plus javadoc and sources for the 3 domain services. `upload-artifact` strips the common `services/` prefix, which is why the download-side glob mirrors the upload one — the current file already relies on this. |
| Rejected: narrowing the glob to boot jars | **No** | The javadoc and sources jars have been on every Release since `-Pfull` produced them; dropping them would change what the release publishes, which §2 rules out. |
| `timeout-minutes` | **40 on `build-and-image`, 15 on `create-release` unchanged** | Today's 30 + 20 minus the recompile and the second setup the merge removes. Generous rather than tight: the first Gradle release run has no warm cache for this ref, and five CNB image builds are serial. |
| `permissions` | **Unchanged** | `contents: write` for the Release, `packages: write` for ghcr.io. The merge moves the image push into the first job, but permissions are workflow-level already. |
| `create-release` | **Unchanged but for the glob and `needs:`** | git-cliff, its config, `--latest --strip header`, the `fetch-depth: 0` checkout and `gh release create` are build-tool-agnostic. |
| Commit type | **`ci(gradle)`** | The substance is a workflow file, matching the CI-workflow sibling `705cf916`. `ci` is a listed tooling scope in `.claude/rules/todo.md`. |

## 5. Changes by file

**`.github/workflows/release.yml`** — the header comment's job list loses an entry and gains the image step:

```
# It runs two sequential jobs:
#   1. build-and-image   — full Gradle build (tests, coverage, Javadoc, sources, API guide,
#                          style check), then versioned Docker images pushed to ghcr.io
#   2. create-release    — generates changelog and creates the GitHub Release with JARs
```

Its first and last lines, and the `Permissions required` block below it, are unchanged. The jobs become:

```yaml
  # Builds and tests the project, then builds a versioned Docker image for every service
  # (lib modules are skipped) and pushes them to ghcr.io/attrigo/<service-name>:<version>.
  # Uploads the service JARs as an artifact for use by the create-release job.
  build-and-image:
    runs-on: ubuntu-latest
    timeout-minutes: 40
    steps:
      - name: Checkout the project
        uses: actions/checkout@v6

      - name: Set up JDK 25
        uses: actions/setup-java@v5
        with:
          java-version: '25'
          distribution: 'temurin'

      - name: Set up Gradle
        uses: gradle/actions/setup-gradle@v6
        with:
          # Checks the committed wrapper jar against Gradle's published checksums.
          validate-wrappers: true
          # Stores the Gradle build cache and dependencies in the GitHub Actions cache.
          cache-provider: 'basic'

      - name: Build and test the project
        run: ./gradlew fullBuild --console=plain --stacktrace --continue

      - name: Build the Docker images
        run: ./gradlew bootBuildImage --console=plain --stacktrace

      - name: Upload service JARs
        uses: actions/upload-artifact@v6
        with:
          name: service-jars
          path: services/*/build/libs/*.jar
          retention-days: 1

      - name: Log in to ghcr.io
        uses: docker/login-action@v4
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Push the Docker images
        run: |
          VERSION="${GITHUB_REF_NAME#v}"

          for SERVICE_DIR in services/*/; do
            SERVICE=$(basename "$SERVICE_DIR")
            docker push "ghcr.io/attrigo/${SERVICE}:${VERSION}"
          done
```

`create-release` changes in exactly three places: its leading comment `# Downloads the service JARs built in build-and-test` → `… built in build-and-image`, `needs: [publish-docker]` → `needs: [build-and-image]`, and the `gh release create` glob `release-artifacts/*/target/*.jar` → `release-artifacts/*/build/libs/*.jar`. Its checkout, download step, git-cliff step and their comments are untouched. Everything above `jobs:` — the trigger, `permissions` — is unchanged except the header comment's job list.

Removed outright: the `publish-docker` job header, its `outputs:` block, its second `Checkout` / `Set up JDK 25` steps, and the `Extract version from tag` step with its `id: version`.

**`.claude/rules/gradle.md`** — two bullets, no new section:

1. **`## Full build`** — `release.yml` is `fullBuild`'s consumer: a `v*` tag push runs `./gradlew fullBuild --console=plain --stacktrace --continue` in one job, **never** with `clean` (the existing never-bake-`clean` bullet gains its real-world referent), and the tag ref restores the build cache `ci.yml` writes on `main` while writing none of its own, because `setup-gradle`'s `cache-read-only` default is `ref_name != default_branch`. Note that `ciBuild` is deliberately not also invoked there.
2. **`## Docker images`** — `release.yml` builds all five images with the unqualified `bootBuildImage` in the **same job** as `fullBuild`, and the artifact upload sits **after** that step on purpose: `bootBuildInfo` restamps `build.time` and `bootJar` repacks (already recorded under `## Packaging`), so uploading first would attach jars the images do not contain. The push is an explicit `docker push` loop over `services/*/` with `VERSION="${GITHUB_REF_NAME#v}"`, **not** `--publishImage` — that would move registry credentials into `asapp.service-conventions` and turn this section's `publish` and `docker { }` defaults into configured values for a CI-only need.

**`TODO.md`** — three edits:

1. Tick line 33 and remove its six notes; this spec absorbs all of them (notes 1, 2 and 3 as decision rows in §4, note 4 as the rejected `--publishImage` row, note 5 as §3's `fullBuild`-only gap non-goal, note 6 as §1's cache resolution).
2. Add a **Warning** under *Keep Claude Code files in sync with the migration* (line 40): `asapp-release` still drives the version through `pom.xml` (Steps 2, 5, 8, 9, 10) and pre-flights with `mvn clean test` (Step 7); until those read and write `gradle.properties`, a pushed tag builds `-SNAPSHOT`-named images and the push loop fails.
3. No other entry changes. Line 73's Maven-removal task already covers the POMs, and line 82's `ci.yml` Warning is untouched.

## 6. Verification / Definition of Done

Locally, before the workflow can be observed at all:

- `actionlint` clean on `release.yml` — `docker run --rm -v "$PWD:/repo:ro" --workdir /repo rhysd/actionlint:latest -color`, matching how the CI-workflow sibling was checked.
- `./gradlew fullBuild` from a warm tree, green, and `services/*/build/libs/` holding 11 jars: five `<svc>-<version>.jar`, and `-javadoc.jar` + `-sources.jar` for the three domain services only. No `-plain.jar`.
- The ordering claim, proved rather than asserted: hash the five boot jars after `fullBuild`, run `./gradlew bootBuildImage`, hash them again. The hashes **must** differ — that is the restamp, and it is why the upload moved.
- Then that the post-image jar is the one the image carries. **There is no jar to hash inside the image** — Paketo's java buildpack explodes the archive, so the comparison is on content, not bytes: `docker run --rm --entrypoint cat ghcr.io/attrigo/asapp-config-service:<version> /workspace/META-INF/build-info.properties` against the same entry unzipped from `services/asapp-config-service/build/libs/asapp-config-service-<version>.jar`. Equal `build.time` values prove the image wraps the post-image jar; the pre-image jar's differ. One service is enough — the mechanism is identical across the five.
- `./gradlew bootBuildImage --dry-run` schedules the task in exactly the five services and neither lib.

Residue and parity:

- `release.yml` contains no `mvn`, no `-Pfull`, no `-DskipTests`, no `target/`, and exactly two `./gradlew` invocations.
- No `outputs:` block and no `id: version` survive; `create-release` still resolves its tag and title from `github.ref_name`.
- The trigger, `permissions`, the git-cliff step and the `fetch-depth: 0` checkout are byte-identical to before.

Observable only on the next tag, which by the developer's decision is after the whole epic lands:

- Two jobs run, `build-and-image` green, five images pushed, and the Release carries 11 jars.
- The Gradle step's log shows cache restoration from `main` and no cache write.

## 7. Out of scope / YAGNI

The `asapp-release` skill · `pom.xml` removal · `ci.yml` and its temporary scaffolds · README and CLAUDE.md documentation · a tag-vs-version guard · `--publishImage` · widening `ciBuild` · uploading test or coverage reports on failure · `--scan` / `--profile` in the release build · a build matrix or a second runner · action SHA pinning · publishing to a Maven repository · any build-script, application-source or test change.

## 8. Contingencies

- **Someone re-orders the steps and puts the upload back before the image build.** The identity breaks silently — the run stays green and only a byte comparison would reveal it, and the workflow file carries no marker saying the position matters (the comment-style decision in §4). The `## Docker images` bullet is the sole guard, so it must state the ordering explicitly rather than merely describe it; if this ever regresses, the fix is the order, never a second `bootJar` invocation to "refresh" the artifact.
- **Boot fixes `bootBuildInfo`'s restamp, or `bootJar` moves to `@Classpath`.** The ordering becomes harmless rather than wrong, so leave it; delete the comment only once the measurement in `## Packaging` is re-taken and updated.
- **The 40-minute timeout is hit.** Read the step timings before raising it: a cold cache on the tag ref plus five serial CNB builds is the expected shape, but a `fullBuild` that alone exceeds ~30 minutes is a signal about the integration tier, not about the timeout.
- **A tag run cannot restore `main`'s cache.** The release build goes cold — slower, not wrong. Do **not** reach for `cache-read-only: false` to "fix" it; that makes a one-off ref write the shared cache. Check first that `ci.yml` has run green on `main` since the last cache eviction.
- **`docker push` fails for one service.** The loop aborts mid-way and the Release is never created (`create-release` needs the job), leaving a partially-pushed tag in ghcr.io. Same behaviour as today; re-running the job re-pushes all five, and re-running now also re-runs `fullBuild`, which is the accepted cost of the merge.
- **A non-service directory appears under `services/`.** The loop would try to push an image that was never built. Unchanged from today's loop, and it would fail loudly.
- **`fullBuild` fails only in `javadoc`.** Expected and exactly what note 38 predicted: `ciBuild` does not gate doclint, so the release build is where it first appears. Fix the Javadoc and re-tag; do not drop `--continue` to shorten the log.

## 9. Git workflow

Lands on the current branch, `build/replace-maven-with-gradle`. Two commits, matching this task's siblings: this spec on its own (`docs(gradle)`), then the implementation (`ci(gradle)`) carrying `release.yml`, the two `gradle.md` bullets and the three `TODO.md` edits.

Per the compressed flow used by every subtask since the coverage one, implementation proceeds without a separate plan document unless the developer asks for one.

## 10. References

- Spring Boot Gradle plugin — [Packaging OCI Images](https://docs.spring.io/spring-boot/gradle-plugin/packaging-oci-image.html): `bootBuildImage`'s dependency on `bootJar`, and the `publishRegistry` / `--publishImage` mechanism §4 declines.
- `gradle/actions` — [setup-gradle caching](https://github.com/gradle/actions/blob/main/docs/setup-gradle.md): the `cache-read-only` default of `ref_name != default_branch`, and the `basic` vs `enhanced` provider split.
- GitHub Actions — [Caching dependencies](https://docs.github.com/actions/using-workflows/caching-dependencies-to-speed-up-workflows): a run restores caches created on its own ref or on the default branch, which is what lets a tag run read `main`'s cache.
- `docs/superpowers/specs/v0.5.0/2026-08-07-gradle-ci-workflow-design.md` — the setup steps, flags and licensing call this workflow reuses verbatim.
- `docs/superpowers/specs/v0.5.0/2026-08-09-gradle-ci-aggregate-task-design.md` — §10's measurement that `bootBuildInfo` and `bootJar` re-execute on every invocation, which §1 builds on.
- `docs/superpowers/specs/v0.5.0/2026-07-27-gradle-full-build-design.md` and `2026-07-31-gradle-docker-image-design.md` — the two tasks this workflow invokes.
- `.claude/rules/gradle.md` — `## Full build`, `## Docker images`, `## Packaging`, `## SBOM`.

## 11. Post-implementation notes

The canonical implementation is `.github/workflows/release.yml`, `ci.yml`'s checkout, both convention plugins' `sourcesJar` tasks, `build-logic/settings.gradle.kts`, `InstallGitHooks.kt`, and `.claude/rules/gradle.md`, not this document.

Notable deltas:

- **Javadoc jars excluded from the Release upload (revises §2, §4, §6).** Maven's javadoc goal produced no jar; Gradle's `fullBuild` does, so `release.yml` now excludes `*-javadoc.jar` from the upload glob.
- **`sourcesJar` rewritten in both convention plugins (revises Scope, §6, §7).** `gradle-git-properties`'s generated resources dir broke `from(main.allSource)`; both conventions now filter to `main.allJava` plus real resource dirs.
- **Workflow permissions moved from workflow-level to per-job (revises §4, §5).** `build-and-image` now grants `contents: read` + `packages: write`; `create-release` grants `contents: write` only.
- **`persist-credentials: false` added to all checkouts (revises §3, §5, §6).** Prevents `GITHUB_TOKEN` from persisting in `.git/config`; this is the only `ci.yml` edit, though the spec declared it untouched.
- **Release tag passed via env, not interpolation (revises §4, §5).** `${{ github.ref_name }}` interpolated into the script risked command injection; `env: TAG` with `"$TAG"` fixes it.
- **`timeout-minutes` tightened from 40 to 30 (revises §4, §8).** Measured from the last three releases (peak 10m36s); the merged job does strictly less work than the two it replaced.
- **`build-logic` resolves Central before Plugin Portal (revises Scope, §7).** Portal-first routed all plugins and libraries through the Portal's Central proxy; five genuinely Portal-only plugins are now listed in `gradle.md`.
- **`installGitHooks` now names `core.hooksPath` before clearing (revises Scope, §7).** The task silently unset a developer's local opt-out; `clearHooksPath` now reports the prior value, pinned by a functional test.
- **`asapp-release/SKILL.md` edited despite being declared out of scope (revises §3).** Step 7 named the now-false `-Pfull`; fixed to `./gradlew fullBuild`. The remaining version-drift blocker stays recorded only in `TODO.md`'s Warning.
- **§6's in-image verification recipe doesn't work as written (revises §6).** Paketo's run image has no `cat`; use `docker create` + `docker cp` + `tar -xO`. The restamp check itself passed.

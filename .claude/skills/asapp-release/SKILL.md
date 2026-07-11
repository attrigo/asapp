---
name: asapp-release
description: >
  Runs the full ASAPP release cycle: version bump, Liquibase database tagging, design-spec archival,
  build verification, git commit + tag, next SNAPSHOT prep, and a confirmed push.
  Use when the user wants to execute the release itself, not just talk about it.
  Triggers: /asapp-release, cut a release, open a release, ship the release, run the release, release
  the next version, go with the next release.
  Do NOT use when the user is only asking ABOUT releasing (cadence, what's in the release, how the
  process works) without asking to execute it, to push commits independently of a release, or to
  prepare the next version's backlog (use asapp-prepare-version).
---

# Release

Automates the full ASAPP release cycle: version bump, Liquibase tagging, design-spec archival, build verification, git commit + tag, next SNAPSHOT prep, and push.

**Core principle:** abort readily, mutate nothing until each gate passes, and never push without explicit confirmation.

## Usage

- `/asapp-release` — runs all steps, asks for confirmation before pushing.

## Process

### Step 0: Set up progress tracking

**Before any other step**, create these thirteen tracking tasks with the task tool; mark each `in_progress` when you start it and `completed` when it's done:

1. Validate preconditions (Step 1)
2. Detect versions (Step 2)
3. Check TODO completeness (Step 3)
4. Drop the released TODO section (Step 4)
5. Remove SNAPSHOT (Step 5)
6. Add Liquibase database tags (Step 6)
7. Archive this version's design specs (Step 7)
8. Build and verify (Step 8)
9. Commit release and create tag (Step 9)
10. Bump to next SNAPSHOT (Step 10)
11. Commit next development version (Step 11)
12. Push (Step 12)
13. Wrap-up (Step 13)

If the release aborts (a failed precondition, pending TODO items, or a build failure), leave the current task `in_progress` so the stopping point — and what has already been mutated — stays visible. If the Step 4 TODO drop already landed, see Step 4 to undo it before retrying.

### Step 1: Validate preconditions

```bash
git branch --show-current
git status --porcelain
git log origin/main..HEAD --oneline
gh run list --branch main --workflow ci.yml --limit 1 --json status,conclusion,headBranch,createdAt
```

- If not on `main`: **abort** and tell the user to switch branches.

  ```
  Aborted: You are on branch '<branch-name>'. Releases must be made from main.
  Switch to main before releasing.
  ```

- If working tree is dirty: **abort** and list the uncommitted files:

  ```
  Aborted: Working tree has uncommitted changes:
    M <file>
    ...
  Commit or stash all changes before releasing.
  ```

- If there are unpushed commits: **abort** and list them:

  ```
  Aborted: There are unpushed commits on main:
    <hash> <message>
    ...
  Push or review these commits before releasing.
  ```

- If the last CI run on `main` did not succeed: **abort** and report its status:

  ```
  Aborted: Last CI run on main did not succeed (status: <status>, conclusion: <conclusion>).
  Fix the build before releasing.
  ```

  A run is acceptable only when `status` is `completed` and `conclusion` is `success`. If the run is still `in_progress` or `queued`, abort and ask the user to wait.

### Step 2: Detect versions

Read the root `pom.xml` to extract the current version (e.g. `0.3.0-SNAPSHOT`).

Derive:
- **Release version**: strip `-SNAPSHOT` → `0.3.0`
- **Version underscored**: replace `.` with `_` → `0_3_0` (used for file names)

### Step 3: Check TODO completeness

Open `TODO.md` and locate the version section whose heading starts with `## X.Y.Z ·` (the `## <ver> · <theme>` format — see `.claude/rules/todo.md`). The section spans from that header until the next `##` header or end of file.

Scan every line in that section for unchecked items matching `[ ]`.

- If any `[ ]` items are found: **abort**. List all pending tasks and recommend the user complete them before releasing:

  ```
  Aborted: Version X.Y.Z has pending TODO items:
    - [ ] <task description>
    - [ ] <task description>
    ...
  Complete all tasks for version X.Y.Z before releasing.
  ```

- If the version section is not found in TODO.md: **abort** with a warning that the version has no TODO section and ask the user to confirm whether to proceed.
- If all items are checked (or the section has no checkboxes): continue.

### Step 4: Drop the released TODO section

The version just passed the Step 3 completeness gate — remove its section from `TODO.md` and commit that removal on its own, before the release mutations begin.

- Locate the `## X.Y.Z · <theme>` section for the **release version** (the same section Step 3 just validated).
- Delete it **wholesale** — from its `## ` header to the next `## ` header, including the trailing `---` divider.
- No preservation audit: Step 3 already gated it complete, history keeps it, and the edit is recoverable with `git checkout TODO.md`.
- If Step 3 proceeded with the section absent (user-confirmed), there is nothing to drop — skip this step entirely (no edit, no commit).

Commit the drop on its own (use the release version from Step 2 for `X.Y.Z`):

```bash
git add TODO.md
git commit -m "docs(todo): drop released X.Y.Z section"
```

This keeps backlog housekeeping out of the release and next-dev commits. If the release later aborts, reset it with `git reset --hard HEAD~1` before retrying.

### Step 5: Remove SNAPSHOT

#### Update pom version

```bash
mvn versions:set -DremoveSnapshot=true -DprocessAllModules=true -DgenerateBackupPoms=false
```

Confirm the root `pom.xml` now reads `<version>X.Y.Z</version>` (no SNAPSHOT).

#### Update OpenAPI version

In each of the three service `OpenApiConfiguration.java` files, update the `version` attribute in `@Info(...)` to the **release version**:

```
services/asapp-authentication-service/src/main/java/com/attrigo/asapp/authentication/infrastructure/config/OpenApiConfiguration.java
services/asapp-tasks-service/src/main/java/com/attrigo/asapp/tasks/infrastructure/config/OpenApiConfiguration.java
services/asapp-users-service/src/main/java/com/attrigo/asapp/users/infrastructure/config/OpenApiConfiguration.java
```

Replace `version = "OLD_VERSION"` → `version = "X.Y.Z"` in the `@OpenAPIDefinition` annotation.

#### Update docker-compose.yml

Open `docker-compose.yml` and for every `image:` line matching `ghcr.io/attrigo/asapp-*:`, replace the version tag with the **release version** (e.g. `0.3.0`).

Confirm all five `asapp-*` service image tags now reference the release version.

### Step 6: Add Liquibase database tags

For each service, locate the version changelog file:

```
services/<service>/src/main/resources/liquibase/db/changelog/v<X.Y.Z>/v<X_Y_Z>-changelog.xml
```

For example, for release `0.3.0`:
- `services/asapp-authentication-service/src/main/resources/liquibase/db/changelog/v0.3.0/v0_3_0-changelog.xml`
- `services/asapp-users-service/src/main/resources/liquibase/db/changelog/v0.3.0/v0_3_0-changelog.xml`
- `services/asapp-tasks-service/src/main/resources/liquibase/db/changelog/v0.3.0/v0_3_0-changelog.xml`

**For each file that exists**: check if it already contains `<tagDatabase tag="X.Y.Z"/>`. If not, insert the following changeset before the closing `</databaseChangeLog>` tag:

```xml
    <changeSet id="tag_version_X_Y_Z" author="attrigo">
        <tagDatabase tag="X.Y.Z"/>
    </changeSet>

</databaseChangeLog>
```

Use the underscored version in the `id` attribute (e.g. `tag_version_0_3_0`) and the dotted version in the `tag` attribute (e.g. `0.3.0`).

If a service has no changelog file for this version, skip it — that service had no schema changes in this release.

### Step 7: Archive this version's design specs

Move every design spec sitting directly in `docs/superpowers/specs/` into a version folder (e.g. `docs/superpowers/specs/v0.3.0/`):

```bash
mkdir -p docs/superpowers/specs/vX.Y.Z
git mv docs/superpowers/specs/*-design.md docs/superpowers/specs/vX.Y.Z/
```

- Only the **root-level** specs move; specs already archived in `v*/` subfolders are untouched (the glob does not recurse).
- Keep each file's original `YYYY-MM-DD-<slug>-design.md` name — only its location changes.
- Use `git mv` (never delete and recreate); it stages the moves, so they ride along in the release commit (Step 9).

If there are no root-level specs, skip this step — this version introduced no new design specs.

### Step 8: Build and verify

```bash
mvn clean test
```

**If the build fails**: stop immediately, report the failure, and do not proceed. The user must fix the build before the release can continue.

This is a fast **local pre-flight** only — pushing the tag in Step 12 triggers the `Release` workflow (`.github/workflows/release.yml`), which runs the full `-Pfull` build and tests, publishes the versioned Docker images, and creates the GitHub Release with its changelog. That full verification and publication happens in CI, after the tag lands.

### Step 9: Commit release and create tag

```bash
RELEASE_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
git add .
git commit -m "chore: release version ${RELEASE_VERSION}"
git tag v${RELEASE_VERSION}
```

Confirm the commit and tag were created successfully.

### Step 10: Bump to next SNAPSHOT

#### Update pom version

```bash
mvn versions:set -DnextSnapshot=true -DnextSnapshotIndexToIncrement=2 -DprocessAllModules=true -DgenerateBackupPoms=false
```

Confirm the root `pom.xml` now reads the next SNAPSHOT version (e.g. `0.4.0-SNAPSHOT`).

#### Update OpenAPI version

In each of the three service `OpenApiConfiguration.java` files, update the `version` attribute in `@Info(...)` to the **next SNAPSHOT version**:

```
services/asapp-authentication-service/src/main/java/com/attrigo/asapp/authentication/infrastructure/config/OpenApiConfiguration.java
services/asapp-tasks-service/src/main/java/com/attrigo/asapp/tasks/infrastructure/config/OpenApiConfiguration.java
services/asapp-users-service/src/main/java/com/attrigo/asapp/users/infrastructure/config/OpenApiConfiguration.java
```

Replace `version = "X.Y.Z"` → `version = "X.Y+1.0-SNAPSHOT"` in the `@OpenAPIDefinition` annotation.

#### Update docker-compose.yml

Open `docker-compose.yml` and for every `image:` line matching `ghcr.io/attrigo/asapp-*:`, replace the version tag with the **next SNAPSHOT version** (e.g. `0.4.0-SNAPSHOT`).

Confirm all five `asapp-*` service image tags now reference the next SNAPSHOT version.

### Step 11: Commit next development version

```bash
NEXT_DEV_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
git add .
git commit -m "chore: prepare next development version ${NEXT_DEV_VERSION}"
```

### Step 12: Push

Show the push command, then gate on `AskUserQuestion` before running it:

```bash
git push --atomic origin main vX.Y.Z
```

Ask: "Ready to push — publish the release?" with options **Push** (run the command above) and **Abort** (stop; nothing is pushed). Only run the command if the user picks **Push**.

### Step 13: Wrap-up

Report the pushed main branch and the tag (`vX.Y.Z`).

## Example Output

See [example-output.md](example-output.md) for a full sample run.

## Guardrails

- **Abort before mutating if a precondition fails** — not on `main`, dirty working tree, unpushed commits, or a red last CI run (Step 1); unchecked TODO items for the version (Step 3).
- **Never skip `mvn clean test`** — the release commit (Step 9) is created only after BUILD SUCCESS.
- **Never force push** — `--atomic` only; never `--force` or `--force-with-lease`.
- **Never push without confirmation** — gate on the Step 12 `AskUserQuestion`.

---
name: asapp-release
description: >
  Runs the full ASAPP release cycle: version bump, Liquibase database tagging, design-spec archival,
  build verification, git commit + tag, next SNAPSHOT prep, and a confirmed push.
  Use when the user asks to perform, cut, open, ship, or run the release — e.g. "release the next
  version", "cut a release", "go with the next release".
  Triggers: /asapp-release, cut a release, open a release, ship the release, run the release, go with the next release.
  Do NOT use when the user is only asking ABOUT releasing (cadence, what's in the release, how the
  process works) without asking to execute it, or to push commits independently of a release.
---

# Release

Automates the full ASAPP release cycle: version bump, Liquibase tagging, design-spec archival, build verification, git commit + tag, next SNAPSHOT prep, and push.

## Usage

- `/asapp-release` — runs all steps, asks for confirmation before pushing

## Instructions

### Step 0: Set up progress tracking

**Before any other step**, create these eleven tracking tasks with the task tool; mark each `in_progress` when you start it and `completed` when it's done:

1. Validate preconditions (Step 1)
2. Detect versions (Step 2)
3. Check TODO completeness (Step 3)
4. Remove SNAPSHOT (Step 4)
5. Add Liquibase database tags (Step 5)
6. Archive this version's design specs (Step 6)
7. Build and verify (Step 7)
8. Commit release and create tag (Step 8)
9. Bump to next SNAPSHOT (Step 9)
10. Commit next development version (Step 10)
11. Push (Step 11)

If the release aborts (a failed precondition, pending TODO items, or a build failure), leave the current task `in_progress` so the stopping point — and what has already been mutated — stays visible.

### Step 1: Validate Preconditions

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

### Step 2: Detect Versions

Read the root `pom.xml` to extract the current version (e.g. `0.3.0-SNAPSHOT`).

Derive:
- **Release version**: strip `-SNAPSHOT` → `0.3.0`
- **Version underscored**: replace `.` with `_` → `0_3_0` (used for file names)

### Step 3: Check TODO Completeness

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

### Step 4: Remove SNAPSHOT

#### Update pom version

```bash
mvn versions:set -DremoveSnapshot=true -DprocessAllModules=true -DgenerateBackupPoms=false
```

Confirm the root `pom.xml` now reads `<version>X.Y.Z</version>` (no SNAPSHOT).

#### Update OpenAPI Version

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

### Step 5: Add Liquibase Database Tags

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

### Step 6: Archive This Version's Design Specs

Move every design spec sitting directly in `docs/superpowers/specs/` into a version folder (e.g. `docs/superpowers/specs/v0.3.0/`):

```bash
mkdir -p docs/superpowers/specs/vX.Y.Z
git mv docs/superpowers/specs/*-design.md docs/superpowers/specs/vX.Y.Z/
```

- Only the **root-level** specs move; specs already archived in `v*/` subfolders are untouched (the glob does not recurse).
- Keep each file's original `YYYY-MM-DD-<slug>-design.md` name — only its location changes.
- Use `git mv` (never delete and recreate); it stages the moves, so they ride along in the release commit (Step 8).

If there are no root-level specs, skip this step — this version introduced no new design specs.

### Step 7: Build and Verify

```bash
mvn clean test
```

**If the build fails**: stop immediately, report the failure, and do not proceed. The user must fix the build before the release can continue.

### Step 8: Commit Release and Create Tag

```bash
RELEASE_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
git add .
git commit -m "chore: release version ${RELEASE_VERSION}"
git tag v${RELEASE_VERSION}
```

Confirm the commit and tag were created successfully.

### Step 9: Bump to Next SNAPSHOT

#### Update pom version

```bash
mvn versions:set -DnextSnapshot=true -DnextSnapshotIndexToIncrement=2 -DprocessAllModules=true -DgenerateBackupPoms=false
```

Confirm the root `pom.xml` now reads the next SNAPSHOT version (e.g. `0.4.0-SNAPSHOT`).

#### Update OpenAPI Version

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

### Step 10: Commit Next Development Version

```bash
NEXT_DEV_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
git add .
git commit -m "chore: prepare next development version ${NEXT_DEV_VERSION}"
```

### Step 11: Push

Display the push command and ask for confirmation before running it:

```
Ready to push. Run the following command to publish the release:

  git push --atomic origin main vX.Y.Z

Proceed? [y/N]
```

Only push if the user confirms.

## Safety

- **Abort if not on `main`** — releases must come from the main branch
- **Abort if working tree is dirty** — prevents accidental inclusion of uncommitted changes
- **Abort if there are unpushed commits** — prevents releasing from a state that diverges from the remote
- **Abort if last CI run on `main` did not succeed** — prevents releasing from a broken build
- **Abort if TODO has unchecked items for the version** — ensures the release is feature-complete
- **Archive specs with `git mv`** — relocate the root-level design specs into the version folder; leave already-archived ones untouched
- **Never skip `mvn clean test`** — the build must compile and the tests must pass before any commits are made
- **Never force push** — use `--atomic` only; never `--force` or `--force-with-lease`
- **Never push without confirmation**

## Example Output

```
[Step 1] Validating preconditions...  done (branch: main, tree: clean, unpushed: none, CI: success)
[Step 2] Detected version: 0.3.0-SNAPSHOT → releasing as 0.3.0
[Step 3] Checking TODO completeness for version 0.3.0...  done (all tasks completed)
[Step 4] Removing SNAPSHOT...
  - pom.xml → 0.3.0
  - OpenAPI version → 0.3.0 (3 services)
  - docker-compose.yml → 0.3.0 (5 services)
[Step 5] Tagging Liquibase changelogs...
  - asapp-authentication-service: added tag_version_0_3_0
  - asapp-users-service: added tag_version_0_3_0
  - asapp-tasks-service: no v0.3.0 changelog found, skipped
[Step 6] Archiving design specs...  done (4 specs → docs/superpowers/specs/v0.3.0/)
[Step 7] Building and testing...  done (BUILD SUCCESS)
[Step 8] Committing release and tagging...  done (tag: v0.3.0)
[Step 9] Bumping to next SNAPSHOT...
  - pom.xml → 0.4.0-SNAPSHOT
  - OpenAPI version → 0.4.0-SNAPSHOT (3 services)
  - docker-compose.yml → 0.4.0-SNAPSHOT (5 services)
[Step 10] Committing next dev version...  done

Ready to push. Run the following command to publish the release:

  git push --atomic origin main v0.3.0

Proceed? [y/N]
```

## Common mistakes

| Mistake | Fix |
|---------|-----|
| Starting Step 1 before creating the eleven tracking tasks | Do Step 0 first — create the tasks, then begin. |
| Continuing past a failed precondition | Any failed check in Step 1 aborts — never release from a dirty, diverged, or red-CI state. |
| Releasing with pending TODO items for the version | Step 3 aborts on any `[ ]` — complete them first. |
| Committing before the build passes | Never skip the build in Step 7; fix it before any commit. |
| Updating only some version references | Bump the pom, all three OpenAPI configs, and all five docker-compose tags together. |
| Pushing without confirmation, or force-pushing | Show the command, wait for confirmation, and push only with `--atomic`. |
| Archiving specs by delete-and-recreate | Relocate root-level specs with `git mv` so history is preserved. |

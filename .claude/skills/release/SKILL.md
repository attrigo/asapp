---
name: release
description: >
  Use this skill when the user wants to cut and publish a new release of ASAPP.
  Automates the full release cycle: removes SNAPSHOT suffix, tags Liquibase changelogs,
  builds and verifies, commits, creates a git tag, bumps to the next SNAPSHOT, and pushes.
  Triggers include: release, publish, cut a release, ship a version, bump to release, tag and push.
  Do NOT use for ad-hoc version bumps, hotfix cherry-picks, or SNAPSHOT-only changes.
---

# Release

Automates the full ASAPP release cycle: version bump, Liquibase tagging, build verification, git commit + tag, next SNAPSHOT prep, and push.

## Usage

- `/release` — runs all steps, asks for confirmation before pushing

## Instructions

### Step 1: Validate Preconditions

```bash
git branch --show-current
git status --porcelain
git log origin/main..HEAD --oneline
gh run list --branch main --workflow ci.yml --limit 1 --json status,conclusion,headBranch,createdAt
```

- If not on `main`: **abort** and tell the user to switch branches.
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

Open `TODO.md` and locate the section `## Version X.Y.Z` that matches the release version from Step 2. The section spans from that header until the next `##` header or end of file.

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
services/asapp-authentication-service/src/main/java/com/bcn/asapp/authentication/infrastructure/config/OpenApiConfiguration.java
services/asapp-tasks-service/src/main/java/com/bcn/asapp/tasks/infrastructure/config/OpenApiConfiguration.java
services/asapp-users-service/src/main/java/com/bcn/asapp/users/infrastructure/config/OpenApiConfiguration.java
```

Replace `version = "OLD_VERSION"` → `version = "X.Y.Z"` in the `@OpenAPIDefinition` annotation.

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

### Step 6: Build and Verify

```bash
mvn clean install
```

**If the build fails**: stop immediately, report the failure, and do not proceed. The user must fix the build before the release can continue.

### Step 7: Commit Release and Create Tag

```bash
RELEASE_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
git add .
git commit -m "chore: release version ${RELEASE_VERSION}"
git tag v${RELEASE_VERSION}
```

Confirm the commit and tag were created successfully.

### Step 8: Bump to Next SNAPSHOT

#### Update pom version

```bash
mvn versions:set -DnextSnapshot=true -DnextSnapshotIndexToIncrement=2 -DprocessAllModules=true -DgenerateBackupPoms=false
```

Confirm the root `pom.xml` now reads the next SNAPSHOT version (e.g. `0.4.0-SNAPSHOT`).

#### Update OpenAPI Version

In each of the three service `OpenApiConfiguration.java` files, update the `version` attribute in `@Info(...)` to the **next SNAPSHOT version**:

```
services/asapp-authentication-service/src/main/java/com/bcn/asapp/authentication/infrastructure/config/OpenApiConfiguration.java
services/asapp-tasks-service/src/main/java/com/bcn/asapp/tasks/infrastructure/config/OpenApiConfiguration.java
services/asapp-users-service/src/main/java/com/bcn/asapp/users/infrastructure/config/OpenApiConfiguration.java
```

Replace `version = "X.Y.Z"` → `version = "X.Y+1.0-SNAPSHOT"` in the `@OpenAPIDefinition` annotation.

### Step 9: Commit Next Development Version

```bash
NEXT_DEV_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
git add .
git commit -m "chore: prepare next development version ${NEXT_DEV_VERSION}"
```

### Step 10: Push

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
- **Never skip `mvn clean install`** — the build must pass before any commits are made
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
[Step 5] Tagging Liquibase changelogs...
  - asapp-authentication-service: added tag_version_0_3_0
  - asapp-users-service: added tag_version_0_3_0
  - asapp-tasks-service: no v0.3.0 changelog found, skipped
[Step 6] Building...  done (BUILD SUCCESS)
[Step 7] Committing release and tagging...  done (tag: v0.3.0)
[Step 8] Bumping to next SNAPSHOT...
  - pom.xml → 0.4.0-SNAPSHOT
  - OpenAPI version → 0.4.0-SNAPSHOT (3 services)
[Step 9] Committing next dev version...  done

Ready to push. Run the following command to publish the release:

  git push --atomic origin main v0.3.0

Proceed? [y/N]
```

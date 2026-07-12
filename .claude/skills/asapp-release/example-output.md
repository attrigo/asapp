# Release — Example Output

A sample run of `/asapp-release`, referenced from [SKILL.md](SKILL.md).

```
[Step 1] Validating preconditions...  done (branch: main, tree: clean, unpushed: none, CI: success)
[Step 2] Detected version: 0.3.0-SNAPSHOT → releasing as 0.3.0
[Step 3] Checking TODO completeness for version 0.3.0...  done (all tasks completed)
[Step 4] Closing out documentation for 0.3.0...
  - dropped released TODO section
  - archived 4 specs → docs/superpowers/specs/v0.3.0/
  done (committed: docs: close out 0.3.0)
[Step 5] Removing SNAPSHOT...
  - pom.xml → 0.3.0
  - OpenAPI version → 0.3.0 (3 services)
  - docker-compose.yml → 0.3.0 (5 services)
[Step 6] Tagging Liquibase changelogs...
  - asapp-authentication-service: added tag_version_0_3_0
  - asapp-users-service: added tag_version_0_3_0
  - asapp-tasks-service: no v0.3.0 changelog found, skipped
[Step 7] Building and testing...  done (BUILD SUCCESS — local pre-flight; full build/publish run in CI after push)
[Step 8] Committing release and tagging...  done (tag: v0.3.0)
[Step 9] Bumping to next SNAPSHOT...
  - pom.xml → 0.4.0-SNAPSHOT
  - OpenAPI version → 0.4.0-SNAPSHOT (3 services)
  - docker-compose.yml → 0.4.0-SNAPSHOT (5 services)
[Step 10] Committing next dev version...  done

Ready to push — publish the release?
  git push --atomic origin main v0.3.0

  [Push]   run the command above
  [Abort]  stop; nothing is pushed
```

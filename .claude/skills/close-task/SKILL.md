---
name: close-task
description: >
  Use when a TODO.md task's implementation, fixes, and final review are all done and you are ready to
  integrate the work into local main and wrap the task up.
  Triggers: /close-task, close the task, finish the task, land the branch, squash-merge into main,
  wrap up and merge the task, integrate the task branch.
  Do NOT use to push anything to a remote (it never pushes), to review or fix code (use review-task and
  resolve-review-issues first), or to integrate an unrelated or unfinished branch.
---

# Close Task

Integrate a finished task into the **local** `main` branch. Runs end-to-end with no confirmation gate
and **never pushes** — every git action is local and revertible (the sole exception is the final
SDD-scratch cleanup).

**Core principle — the end-state is a contract.** When this skill finishes:

- **`main`** has the task's work as **one squash commit**, which **includes the implemented spec** and **excludes the plan file**.
- **The task branch** keeps **all its development commits**, with the **plan committed as the last commit**.
- **Nothing is pushed.** Both branches stay local so you can revert if anything looks wrong.

## Usage

- `/close-task` — close the task on the current branch
- `/close-task <branch-or-task-name>` — close the named task/branch

## Process (end-to-end, no gate, no push)

**Track progress with the task tool.** Before Step 0, create these ten tasks; mark each
`in_progress` when you start it and `completed` when it's done:

1. Resolve, detect, capture revert anchors (Step 0)
2. Analyze the task's context (Step 1)
3. Mark the spec implemented (Step 2)
4. Commit the spec (Step 3)
5. Draft the squash message (Step 4)
6. Squash-merge into main, excluding the plan (Step 5)
7. Commit the plan as the last branch commit (Step 6)
8. Verify invariants (Step 7)
9. Clear the SDD record (Step 8)
10. Wrap-up (Step 9)

If the close aborts (a Step 5 merge conflict or a failed Step 7 invariant), leave the current
task `in_progress` so the stopping point — and what has already been mutated — stays visible.

### Step 0: Resolve, detect, capture revert anchors

- **Resolve** — from the current branch or the argument (stop and ask if any is ambiguous; don't guess before mutating):
   - The task branch
   - The task slug
   - The spec — `docs/superpowers/specs/YYYY-MM-DD-<slug>-design.md`
   - The plan — `docs/superpowers/plans/YYYY-MM-DD-<slug>.md`
- **Locate the SDD record** — `.superpowers/sdd/` (untracked working dir): `progress.md` plus the per-task `*-brief.md`/`*-report.md`. It captures intent, decisions, and deviations, and feeds Steps 1, 2, and 4. If it is absent, fall back to commits alone.
- **Capture revert anchors** and surface them to the user immediately:
  ```bash
  PRE_MAIN=$(git rev-parse main)      # main tip before the merge
  PRE_BRANCH=$(git rev-parse HEAD)    # task branch tip before closing
  ```
- **Detect the plan's state** — committed on the branch, or uncommitted/untracked (drives Step 5). See *Plan handling*.
- **Preconditions** — abort with a clear message if any fail (a safety check, not a gate):
   - On the task branch
   - Working tree has no unrelated uncommitted changes
   - `main` exists
   - The branch is not already merged

### Step 1: Analyze the task's context

Find where the implementation diverged from the design.

- **Draw on both sources:**
    - **The SDD record** in `.superpowers/sdd/` — `progress.md` + the `*-report.md`s (intent, decisions, deviations)
    - **Git** — `git log main..<branch>` + the diffs, which also carry the manual-review changes made *after* the SDD run
- **Capture each delta by the durable artifacts it touched** (files, classes, config, tests), **not commit hashes** — the SDD files contain hashes; do not copy them through.
- **Delegate** to a read-only review agent (`architect-reviewer` / `code-reviewer` / `Explore`), or do it inline.

### Step 2: Mark the spec implemented

Dispatch `documentation-engineer` to update **only the spec file** from the Step 1 analysis:

- Changes the header `**Status**:` from `Proposed`/`Draft` → `Implemented`, and
- Appends a `## N. Post-implementation notes` section (N = next section number) per the *Post-implementation notes recipe*.

(If the spec is already `Implemented`, skip this step and reuse the existing notes.)

### Step 3: Commit the spec (on the task branch)

Commit **only the spec file**, message built with `draft-commit-msg`, following the reference style:
`docs(<scope>): mark <task> design spec as implemented`.

### Step 4: Draft the squash message

Build the squash message with `draft-commit-msg`, using the Step 1 analysis (the SDD record + git) and the full task range (`git log main..<branch>` + the squashed diff).

### Step 5: Squash-merge into main, excluding the plan

```bash
git checkout main
git merge --squash <branch>
# Plan handling — keep the plan OFF main (see table):
#   plan already committed on the branch (now staged by the merge):
git restore --staged --worktree -- docs/superpowers/plans/<plan>
#   plan uncommitted/untracked: nothing was staged — skip the restore
git commit -F <squash-message>      # the message from Step 4 (overrides MERGE_MSG)
```

If the squash merge **conflicts**, run `git merge --abort` and report — do not guess resolutions.

### Step 6: Commit the plan as the last branch commit

```bash
git checkout <branch>
# plan uncommitted: commit it now so it is the last commit
git add docs/superpowers/plans/<plan>
git commit -m "docs(<scope>): add <task> implementation plan"
# plan already committed: no-op (verify it is present; if it is not the last commit, report it — do NOT rewrite history)
git checkout main
```

### Step 7: Verify invariants

Run and show the results:
```bash
git cat-file -e main:docs/superpowers/plans/<plan> 2>/dev/null && echo "FAIL: plan on main" || echo "OK: plan absent from main"
git log main -1 --stat        # the squash commit, includes the spec
git log <branch> -1 --stat    # the plan as the last branch commit
```
If any invariant **fails**, stop and report — **do not clean up**.

### Step 8: Clear the SDD record

**Only once the invariants pass**, delete the SDD working content — it is untracked scaffolding and its essence is now in the spec notes + the squash commit. Preserve the `.gitignore` marker:
```bash
[ -d .superpowers/sdd ] && find .superpowers/sdd -mindepth 1 ! -name .gitignore -delete
```
This is the **only non-git-revertible** action — which is why it runs last, after a clean close.

### Step 9: Wrap-up

- **Report**: what landed on `main` (the squash commit), what the branch holds, the spec status change, and that `.superpowers/sdd` was cleared.
- **Revert instructions** (git actions only — see *Reverting*; the cleared SDD files are not recoverable).
- Remind the user: pushing `main` and the task branch is their manual step.

## Plan handling

| Plan state at closing | How it lands on the branch | How it's kept off main |
|-----------------------|----------------------------|------------------------|
| **Uncommitted / untracked** (the clean default) | Committed in Step 6, so it is the last commit | `git merge --squash` never staged it — auto-excluded |
| **Already committed** | Already in branch history | `git merge --squash` staged it → `git restore --staged --worktree` drops it from main's commit |

The canonical flow assumes the plan is **uncommitted** at closing (committed last, in Step 6). The already-committed path is the safety net; if committing the spec in Step 3 leaves the plan no longer the last commit, **report it — never rewrite history to reorder.**

## Post-implementation notes recipe

The `## N. Post-implementation notes` section states, in order:

1. **Opener** — "This spec and its plan (`<plan-path>`) were written before implementation. The core change shipped substantially as designed — &lt;one sentence on what landed as specified&gt;."
2. **Canonical source** — "the canonical implementation is the current state of &lt;the real artifacts: files, configs, tests&gt; on this branch, not this document."
3. **`Notable deltas:`** — a bullet per place the implementation diverged from the design. Each bullet: a **bold headline** naming the delta (and which spec section it reverses), then the *why*, anchored to the **durable artifacts** it touched — the files, classes, config keys, and tests.
4. **Closer** — "For future &lt;area&gt; edits, treat &lt;the real artifacts&gt; as the template; this spec is preserved as a record of the original design intent."

**Never cite commit hashes.** The whole task squash-merges into a single commit on `main` (where this spec lives), so the branch's individual SHAs no longer exist there — a cited hash becomes a dead reference. Anchor every delta to the code artifacts instead; the spec is a record of intent, the code is the source of truth.

## Reverting (nothing is pushed)

```bash
# Undo the squash merge on main:
git checkout main && git reset --hard $PRE_MAIN
# Undo the plan commit on the branch (only if Step 6 added it):
git checkout <branch> && git reset --hard $PRE_BRANCH
```

> The Step 8 `.superpowers/sdd` cleanup is **not** covered by this — those files are untracked and gone for good. That is why it runs only after the invariants pass.

## Delegation & tools — quick reference

| Situation | Use |
|-----------|-----|
| Analyze the SDD record (`.superpowers/sdd/`) + commits/diffs — what shipped and the deltas (read-only) | `architect-reviewer` / `code-reviewer` / `Explore` |
| Update the spec file — Status + post-impl notes (writing only, no code review) | `documentation-engineer` |
| Build the spec / squash / plan commit messages | `draft-commit-msg` skill |
| Locate the spec/plan or understand a commit | `Explore` |

Pick the **most specific** agent from `.claude/agents/`; `general-purpose` is a last resort.

## Hard rules

- **Never push** — no `git push` of any branch, ever.
- **Never rewrite history** — no rebase, no `reset` of existing commits to reorder the plan.
- **Capture `PRE_MAIN` and `PRE_BRANCH` before any mutation** — revert depends on them.
- **The spec lands on main; the plan never lands on main** — verify both before reporting done.
- **One squash commit on main** for the whole task; messages built with `draft-commit-msg`.
- **Source the spec notes and squash message from `.superpowers/sdd/` + git** — but never copy SDD commit hashes into the spec.
- **Clear `.superpowers/sdd` last** — only after the invariants pass; it is the one non-git-revertible action, and `.gitignore` is preserved.
- **Abort, don't guess** — on a merge conflict or an unresolved spec/plan/branch, stop and report.

## Common mistakes

| Mistake | Fix |
|---------|-----|
| Plan file ends up on main | Detect its state; `git restore --staged --worktree` it out of the squash, or commit it only after the merge. |
| Forgetting to capture revert anchors | Record `PRE_MAIN`/`PRE_BRANCH` in Step 0, before mutating. |
| Auto-resolving a squash merge conflict | `git merge --abort` and report. |
| Clearing `.superpowers/sdd` before the close verifies, or deleting its `.gitignore` | Clear it only after Step 7 passes; keep `.gitignore`. |

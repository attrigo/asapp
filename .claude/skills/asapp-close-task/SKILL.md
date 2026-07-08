---
name: asapp-close-task
description: >
  Use when a TODO.md task's implementation, fixes, and final review are all done and it's time to
  integrate the work into local main and wrap up the task.
  Triggers: /asapp-close-task, close the task, finish the task, land the branch, squash-merge into main,
  wrap up and merge the task, integrate the task branch.
  Do NOT use to push anything to a remote (it never pushes), to review or fix code (use asapp-review-task and
  asapp-resolve-review-issues first), or to integrate an unrelated or unfinished branch.
---

# Close Task

Integrate a finished task into the **local** `main` branch. Runs end-to-end with no confirmation gate and **never pushes** — every git action is local and revertible (the sole exception is the final SDD-scratch cleanup).

**Core principle — the end-state is a contract.** When this skill finishes:

- **`main`** has the task's work as **one squash commit**, which **includes the implemented spec**, **marks the parent task `[X]` complete in `TODO.md`**, and **excludes the plan file and the review-task report**.
- **The task branch** keeps **all its development commits**, with the **plan and the review-task report committed as the last commit**.
- **Both branches stay local**, so you can revert if anything looks wrong.

## Usage

- `/asapp-close-task` — close the task on the current branch
- `/asapp-close-task <branch-or-task-name>` — close the named task/branch

## Process

### Step 0: Set up progress tracking

**Before any other step**, create these ten tracking tasks with the task tool; mark each `in_progress` when you start it and `completed` when it's done:

1. Resolve, detect, capture revert anchors (Step 1)
2. Analyze the task's context (Step 2)
3. Mark the spec implemented (Step 3)
4. Mark the parent task complete (Step 4)
5. Draft the squash message (Step 5)
6. Squash-merge into main (Step 6)
7. Commit the plan and review report as the last branch commit (Step 7)
8. Verify invariants (Step 8)
9. Clear the SDD record (Step 9)
10. Wrap-up (Step 10)

If the close aborts (a Step 6 merge conflict or a failed Step 8 invariant), leave the current task `in_progress` so the stopping point — and what has already been mutated — stays visible.

### Step 1: Resolve, detect, capture revert anchors

- **Resolve** — from the current branch or the argument (stop and ask if any is ambiguous; don't guess before mutating):
   - The task branch — if the argument matches a local branch name, use it directly; otherwise treat it as a task name, find the matching `TODO.md` entry, and resolve to the branch checked out for that task.
   - The task slug
   - The spec — `docs/superpowers/specs/YYYY-MM-DD-<slug>-design.md`
   - The plan — `docs/superpowers/plans/YYYY-MM-DD-<slug>.md`
   - The review-task report — `docs/reviews/<task-slug>-review.md` (may be absent if the task had no apply-now review findings)
- **Locate the SDD record** — `.superpowers/sdd/` (untracked working dir): `progress.md` plus the per-task `*-brief.md`/`*-report.md`. It captures intent, decisions, and deviations, and feeds Steps 2, 3, and 5. If it is absent, fall back to commits alone.
- **Capture revert anchors** and surface them to the user immediately:
  ```bash
  PRE_MAIN=$(git rev-parse main)      # main tip before the merge
  PRE_BRANCH=$(git rev-parse HEAD)    # task branch tip before closing
  ```
- **Detect the plan's state** — committed on the branch, or uncommitted/untracked (drives Step 6). See *Plan & report handling*.
- **Preconditions** — abort with a clear message if any fail (a safety check, not a gate):
   - On the task branch
   - Working tree has no unrelated uncommitted changes
   - `main` exists
   - The branch is not already merged

### Step 2: Analyze the task's context

Find where the implementation diverged from the design.

- **Draw on both sources:**
    - **The SDD record** in `.superpowers/sdd/` — `progress.md` plus the per-task `*-brief.md`/`*-report.md` (intent, decisions, deviations)
    - **Git** — `git log main..<branch>` + the diffs, which also carry the manual-review changes made *after* the SDD run
- **Capture each delta by the durable artifacts it touched** (files, classes, config, tests), **not commit hashes** — the SDD files contain hashes; do not copy them through.
- **Delegate by default** to a read-only review agent (`architect-reviewer` / `code-reviewer` / `Explore`); do it inline only for a trivially small diff (e.g. a one- or two-file change).

### Step 3: Mark the spec implemented

Dispatch `documentation-engineer` to update **only the spec file** from the Step 2 analysis:

- Changes the header `**Status**:` from `Proposed`/`Draft` → `Implemented`, and
- Appends a `## N. Post-implementation notes` section (N = next section number) per the *Post-implementation notes recipe*.

(If the spec is already `Implemented`, skip this step and reuse the existing notes.)

Then commit **only the spec file** on the task branch, message built with `asapp-draft-commit-msg`, following the reference style: `docs(<scope>): mark <task> design spec as implemented`.

### Step 4: Mark the parent task complete

Flip the resolved task's `- [ ]` → `- [X]` in `TODO.md` (the entry resolved in Step 1)

(If the TODO is already checked, skip this step.)

Then commit **only `TODO.md`** on the task branch, message built with `asapp-draft-commit-msg`, following the reference style: `docs(<scope>): mark <task> complete in TODO.md`.

### Step 5: Draft the squash message

Build the squash message with `asapp-draft-commit-msg`, using the Step 2 analysis (the SDD record + git) and the full task range (`git log main..<branch>` + the squashed diff). 

`asapp-draft-commit-msg` only displays the text — it never writes files — so write the drafted message verbatim to `<squash-message-file>`, a temp file under the session scratchpad, for Step 6's `git commit -F` to read.

### Step 6: Squash-merge into main, excluding the plan and report

```bash
git checkout main
git merge --squash <branch>
# Plan handling — keep the plan OFF main (see table):
#   plan already committed on the branch (now staged by the merge):
git restore --staged --worktree -- docs/superpowers/plans/<plan>
#   plan uncommitted/untracked: nothing was staged — skip the restore
# The review-task report is untracked here (resolve never commits it), so the squash never stages it — auto-excluded from main.
git commit -F <squash-message-file>      # the file written in Step 5 (overrides MERGE_MSG)
```

If the squash merge **conflicts**, run `git merge --abort` and report — do not guess resolutions.

### Step 7: Commit the plan and review report as the last branch commit

```bash
git checkout <branch>
# plan (+ report) uncommitted: commit them now so they are the last commit
git add docs/superpowers/plans/<plan> docs/reviews/<task-slug>-review.md   # omit the report path if the task had none
git commit -m "docs(<scope>): add <task> implementation plan and review report"   # drop "and review report" when there is no report
# plan already committed: no-op (verify it is present; if it is not the last commit, report it — never rewrite history to reorder)
git checkout main
```

### Step 8: Verify invariants

Run and show the results:
```bash
git cat-file -e main:docs/superpowers/plans/<plan> 2>/dev/null && echo "FAIL: plan on main" || echo "OK: plan absent from main"
git cat-file -e main:docs/reviews/<task-slug>-review.md 2>/dev/null && echo "FAIL: report on main" || echo "OK: report absent from main"   # skip if the task had no report
git log main -1 --stat        # the squash commit, includes the spec
git log <branch> -1 --stat    # the plan and report as the last branch commit
```
If any invariant **fails**, stop and report — **do not clean up**.

### Step 9: Clear the SDD record

**Only once the invariants pass**, delete the SDD working content — it is untracked scaffolding and its essence is now in the spec notes + the squash commit. Preserve the `.gitignore` marker:
```bash
[ -d .superpowers/sdd ] && find .superpowers/sdd -mindepth 1 ! -name .gitignore -delete
```
This is the **only non-git-revertible** action — which is why it runs last, after a clean close.

### Step 10: Wrap-up

- **Report**: what landed on `main` (the squash commit — including the parent task now `[X]` in `TODO.md`), what the branch holds, the spec status change, and that `.superpowers/sdd` was cleared.
- **Revert instructions** (git actions only — see *Reverting*; the cleared SDD files are not recoverable).
- Remind the user: pushing `main` and the task branch is their manual step.

## Plan & report handling

| Plan state at closing | How it lands on the branch | How it's kept off main |
|-----------------------|----------------------------|------------------------|
| **Uncommitted / untracked** (the clean default) | Committed in Step 7, so it is the last commit | `git merge --squash` never staged it — auto-excluded |
| **Already committed** | Already in branch history | `git merge --squash` staged it → `git restore --staged --worktree` drops it from main's commit |

The canonical flow assumes the plan is **uncommitted** at closing (committed last, in Step 7). The already-committed path is the safety net; if committing the spec (Step 3) or the task completion (Step 4) leaves the plan no longer the last commit, **report it — never rewrite history to reorder.**

The **review-task report** (`docs/reviews/<task-slug>-review.md`) is treated exactly like an uncommitted plan, so the squash auto-excludes it and Step 7 commits it in the same last commit as the plan. If the task produced no report, there is nothing to add.

## Post-implementation notes recipe

The `## N. Post-implementation notes` section states, in order:

1. **Opener** — "This spec and its plan (`<plan-path>`) were written before implementation. The core change shipped substantially as designed — &lt;one sentence on what landed as specified&gt;."
2. **Canonical source** — "the canonical implementation is the current state of &lt;the real artifacts: files, configs, tests&gt; on this branch, not this document."
3. **`Notable deltas:`** — a bullet per place the implementation diverged from the design. Each bullet: a **bold headline** naming the delta (and which spec section it reverses), then the *why*, anchored to the **durable artifacts** it touched — the files, classes, config keys, and tests.
4. **Closer** — "For future &lt;area&gt; edits, treat &lt;the real artifacts&gt; as the template; this spec is preserved as a record of the original design intent."

**Never cite commit hashes.** The whole task squash-merges into a single commit on `main` (where this spec lives), so the branch's individual SHAs no longer exist there — a cited hash becomes a dead reference. Anchor every delta to the code artifacts instead; the spec is a record of intent, the code is the source of truth.

## Reverting

```bash
# Undo the squash merge on main:
git checkout main && git reset --hard $PRE_MAIN
# Undo the plan + report commit on the branch (only if Step 7 added it):
git checkout <branch> && git reset --hard $PRE_BRANCH
```

> The Step 9 `.superpowers/sdd` cleanup is **not** covered by this — those files are untracked and gone for good. That is why it runs only after the invariants pass.

## Delegation & tools — quick reference

| Situation | Use |
|-----------|-----|
| Analyze the SDD record (`.superpowers/sdd/`) + commits/diffs — what shipped and the deltas (read-only) | `architect-reviewer` / `code-reviewer` / `Explore` |
| Update the spec file — Status + post-impl notes (writing only, no code review) | `documentation-engineer` |
| Build the spec / squash / plan commit messages | `asapp-draft-commit-msg` skill |
| Locate the spec/plan or understand a commit | `Explore` |

## Hard rules

- **Never push** — no `git push` of any branch, ever.
- **Never rewrite history** — no rebase, no `reset` of existing commits to reorder the plan.
- **Capture `PRE_MAIN` and `PRE_BRANCH` before any mutation** — revert depends on them.
- **The spec lands on main, the parent task flips to `[X]` in `TODO.md`, and the plan and review report never land on main** — verify all before reporting done.
- **One squash commit on main** for the whole task; messages built with `asapp-draft-commit-msg`.
- **Source the spec notes and squash message from `.superpowers/sdd/` + git** — but never copy SDD commit hashes into the spec.
- **Clear `.superpowers/sdd` last** — only after the invariants pass; it is the one non-git-revertible action, and `.gitignore` is preserved.
- **Abort, don't guess** — on a merge conflict or an unresolved spec/plan/branch, stop and report.

## Common mistakes

| Mistake | Fix |
|---------|-----|
| Starting Step 1 before creating the ten tracking tasks | Do Step 0 first — create the tasks, then begin. |
| Plan file ends up on main | Detect its state; `git restore --staged --worktree` it out of the squash, or commit it only after the merge. |
| Review report left uncommitted, or landing on main | Commit it with the plan in Step 7 (last branch commit); untracked at squash → auto-excluded from main. |
| Forgetting to capture revert anchors | Record `PRE_MAIN`/`PRE_BRANCH` in Step 1, before mutating. |
| Auto-resolving a squash merge conflict | `git merge --abort` and report. |
| Forgetting to mark the parent task `[X]` in `TODO.md` | Flip and commit it on the branch in Step 4 — it rides into the squash alongside the spec. |
| Clearing `.superpowers/sdd` before the close verifies, or deleting its `.gitignore` | Clear it only after Step 8 passes; keep `.gitignore`. |

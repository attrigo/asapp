# Generalize close-task's Doc-commit Step Wording

**Status**: Designed

## Context

`asapp-close-task` Step 7 makes the last commit on the task branch: the docs that must stay off
`main`. It is written as though that set is always the plan, the review report, or both.
`SKILL.md:117-123`:

```markdown
### Step 7: Commit the plan and review report as the last branch commit

git add docs/superpowers/plans/<plan> docs/reviews/<task-slug>-review.md   # omit the report path if the task had none
git commit -m "docs(<scope>): add <task> implementation plan and review report"   # drop "and review report" when there is no report
```

Step 6's `git merge --squash` stages nothing untracked, so **every** uncommitted doc is already
excluded from `main` and depends on Step 7 to reach the branch. Step 7 stages two paths. A third
pending doc — a one-off audit, a second report — is then committed nowhere and named nowhere. It
survives as a working-tree leftover, and the next task's Step 1 precondition, *"working tree has no
unrelated uncommitted changes"*, trips over it.

The enumeration has already broken down twice in this repo's history:

| Commit | Subject | What it committed |
|---|---|---|
| `944b5619` | *docs(ai): report files* | 35 review docs |
| `219ddf28` | *docs(gradle): add the SBOM task's review report* | one report, no plan |

The first abandoned the template outright. The second improvised the plan-absent variant — the
template's aside covers only the *report*-absent case.

Three further sites enumerate the same pair: the Step 0 tracking entry (`SKILL.md:39`), the Core
principle's branch-side contract (`SKILL.md:19`), and the `Reverting` block's comment
(`SKILL.md:190`).

## The decision

One term — **the task's pending docs** — in every place that enumerates, and a commit subject that
names what was actually committed. The commands, their paths, and their order are unchanged; this is
a wording change.

A **pending doc** is anything still uncommitted under `docs/` at close. Step 1's precondition already
excludes unrelated changes, and a doc belonging on `main` would have been committed during
implementation — so whatever remains is the task's branch-only paper trail.

## `.claude/skills/asapp-close-task/SKILL.md`

### Core principle, branch side (line 19)

```diff
-- **The task branch** keeps **all its development commits**, with the **plan and the review-task report committed as the last commit**.
+- **The task branch** keeps **all its development commits**, with the **task's pending docs committed as the last commit**.
```

The file's most authoritative statement of what Step 7 produces. Left enumerated, the contract and
the step it governs would disagree.

### Step 0 tracking entry (line 39)

```diff
-7. Commit the plan and review report as the last branch commit (Step 7)
+7. Commit the task's pending docs as the last branch commit (Step 7)
```

### Step 7 title (line 117)

```diff
-### Step 7: Commit the plan and review report as the last branch commit
+### Step 7: Commit the task's pending docs as the last branch commit
```

### Step 7 block (lines 121-123)

```diff
-# plan (+ report) uncommitted: commit them now so they are the last commit
+# pending docs uncommitted: commit them now so they are the last commit
-git add docs/superpowers/plans/<plan> docs/reviews/<task-slug>-review.md   # omit the report path if the task had none
+git add docs/superpowers/plans/<plan> docs/reviews/<task-slug>-review.md   # plus any other pending doc under docs/; omit a path the task never produced
-git commit -m "docs(<scope>): add <task> implementation plan and review report"   # drop "and review report" when there is no report
+git commit -m "docs(<scope>): add the <task> <docs>"   # <docs> names what was committed, e.g. "implementation plan and review report"
```

The two known paths stay spelled out — they are the common case and the skill's freedom tier is
**low**, so nothing prescriptive is vaguened. The `git add` comment carries the generalization,
because a command demonstrating exactly two paths under a title promising any number is what leaves
the third doc behind.

The `-m` template stops enumerating and states its own rule instead, with the old text as the
example. `docs(<scope>): add the <task> <docs>` reproduces `a820de2b` verbatim for the plan-plus-
report case and covers report-only without a second aside.

### Reverting comment (line 190)

```diff
-# Undo the plan + report commit on the branch (only if Step 7 added it):
+# Undo the pending-docs commit on the branch (only if Step 7 added it):
```

Identifies the commit Step 7 created; it must name the same thing Step 7 does. `git reset --hard
$PRE_BRANCH` below it is unchanged and undoes that commit whatever it holds.

## Referrer sweep

`grep -rn "review report\|review-task report\|implementation plan\|Step 7" .claude/ CLAUDE.md docs/superpowers/README.md`

Every referrer to Step 7 or its wording is inside `asapp-close-task`. No rule, agent, or other skill
names the step, its title, or its commit template. The `Step 7` matches in `asapp-release`,
`asapp-refine-task`, and `asapp-review-version` are those skills' own seventh steps.

## Scope

**In scope:** the five sites above in `.claude/skills/asapp-close-task/SKILL.md`, plus `TODO.md:54`.

**Out of scope:**

- **Core principle, main side (line 18)** — *"excludes the plan file and the review-task report"*
  describes what Step 8 verifies with two literal `git cat-file` path checks. Generalizing the prose
  without the checks would overstate them.
- **Step 1's resolve list (line 53) and Step 8's invariants (lines 132-133).** Resolving a pending-doc
  *set* in Step 1 and verifying it path-by-path in Step 8 is a behavior change to the skill's
  resolution and verification phases, not a wording fix. Considered and declined.
- **The `Plan & report handling` section (lines 152-161).** It is specifically about how the plan's
  committed-versus-uncommitted state changes the squash mechanics, and the report's alignment with
  the uncommitted case. Those two files are its subject, not an incidental enumeration.
- **Line 111's squash comment.** States why the review-task report is untracked at that point —
  a fact about the report, not a list of what Step 7 commits.

## Verification

- `grep -n 'add <task> implementation plan and review report\|# plan (+ report)\|Undo the plan + report\|Commit the plan and review report' .claude/skills/asapp-close-task/SKILL.md`
  returns nothing. The phrase *"implementation plan and review report"* survives once, as the
  worked example inside line 123's comment.
- The five generalized sites all read **the task's pending docs** / **pending docs** — one term.
- `git diff` shows no change to any `git checkout`, `git add`, `git commit`, `git restore`,
  `git cat-file`, `git log`, or `git reset` invocation other than the `-m` string on line 123 —
  same commands, same paths, same order.
- Step 7's block still ends `git checkout main`; the step count stays ten.
- `SKILL.md` stays under 500 lines; section order unchanged.
- `git diff --name-only` lists exactly `.claude/skills/asapp-close-task/SKILL.md` and `TODO.md`.
</content>
</invoke>

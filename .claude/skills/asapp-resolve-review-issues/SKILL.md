---
name: asapp-resolve-review-issues
description: >
  Use when a review has already produced issues — logged under a TODO.md task, or written into a report
  under docs/reviews/*.md — and they need to be worked through, fixed, and committed one at a time.
  Triggers: /asapp-resolve-review-issues, resolve review issues, fix the issues I logged under this task,
  work through my review notes, address the review findings, resolve the findings in the review report,
  fix the readiness report, work through a docs/reviews file.
  Do NOT use to perform the review itself (use asapp-review-task or asapp-review-version), to refine or
  decompose a task (use asapp-refine-task), or to implement a brand-new task from a plan (use the SDD flow).
---

# Resolve Review Issues

Work through already-identified review issues and commit the fixes, one issue at a time. Issues come from two sources (the per-issue loop is the same for both):
- **TODO:** bullets logged under a `TODO.md` task.
- **Report:** findings in a `docs/reviews/*.md` review report (`asapp-review-task` or `asapp-review-version`, identical format).

**Core principle:** this skill resolves issues already identified — it does not hunt for new ones.

## Usage

- `/asapp-resolve-review-issues <line-number>` — **TODO:** resolve issues under the task at that line of `TODO.md`.
- `/asapp-resolve-review-issues <docs/reviews/….md>` — **Report:** resolve the findings in that report.
- `/asapp-resolve-review-issues` (no argument) — ask which source (a TODO task vs a report), then proceed.

## Process

### 1. Locate and enumerate

Do all of this up front.

1. **Resolve the source and target** (see *Usage*):
   - **With an argument** — the `TODO.md` task at that line, or the report at that path.
   - **No argument** — ask which source, offering the current task's TODO issues and any `docs/reviews/*.md`.
2. **Get onto a fix branch**, then name it in one line:
   - On `resolve-issues-version-<ver>` — reuse it; it already holds the work.
   - On any other feature / task branch — stay on it; the fixes belong with that task's work.
   - On `main` — `git switch -c resolve-issues-version-<ver> main`, where `<ver>` is the report's `v<ver>`, else the task's `## <ver>` section in `TODO.md`.
3. **Enumerate the issues** — list every un-resolved one in the order it appears in the source; work them in that order, never re-sorted or regrouped.
   - **TODO:** the task's plain nested `- <issue>` bullets — never its guidance bullets.
   - **Report:** the report's un-ticked finding blocks.
4. **Confirm only if in doubt** — if the source, target and issue list are unambiguous, state your read in one line and go straight to Step 2.

### 2. Per-issue loop

For the current issue:

- **a. Triage** — per source:
  - **Report:** run the triage gate (see *Triage gate*).
  - **TODO:** no gate, no print — go straight to 2b. If the issue is genuinely unclear, stop and ask (`AskUserQuestion`) first.
- **b. Explore (delegate)** — dispatch `Explore`:
  - **Scope:** the involved code, and only the slice this issue needs of any supporting doc — never the whole file:
    - **Report:** start from the finding's Where — go straight to those sites rather than rediscovering them.
    - **TODO:** the task's spec (`docs/superpowers/specs/YYYY-MM-DD-<slug>-design.md`), plan (`docs/superpowers/plans/YYYY-MM-DD-<slug>.md`, may not exist), or branch commits (`git log main..HEAD`).
  - **Ask for:** the briefing's raw material (see *Issue briefing*) — the code at the site, the failure and its direct outcome, the rule or standard the code breaks, and the fix approach with what it touches.
- **c. Propose:**
  1. **Print the briefing** — the four parts of *Issue briefing*, built from 2b, *before* any `AskUserQuestion`.
  2. **Then propose** one or several solutions with a recommended one, via `AskUserQuestion` — recommended first, one line of trade-off each. Fold in any Watch note on the finding.
- **d. Apply** — route on whether the fix changes runtime behavior:
  - **Behavioral** (bug fix, logic change, new validation or edge case): drive `superpowers:test-driven-development` from the main context — it owns the RED→GREEN→refactor loop and delegates the failing test and the production fix to specialist subagents.
  - **Non-behavioral** (docs, comments, formatting, config without logic, pure rename): dispatch the most specific specialist subagent to apply the change directly.
- **e. Review & approve** — show the user what changed (diff/summary). Wait for approval; if changes are requested, iterate (back to 2d) before committing.
- **f. Mark done:**
  - **TODO:** remove the `- <issue>` line.
  - **Report:** tick `- [ ]` → `- [x]` and append the outcome bullet — `**Deferred:**`, `**Ignored:**`, else `**Applied:**`.
- **g. Commit** — stage every change this issue made, including `TODO.md`. Build the message with the `asapp-draft-commit-msg` skill, then commit.

### 3. Wrap-up

- Summarize every issue's outcome in source order, in three groups: Resolved (issue → commit), Deferred, Ignored (one line each).
- The report's ticks and outcome bullets stay uncommitted in the working tree.
- Point at what's next: `asapp-close-task` for a task branch, `asapp-release` for a version — both the user's manual step.

## Reference

### Triage gate

Report source only, at 2a. Print the finding, then ask the route.

#### 1. The finding, as written

```markdown
#### <n>/<total> · <finding title>

<its own bullets, copied verbatim from the report>
```

Add nothing: no exploration, no restatement, no view on the fix.

#### 2. The route (`AskUserQuestion`), recommended first

| Route | Runs | Note |
|-------|------|------|
| Apply | 2d–2g | the finding's Fix is the solution; honor any Watch |
| Explore | 2b–2g | — |
| Defer | 2f–2g | 2g only if something changed |
| Ignore | 2f | — |

Recommend in this order:

- the finding carries a Defer note → Defer
- Where pins an exact site and Fix names a concrete, local change → Apply
- otherwise → Explore

Effort is not the signal — it rates the fix's size, not the cost of understanding it.

### Issue briefing

Printed once per issue at 2c.1, built from 2b's exploration. Four parts, in order:

```markdown
#### <n>/<total> · <issue title>

**What we have:** <the site, then the real code at it>

**What's wrong:** <the defect and its direct outcome>

**Why:** <the rule or standard the code breaks, and where the repo already follows it>

**How to fix:** <the recommended approach as a code sketch, then what it touches>
```

| Part | Cap |
|------|-----|
| What we have | ≤10 lines of real code, quoted from the file |
| What's wrong | ≤30 words |
| Why | ≤30 words |
| How to fix | ≤10 lines of sketch — the key lines, never a full patch — plus ≤20 words of blast radius |

- **Never lift it from the finding** — the finding only aimed the exploration; the briefing is what exploration confirmed.
- **Why cites in this order:** a `.claude/rules/*` rule, then a named standard, then a URL only when neither exists.
- **Name every subject outright** — never "the two writes", "it", "the above". No sentence may require scrolling up to parse.
- **Stop at the direct, verified outcome** — no chained downstream consequences, no speculation.
- **No em dashes in the briefing** — colon after each label, plain sentences inside.

## Delegation

| Situation | Use |
|-----------|-----|
| Locate / understand code for an issue | `Explore` |
| Apply a production-code fix | `spring-boot-developer` |
| Add or adjust tests | `test-automator` |
| CI / Docker / observability fix | `devops-engineer` |
| README / changelog / docs fix | `documentation-engineer` |
| Agent / rule / skill (`.claude/`) fix | `claude-docs-maintainer` |
| Safe rename, reformat, inspections | IntelliJ MCP |
| Build the commit message | `asapp-draft-commit-msg` skill |
| A fix triggers a test failure or bug | `superpowers:systematic-debugging` |

## Guardrails

- **Never modify a file before the user chooses the fix** — Apply at the triage gate, or an approved solution at 2c.
- **One issue at a time** — never analyze or propose a fix ahead of the current issue.
- **Never commit onto `main`** — get onto a fix branch first (Step 1).
- **One issue → one commit** — the fix and its `TODO.md` change together, nothing else.
- **Never stage `docs/reviews/`** — `asapp-close-task` relies on the report being uncommitted to keep it off `main`.
- **Do not close the task, merge, or release** — that is the user's separate manual step.

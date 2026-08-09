# Right-Size the Review Skills' Delegation to the Change

**Status**: Designed

## Context

Both review skills dispatch the same way regardless of what changed.

`asapp-review-task` Step 2 always dispatches `code-reviewer` and adds `security-auditor` for
security-relevant files. That half is already right — the sibling roster spec landed it. What it
still lacks is a floor: a one-line wording fix in a `SKILL.md` gets the same subagent dispatch as a
forty-file authentication change, and its Guardrails forbid anything else — *"Delegate all reviewing
to subagents"*.

`asapp-review-version` Step 3 dispatches **one reviewer per theme as its dominant-concern
specialist**, drawn from a seven-row table. Five of those rows name agents from outside the Review
phase: `documentation-engineer`, `test-automator`, `devops-engineer`, `api-designer`,
`claude-docs-maintainer`. Two problems follow.

1. **The roster is write-shaped.** Four of the five hold `Write` and `Edit` (`api-designer` is the
   exception). The skill's own guardrail is *"never change code, commit, push, tag, or merge"*, and
   it enforces that by dispatching agents equipped to do all of it. The two agents authored for
   review, and carrying no write tools, are the two the specialist table pushes aside.
2. **The specialists no longer add routing.** The sibling rule-routing spec deleted
   `code-reviewer`'s hand-written glob table on the grounds that reading a changed file loads the
   rules governing it. That admits all nineteen rules, `skill-authoring.md` and `testing-core.md`
   and `maven.md` included. Whatever a theme specialist knew about its surface, `code-reviewer` now
   loads on the read it performs anyway.

Step 3 also carries an open-ended second-dispatch hatch — *"or the first pass flags something
deeper"* — with no stated bound. Between that and the missing floor, the Warning at `TODO.md:49`
records what over-dispatch has already cost once: an earlier attempt dispatched domain specialists
and burned minutes and heavy tokens on a two-line diff.

## The decision

**Give `asapp-review-task` a floor. Give `asapp-review-version` a uniform read-only roster.**

Two independent changes, one per skill. They are not symmetric, and the asymmetry is the point:

- A **task** gate runs many times per version, mostly on small diffs, and its cost is felt every
  time. It gets an escape.
- A **release** gate runs once, on everything that shipped. It keeps delegating every theme; what
  changes is *who* it delegates to.

### What is knowingly lost

**The specialist lenses, as separately-prompted roles.** `test-automator`'s fixture judgment and
`api-designer`'s contract-shape judgment reach the review only through `testing-core.md` and
`rest.md`, loaded on read. A rule states what must hold; a specialist body also carries taste. The
taste goes.

**The unbounded escalation.** `or the first pass flags something deeper` was the one path to a
third opinion on a theme that turned out worse than it looked. Closing it means a bad theme gets one
reviewer, and the developer catches the rest at the report.

**Inline review is unverified by a second context.** On a trivial diff, the reviewer is the same
agent that just resolved the task and holds its assumptions. Accepted: the gate admits only changes
where those assumptions cannot cause a defect — no production logic, nothing security-bearing.

### Alternatives rejected

- **A pure numeric gate** (`≤2 files and ≤20 lines → inline`). Cheapest to apply and impossible to
  rationalize around, but blind to what changed: it waves through a five-line filter-chain edit and
  dispatches a reviewer at a four-file README pass. Size is a sanity bound, not the test.
- **A pure judgment gate**, no numbers — matching the high-freedom tier `skill-authoring.md` assigns
  to review skills. Rejected because the failure this task fixes was a judgment failure, and a gate
  whose only content is *"skip when you can judge it yourself"* is one an agent under load talks
  itself past. The size bound is the part that cannot be argued with.
- **Extending the trivial gate to `asapp-review-version` per theme.** A release gate is the last
  read before a tag; a theme that looks trivial from its file list is exactly the one worth a second
  context. Rejected in favor of keeping the release gate absolute.
- **Keeping `claude-docs-maintainer` for `.claude/**` themes.** The most defensible exception —
  it owns those surfaces and carries the secure-authoring audit duty. Rejected because it holds
  `Write` and `Edit` inside a read-only gate, and `agent-authoring.md`'s *Secure authoring* section
  loads for `code-reviewer` on any read of a changed agent body. One exception would also cost the
  uniform vocabulary both skills now share.

## `asapp-review-task`

| Line | Element | Change |
|---|---|---|
| 46–48 | Step 1 scope command | `--name-only` becomes `--stat`, so one command serves both the anchor and the gate's line counts. |
| — | Step 1, new item 3 | **Right-size** — the diff is **trivial** only when all three hold: no production logic changed (docs, comments, config values, mechanical renames), nothing security-bearing touched, and it fits in one read (~≤3 files, ≤30 changed lines). When in doubt, it is not trivial. |
| 49 | Step 1, *Confirm only if in doubt* → item 4 | Becomes **State your read** — task, scope, and verdict (`trivial → review inline` / `dispatch`) in one line; proceed unless the user objects. Stop to ask only when the task match is genuinely ambiguous. |
| 51 | Step 2 title | *Delegate the review, then consolidate* becomes *Run the review, then consolidate*. Delegation is now one of two branches, not the step. |
| 53–56 | Step 2 dispatch | Opens with the branch: **Trivial** — review it inline; dispatch nothing. **Otherwise** — the existing parallel dispatch, `code-reviewer` always and `security-auditor` only for security-relevant files. |
| 60 | Step 2, *Tell each reviewer to:* | Becomes **Every review — inline or delegated — must:**, holding the inline path to the same four instructions. |
| 66 | Step 2 consolidation | `dedupe overlaps, merge into one list, assign each an ID` becomes `merge into one list (deduping where reviewers overlap) and assign each an ID` — with one reviewer now the norm, the two-reviewer wording is vestigial. |
| 122 | Guardrails | `Delegate all reviewing to subagents — keep the main context clean` becomes `Never review inline unless Step 1 called the diff trivial — keep the main context clean`. The guardrail states the boundary; Step 1 states the test and Step 2 the branches. |
| — | Unchanged | Step 0's tracking tasks, Step 2's *Depth* paragraph, Steps 3–6, the Delegation table. |

The gate is self-consistent with the security branch: a trivial diff touches nothing
security-bearing, so `security-auditor` is never the reviewer skipped.

## `asapp-review-version`

| Line | Element | Change |
|---|---|---|
| 60 | Step 2 theme list | Gains `flagging any security-relevant theme`, so the dispatch count is visible before fan-out. |
| 64 | Step 3 dispatch | `one reviewer per theme … as its dominant-concern specialist (see Delegation)` becomes `one code-reviewer per theme`. Scoping, the one-gather rule, and the `≤5 running at once` cap stand. |
| 69 | Step 3 lens | `The theme's specialty — security, tests, API, docs, CI, etc.` becomes `The theme's own concern — tests, API, docs, CI, and so on`. It now reads as a lens the reviewer applies, not a hand-off; security moves to the clause below. |
| 71 | Step 3 escalation | Becomes **Add `security-auditor`** as a second reviewer only for a security-relevant theme (auth / security config, JWT / token handling, filter chains, crypto, secrets, new endpoints). Otherwise one reviewer per theme. The trigger is now `asapp-review-task`'s, word for word, and the `flags something deeper` hatch is gone. |
| 123–140 | Delegation | Two tables (7 specialist rows, 4 support rows) collapse to one, matching `asapp-review-task`'s shape: `code-reviewer` for every theme and the seam pass, `security-auditor` for a security-relevant theme, then `Explore`, `superpowers:requesting-code-review`, `superpowers:systematic-debugging`. The `Dispatch each theme's reviewer as its dominant-concern specialist` lead-in goes with them. |
| 147 | Guardrails | Unchanged. `Delegate all reviewing to subagents` stays absolute here — that is the asymmetry with `asapp-review-task`, and it is deliberate. |
| — | Unchanged | Step 0, Step 3's *Depth* and *Mechanical themes* paragraphs, Step 4's seam pass and its `≥2 themes touch production code` gate, Steps 5–7. |

The dispatch count barely moves — still one reviewer per theme. What moves is fit: the gate becomes
read-only by construction, and both skills describe their roster in one vocabulary.

## Referrer sweep

| File | Line | Edit |
|---|---|---|
| `docs/superpowers/README.md` | 22 | `The 13-agent roster` becomes `The 12-agent roster`. The listing below it already shows twelve; the count was left stale by the `architect-reviewer` removal. |
| `TODO.md` | 45 | Mark the subtask `- [X]` and drop its three now-resolved Notes and the Warning. |

No agent body changes, and no other skill names a review specialist in a review context.
`asapp-resolve-review-issues`, `asapp-close-task`, and `asapp-prepare-version` dispatch the same
five agents for **fix and write** work, which is what they are for.

## Scope

**In scope:** the two skills, the README count, and the `TODO.md` line above.

**Out of scope:**

- **The sibling subtasks at `TODO.md:50–67`** — reserving `code-reviewer` for judging quality,
  capping finding verbosity, trimming chat output, auto-generating the findings report, emitting
  commit-sized outcomes, and generalizing close-task's doc-commit step. Line 50 in particular owns
  `code-reviewer`-for-context in `asapp-resolve-review-issues` and `asapp-close-task`; neither file
  is touched here.
- **`asapp-review-task`'s report / `TODO.md` routing** (Step 5). The subtask at `TODO.md:60` drops
  the `TODO.md` write and gives both skills one report shape; this task changes who reviews, not
  where findings land.
- **The agent bodies.** Nothing in `code-reviewer` or `security-auditor` needs to change for either
  skill to dispatch them this way, and the five dropped specialists keep their roles elsewhere.
- **Read depth.** Both skills already set it explicitly and both keep their wording; scaling depth
  to diff size is not proposed — the trivial gate scales *dispatch*, not depth.
- **Archived specs** under `docs/superpowers/specs/v0.4.0/**` — a historical record of what was
  designed then.
- **A `TODO.md` Decisions entry.** This is not a keep-as-is outcome — a table is deleted and a gate
  is added — so this spec is the record.

## Verification

- `grep -n "documentation-engineer\|test-automator\|devops-engineer\|api-designer\|claude-docs-maintainer" .claude/skills/asapp-review-version/SKILL.md`
  returns nothing.
- Both skills' Delegation tables resolve to agents that exist in `.claude/agents/`, and both name
  the same security trigger in the same words.
- Every agent either skill can dispatch as a reviewer is read-only: no `Write`, no `Edit`.
- The trivial gate is stated three times in `asapp-review-task` and no more — as the test (Step 1),
  the branch (Step 2), and the boundary (Guardrails). Step 1's read line names the verdict but does
  not restate the test.
- Neither skill instructs an inline review without also binding it to the same finding shape and
  depth as a delegated one.
- Both files keep `skill-authoring.md`'s section order and stay well under 500 lines.
- `docs/superpowers/README.md`'s agent count matches the file count in `.claude/agents/`.
- No build or test impact: no production code, resources, or build files change.

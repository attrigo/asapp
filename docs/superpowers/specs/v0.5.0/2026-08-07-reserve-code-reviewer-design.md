# Reserve `code-reviewer` for Judging Code Quality

**Status**: Designed

## Context

`code-reviewer` is a review-phase agent. Its body is written around one job: read a diff, judge it
against the rules governing each changed path, and emit a severity-classified finding with a
citation. Its delivery notification is a finding count. It holds no write tools because it is not
meant to produce anything but findings.

Two skills dispatch it for a different job.

- **`asapp-resolve-review-issues`** Step 2b sends a subagent to "investigate the involved code and
  only the relevant slice of any spec/plan", and closes with *"Pick the most specific agent."* That
  sentence resolves roster-wide, and the Delegation table below it offers `` `Explore`, or
  `code-reviewer` `` — so the most specific match for *understand this code* reads as the reviewer.
- **`asapp-close-task`** Step 2 analyzes the SDD record and the branch commits to find where the
  implementation diverged from the design, and delegates to *"a read-only review agent
  (`code-reviewer` / `Explore`)"*. Nothing is being reviewed; the output feeds the spec's
  post-implementation notes and the squash message.

Both want a read-only summary of what exists. Neither wants a severity-classified finding list, and
neither acts on one. Dispatching a rule-citing reviewer to produce a summary pays for a review pass
that is then discarded, and it blurs what the roster's one review agent is for.

`Explore` is the agent authored for this: read-only, excerpt-based, reports the conclusion rather
than the file dumps. Both call sites already name it — as an alternative rather than the answer.

## The decision

**Name `Explore` at both context-gathering dispatch sites, and drop `code-reviewer` from both.**

The change is at the dispatch sites, not in the agent. Four line edits across two `SKILL.md` files.

Where these skills dispatch a subagent to *apply* something, the roster-wide "most specific
specialist" wording stays — `asapp-resolve-review-issues` Step 2e in particular. Applying a fix does
need the specialist; understanding the code does not.

### What is knowingly lost

**A reviewer's eye during context gathering.** `code-reviewer` reading an issue's code would
sometimes notice a second problem next to the one being fixed. Accepted: `asapp-resolve-review-issues`
already guards against that — *"this skill resolves issues already identified — it does not hunt for
new ones"* — so the extra finding was out of scope where it surfaced, and hunting for new ones is
`asapp-review-task`'s job.

### Alternatives rejected

- **Encode the boundary in `code-reviewer`'s body** ("decline context-gathering dispatches"). A body
  is a static system prompt with no view of why it was called, and `agent-authoring.md` scopes bodies
  to the role, not to caller behavior. The dispatch sites are where the choice is actually made.
- **Leave the Delegation tables listing both and fix only the Process steps.** Half the problem is
  in the tables: the reviewer stays offered as a valid pick for *locate / understand code*, which is
  the wording that produced the wrong dispatch.
- **Add a Guardrail — "never dispatch `code-reviewer` for context".** Restates a Process step as a
  never, which `skill-authoring.md` forbids: Guardrails are damage-causing invariants, and naming
  the right agent in the step already settles it.

## `asapp-resolve-review-issues`

| Line | Element | Change |
|---|---|---|
| 58 | Step 2b, *Explore (delegate)* | *dispatch a subagent to investigate …* becomes *dispatch `Explore` to investigate …*, and the trailing *Pick the most specific agent.* sentence goes. The rest of the bullet — the relevant-slice scoping, the TODO-mode commit range, and `a concise findings report — not file dumps` — stands, as does the report-mode sub-bullet under it. |
| 98 | Delegation, row 1 | `` \| Locate / understand code for an issue \| `Explore`, or `code-reviewer` \| `` becomes `` \| Locate / understand code for an issue \| `Explore` \| ``. |
| 66–67 | Step 2e, *Apply* | **Unchanged.** Both branches keep `the most specific specialist subagent`; a fix needs the specialist. |
| — | Unchanged | Step 1, the rest of the per-issue loop, Step 3, Sources, Guardrails. |

## `asapp-close-task`

| Line | Element | Change |
|---|---|---|
| 75 | Step 2, third bullet | `` Delegate by default to a read-only review agent (`code-reviewer` / `Explore`) `` becomes `` Delegate by default to `Explore` ``. The `do it inline only for a trivially small diff (e.g. a one- or two-file change)` escape stands. |
| 189, 192 | Delegation | The two rows that now both resolve to `Explore` collapse into one: `` \| Analyze the SDD record + commits/diffs, locate the spec/plan (read-only) \| `Explore` \| ``, kept in row 1's position. The table goes from four rows to three. |
| — | Unchanged | Steps 0–1 and 3–10, *Plan & report handling*, *Post-implementation notes recipe*, *Reverting*, Guardrails. |

Step 2's phrase *"a read-only review agent"* goes with the parenthetical. `Explore` is read-only but
is not a review agent, and after this change the step is not reviewing anything.

## Referrer sweep

| File | Line | Edit |
|---|---|---|
| `TODO.md` | 46 | Mark the subtask `- [X]` and drop its three now-resolved Notes. |

No other skill or agent dispatches `code-reviewer` for context. `asapp-review-task` (58, 115) and
`asapp-review-version` (64, 91, 125) dispatch it to judge quality, which is what it is for. The four
agent bodies that name it — `security-auditor`, `spring-boot-developer`, `test-automator`,
`claude-docs-maintainer` — all name it as the thing that reviews their output.

## Scope

**In scope:** the four edits across the two `SKILL.md` files, and the `TODO.md` line above.

**Out of scope:**

- **`code-reviewer.md`.** The body already describes only diff review; there is nothing to narrow.
- **The review skills.** `asapp-review-task` and `asapp-review-version` dispatch the reviewer
  correctly and are untouched. Their sibling subtask at `TODO.md:45` already landed.
- **The sibling subtasks at `TODO.md:50–68`** — finding verbosity, chat output, the auto-generated
  findings report, the resolve skill's applied-note line, commit-sized outcomes, and close-task's
  doc-commit wording. This task changes who is dispatched, not what comes back or how it is shown.
- **Adding a trivial-diff escape to `asapp-resolve-review-issues` Step 2b.** `asapp-close-task`
  Step 2 has one; the resolve loop does not, and giving it one is a right-sizing decision, not a
  routing one.
- **Archived specs** under `docs/superpowers/specs/v0.4.0/**`.

## Verification

- `grep -rn "code-reviewer" .claude/skills/` returns exactly five lines — `asapp-review-task` 58 and
  115, `asapp-review-version` 64, 91, and 125 — every one of them a quality-judgment dispatch.
- Neither edited skill names an agent for context gathering other than `Explore`.
- `asapp-resolve-review-issues` Step 2e still reads roster-wide; the fix dispatch is unnarrowed.
- `asapp-close-task`'s Delegation table has no two rows resolving to the same agent.
- Both files keep `skill-authoring.md`'s section order, and every agent either table names exists in
  `.claude/agents/`.
- No build or test impact: no production code, resources, or build files change.

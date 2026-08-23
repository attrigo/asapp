# Add a Triage Gate Before resolve-review-issues Explores an Issue

**Status**: Designed

## Context

Step 2b of `asapp-resolve-review-issues` dispatches `Explore` for every issue in the source,
unconditionally. It is the loop's most expensive step: a subagent reads the code at the site, the
covering tests, and the governing rule, then returns the briefing's raw material. It runs before the
developer has said whether the issue is worth fixing at all.

Two cases pay for exploration they do not need.

- **The finding is already specified.** A report finding that pins `Where: JwtService.java:88` and
  states a concrete `Fix` has had the work done once, by a reviewer who held the whole file in
  context. Exploring re-derives it.
- **The finding was never going to be fixed.** The reviewer's own **Defer** note says it is out of
  scope. The exploration is spent in full, then discarded at the first question that asks the
  developer anything.

`.claude/rules/review-report.md:52` already names the step the skill is missing: *"**Defer** is a
suggestion, not a routing instruction — its presence says the reviewer judges the finding out of
scope here; the developer settles it at fix time."* There is nowhere in the loop where the developer
settles it. The first decision point is Step 2c, after exploration has been paid for.

The position was reserved on purpose. `2026-08-07-trim-review-resolve-output-design.md` deleted Step
2a's print, recording that this task *"deliberately re-adds a print at that position for its triage
gate, which answers a different question — is this worth doing? rather than how do we fix it? — and
reads the finding as written rather than the code."*

### Why the gate is report-mode only

TODO-mode issues are the developer's own hand-logged bullets. Logging one **is** the decision to fix
it, so the two routes that discard an issue contradict why it was written down. A hand-logged bullet
also carries no **Where** and no **Fix**, so the route that skips exploration has nothing to act on.
All three non-default routes are unreachable in TODO mode, which would leave a gate whose only
outcome is the path it already takes. TODO mode keeps today's flow unchanged.

This makes the gate a **third** axis on which the two sources differ, alongside where issues are read
and how a resolved issue is marked done. The `### Sources` intro that says only two things differ
becomes wrong and is corrected.

## The decision

**Step 2a becomes a triage gate in report mode: print the finding as written, ask for one of four
routes, and dispatch exploration only for *Explore*.**

One file changes: `.claude/skills/asapp-resolve-review-issues/SKILL.md`. No rule changes; no new
file.

### The four routes

| Route | Effect | Cost |
|---|---|---|
| **Apply** | skip (b) and (c) — go to (d) with the finding's **Fix** as the solution, honoring any **Watch** note | no exploration, no briefing |
| **Explore** | today's path — (b) → (c) → (d) | unchanged |
| **Defer** | skip (b)–(e) and (g); at (f) mark done with a **Deferred:** bullet | nothing |
| **Ignore** | skip (b)–(e) and (g); at (f) mark done with an **Ignored:** bullet | nothing |

**Apply** does not weaken the loop's safety. It still routes through (d)'s behavioral split
(behavioral → `superpowers:test-driven-development`, non-behavioral → the specialist subagent), and
it still stops at (e) for the developer's diff review before anything is committed. What it skips is
the *derivation* of a fix the finding already states, not the review of what that fix did.

**Apply** costs no extra stop: the gate replaces the proposal question at (c), which that path skips.
**Explore** adds one, and it is the path where the developer chose to look anyway. **Defer** and
**Ignore** remove every remaining stop for that issue.

### Which route is recommended

`AskUserQuestion` lists the recommended route first. In order:

1. the finding carries a **Defer** note → **Defer** — the reviewer already judged it out of scope,
   and this is the fix-time moment `review-report.md:52` points at
2. **Where** pins an exact site and **Fix** names a concrete, local change → **Apply**
3. otherwise → **Explore**

The report's **Effort** column is deliberately not the signal. It rates the size of the fix, not the
cost of understanding it: a finding can be effort M and still be fully specified, while a vague
finding with a one-line fix still needs the code read. Specificity of **Where** and **Fix** is what
determines whether exploration can be skipped.

### The intro is the finding, never a summary of it

The gate prints the finding's title and its own bullets — **Where / What / Fix / Watch / Defer** —
copied from the report. Nothing is added: no exploration, no restatement, no opinion on the fix.

This is what keeps it from colliding with (c)'s briefing. The two answer different questions from
different sources: **the intro is the finding as written, the briefing is what exploration
confirmed**. On the Explore path both print, and they share no sentence.

No new caps are needed. `review-report.md` already caps every field a finding carries (title 10
words, **What** 30, **Fix** 20, **Watch** 20, **Defer** 20), so a verbatim echo is bounded at roughly
a hundred words by construction.

### Everything ticks

Every decided finding ticks `- [x]` and gains exactly one outcome bullet, one line, appended last:

| Route | Bullet |
|---|---|
| Apply, or Explore through to a fix | `- **Applied:** <what changed>` |
| Defer | `- **Deferred:** <why not now>` |
| Ignore | `- **Ignored:** <why not at all>` |

Un-ticked keeps meaning *still open*, so Step 1's enumeration rule (`skip any already ticked`) is
unchanged and a later run never re-litigates a decision already made. The ticked block keeps its
outcome bullet, so the report stays the durable record of what was decided and why.

The three bullets are specified one-line from the start. The sibling subtask at `TODO.md:56` caps the
existing **Applied:** bullet the same way; that task brings **Applied:** into line, this one does not
touch it.

### What is knowingly accepted

- **The `#### <n>/<total>` header prints twice on the Explore path** — once at the gate, once on the
  briefing. Accepted: an exploration round-trip sits between them, and the counter is the developer's
  only position marker in the loop, so the re-anchor earns its line.
- **A deferred finding is never re-offered.** It ticks, so a later run skips it. Accepted: the
  developer's explicit call. The **Deferred:** bullet on the ticked block is the record; re-asking a
  settled question is what the tick exists to prevent.
- **Apply trusts the finding's Fix without verifying it against the code first.** Accepted: (e)'s
  diff review is the gate that catches a wrong fix, and it already exists.

### Alternatives rejected

- **Gate only findings that look simple; send complex ones straight to Explore.** Backwards. The
  cheap exit matters most exactly where exploration is most expensive — deferring a complex finding
  is the single largest saving the gate can produce, and this variant removes it.
- **Batch-triage every finding up front at Step 1.** Fewest stops on a large report, but it breaks
  the *one issue at a time* guardrail, and Step 1's job is to enumerate without analyzing.
- **Run the gate in TODO mode too.** Rejected by the developer: a hand-logged bullet is a decision to
  fix, already made. See *Why the gate is report-mode only*.
- **Leave a deferred finding un-ticked so a later run re-offers it.** Rejected by the developer in
  favor of *everything ticks* — a settled decision should not come back.
- **Promote deferred findings into `TODO.md` as new logged bullets.** Nothing would be lost, but the
  flow would edit a second file and need a placement decision per issue, and the ticked report block
  already holds the record.
- **Drop Ignore and keep only Defer.** They differ in what they assert — *not now* versus *not at
  all* — and both are one line in the report. Collapsing them loses the distinction at no saving.
- **Print a one-line "about to change X" before Apply edits.** Offered and declined: (e) shows the
  actual diff, which is strictly better than a prediction of it.
- **Use the report's Effort column to pick the recommended route.** See *Which route is recommended*.
- **Add the routes to `.claude/rules/review-report.md`.** The rule defines the report's *format*;
  routing is the resolving skill's behavior, and the **Applied:** bullet already lives in the skill's
  `### Sources`. Its two siblings join it there.

## `.claude/skills/asapp-resolve-review-issues/SKILL.md`

### Step 2, the per-issue loop

| Line | Element | Change |
|---|---|---|
| 52 | **a. Understand** | Renamed **a. Triage** and replaced by a lead line plus one nested bullet per source, matching the shape (c) and (d) already use: *"per source:"* → *"**Report:** run the triage gate (see *Triage gate*). Only **Explore** continues to (b)."* / *"**TODO:** no gate, no print — go straight to (b). If the issue is genuinely unclear, **stop and ask** (`AskUserQuestion`) first."* |
| 53–54 | **b. Explore (delegate)** | **Unchanged.** It now runs only on the Explore route; the route table states that, so the step does not restate it. |
| 55–57 | **c. Propose** | **Unchanged.** Skipped on the Apply route by the route table. |
| 58–60 | **d. Apply** | **Unchanged** — the behavioral / non-behavioral split applies identically on both fix routes. |
| 61 | **e. Review & approve** | **Unchanged.** It is what makes Apply safe. |
| 62 | **f. Mark done** | **Unchanged** — it already defers to *Sources* for the edit, and *Sources* is where the three outcomes are defined. |
| 63 | **g. Commit** | **Unchanged.** Skipped for Defer and Ignore by the route table. |

### Step 3, wrap-up

| Line | Element | Change |
|---|---|---|
| 67 | first bullet | *"Summarize what was resolved: issue → commit, in order."* becomes *"Summarize every issue's outcome in source order, in three groups: **Resolved** (issue → commit), **Deferred**, **Ignored** (one line each)."* |
| 68 | second bullet | **Unchanged.** |

### `### Sources`

| Line | Element | Change |
|---|---|---|
| 74 | intro | *"Only two things differ between the sources — **where issues are read** and **how a resolved issue is marked done**."* becomes *"Only three things differ between the sources — **where issues are read**, **whether the triage gate runs**, and **how a resolved issue is marked done**."* |
| 79 | Report row, mark-done cell | *"…appending an **Applied:** bullet as last bullet summarizing what was applied"* becomes *"…appending one outcome bullet last (see below)"* |
| 78 | TODO row | **Unchanged.** |
| 81–82 | the two bullets | **Unchanged**, with the outcome-bullet table added after them. |

The outcome-bullet table added under `### Sources`:

````markdown
- **Report outcome bullet** — exactly one, appended last, one line:

| Route | Bullet |
|-------|--------|
| Apply, or Explore through to a fix | `- **Applied:** <what changed>` |
| Defer | `- **Deferred:** <why not now>` |
| Ignore | `- **Ignored:** <why not at all>` |
````

### The new `### Triage gate` reference block

Added under `## Reference`, before `### Issue briefing` — the order the loop runs them in.
`skill-authoring.md` reserves `## Reference` for skill-specific output formats, which is what this
is.

````markdown
### Triage gate

Report mode only, at Step 2a. Print the finding, then ask the route.

**1. The finding, as written:**

```markdown
#### <n>/<total> · <finding title>

<its own bullets — Where / What / Fix / Watch / Defer — copied from the report>
```

Add nothing: no exploration, no restatement, no view on the fix. The intro is the finding as
written; (c)'s briefing is what exploration confirmed.

**2. The route** (`AskUserQuestion`), recommended first:

| Route | Effect |
|-------|--------|
| **Apply** | skip (b) and (c) — go to (d) with the finding's **Fix** as the solution, honoring any **Watch** note |
| **Explore** | the full path — (b) → (c) → (d) |
| **Defer** | skip (b)–(e) and (g); at (f) mark done with the **Deferred:** bullet |
| **Ignore** | skip (b)–(e) and (g); at (f) mark done with the **Ignored:** bullet |

Recommend in this order:

- the finding carries a **Defer** note → **Defer**
- **Where** pins an exact site and **Fix** names a concrete, local change → **Apply**
- otherwise → **Explore**

**Effort** is not the signal — it rates the fix's size, not the cost of understanding it.
````

### `## Guardrails`

| Line | Element | Change |
|---|---|---|
| 128 | first guardrail | *"**Never modify a file until the user approves** the chosen solution for the current issue."* becomes *"**Never modify a file before the user chooses the fix** — **Apply** at the triage gate, or an approved solution at (c)."* |
| 134 | one-issue-one-commit | **Unchanged** — still true, and the route table already says Defer and Ignore skip (g). |
| — | others | **Unchanged.** |

### Frontmatter

**Unchanged**, deliberately. *"Both share one per-issue flow"* stays accurate — the loop is the same
and report mode adds one step to it — and `skill-authoring.md` forbids enumerating process steps in a
description, which drives skill selection.

## Referrer sweep

`grep -rn "Understand\|triage\|Applied:\|mark-done" .claude/ CLAUDE.md docs/superpowers/README.md`
returns hits only inside `asapp-resolve-review-issues/SKILL.md` (the lines above) plus unrelated
prose in `test-automator.md`, `security-auditor.md`, and `devops-engineer.md`. No agent, rule, or
sibling skill constrains Step 2a or the mark-done edit.

`.claude/rules/review-report.md` needs no change: it already specifies the **Defer** field and states
that the developer settles it at fix time. This design supplies the missing fix-time moment.

| File | Line | Edit |
|---|---|---|
| `TODO.md` | 50 | Mark the subtask `- [X]` and drop its five Notes (51–55), now superseded by this spec. |

## Scope

**In scope:** one renamed and rewritten step, two edited bullets, one edited table cell, one edited
guardrail, one added outcome-bullet table, one added `### Triage gate` reference block — all in
`.claude/skills/asapp-resolve-review-issues/SKILL.md` — plus the `TODO.md` line above.

**Out of scope:**

- **TODO mode.** Byte-identical behavior. See *Why the gate is report-mode only*.
- **`.claude/rules/review-report.md`.** Byte-identical.
- **The `### Issue briefing` block.** Untouched; it is what runs after the gate on the Explore route.
- **The `Applied:` bullet's length** — `TODO.md:56`.
- **Commit-sized review outcomes** — `TODO.md:59`.
- **Both review skills.** Neither writes or reads a route; findings are written unchecked as today.
- **Step 1.** Enumeration, branch selection, and context gathering are unchanged — the gate is
  per-issue, inside the loop.

## Verification

- Step 2a is titled **Triage**, names both modes, and routes TODO mode straight to (b) with no gate.
- `### Triage gate` exists under `## Reference`, sits before `### Issue briefing`, and is the only
  place the four routes and the recommendation order are stated — no Process step restates them.
- The gate's print instruction says the finding is copied and nothing is added; it contains no cap
  numbers of its own.
- Defer and Ignore are documented as skipping (b)–(e) and (g), so neither can produce a commit.
- `### Sources` says **three** things differ; its Report row points at the outcome-bullet table, and
  that table lists exactly three bullets, each specified as one line.
- The TODO row of `### Sources` is byte-identical, and no step adds a Defer or Ignore path for TODO
  mode.
- Step 3's first bullet names all three groups; the second bullet is unchanged.
- Guardrail 128 names **Apply** at the gate as an approval path; guardrails 129–135 are unchanged.
- `.claude/rules/review-report.md` and both review skills are absent from `git diff --name-only`.
- The file keeps `skill-authoring.md`'s section order and stays well under 500 lines (136 → ~175).
- `TODO.md:50` is `- [X]` and its Notes are gone; the sibling subtasks at 56, 59 and 62 are
  untouched.
- No build or test impact: no production code, resources, or build files change.

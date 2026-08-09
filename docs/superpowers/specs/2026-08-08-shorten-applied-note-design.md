# Hold resolve-review-issues' Applied Note to One Line

**Status**: Designed

## Context

When `asapp-resolve-review-issues` finishes an issue in report mode, Step 2f appends one outcome
bullet to the finding in `docs/reviews/*.md`. The `### Sources` block defines its shape:

```markdown
- **Report outcome bullet** — exactly one, appended last, one line:

| Route | Bullet |
|-------|--------|
| Apply, or Explore through to a fix | `- **Applied:** <what changed>` |
| Defer | `- **Deferred:** <why not now>` |
| Ignore | `- **Ignored:** <why not at all>` |
```

The `**Applied:**` bullet does not come out as one line. In the reference report it ran to roughly
250 words on a single finding — the longest block in the file, longer than the finding it was
resolving. It absorbs the rationale for the fix, the files it touched, and a before/after of the
code, none of which the bullet is for.

Three prior tasks in this series saw it and left it alone on purpose:
`2026-08-07-cap-review-findings-design.md` named it *"the worst single offender in the reference
report"*, and both `2026-08-07-trim-review-resolve-output-design.md` and
`2026-08-08-triage-gate-design.md` list it out of scope against this task. It is the last piece of
the report format still uncapped.

### Why "one line" failed

The constraint is real but unenforceable as written. "One line" describes markdown structure, not
length — a single line holds 250 words as readily as 10, so the instruction is satisfied by the
output it was meant to prevent. Nothing else in the block pushes back: `<what changed>` is an
unbounded placeholder, and no line says what the bullet must leave out.

Every other field in this format is bounded by a number. `.claude/rules/review-report.md:38-47` caps
each finding field in words — Title 10, What 30, Fix 20 — and pairs each cap with a one-clause
statement of the field's job. The outcome bullet is the one field that got neither.

## The decision

Give the outcome bullet the same treatment the finding fields already have: a countable cap plus a
statement of what the bullet is for.

- The cap goes in the line that already introduces the table, replacing "one line".
- The job statement goes on its own line after the table, flush left, matching the table's own
  placement inside the bullet.

### Why 15 words

Tighter than `Fix`'s 20. `Fix` is written by a reviewer proposing a change that does not exist yet;
the outcome bullet is written after the change is made and approved, so it only has to name it.
Fifteen words cannot hold a file list or a before/after, which is the point.

### Why all three routes

`**Deferred:**` and `**Ignored:**` are short in practice, so capping them fixes nothing today. They
are capped anyway because the cap lives in the shared intro line, where excluding them would cost a
qualifier and invite the same drift later.

## `.claude/skills/asapp-resolve-review-issues/SKILL.md`

One block changes, under `### Sources`:

```diff
-- **Report outcome bullet** — exactly one, appended last, one line:
+- **Report outcome bullet** — exactly one, appended last, one sentence of ≤15 words:

 | Route | Bullet |
 |-------|--------|
 | Apply, or Explore through to a fix | `- **Applied:** <what changed>` |
 | Defer | `- **Deferred:** <why not now>` |
 | Ignore | `- **Ignored:** <why not at all>` |
+
+Name the outcome only — never the rationale, the files touched, or a before/after.
```

The `### Sources` table's mark-done cell is unchanged: its *"appending one outcome bullet last (see
below)"* still points at this block, which is still the only place the bullet is specified.

## Referrer sweep

`grep -rn "Applied:\|outcome bullet" .claude/ docs/superpowers/README.md CLAUDE.md`

The wording has one home. `.claude/rules/review-report.md` stops at the finding fields and never
mentions the outcome bullet; neither review skill writes one. No referrer needs updating.

## Scope

**In scope:** one edited line and one added line in
`.claude/skills/asapp-resolve-review-issues/SKILL.md`, plus the `TODO.md` line above.

**Out of scope:**

- **`.claude/rules/review-report.md`.** Byte-identical. The rule owns the finding format; the route
  a fix took is the resolving skill's behavior, and `2026-08-08-triage-gate-design.md` settled that
  the outcome bullet lives with it.
- **The `Sources` mark-done cell, the triage gate, and the issue briefing.** Untouched.
- **Step 3's wrap-up summary.** Its "one line each" governs chat output, not the report file, and is
  already honored.
- **Both review skills.** Neither writes an outcome bullet.
- **Existing reports.** `docs/reviews/` holds none on this branch; nothing is retrofitted.

## Verification

- The intro line reads *"exactly one, appended last, one sentence of ≤15 words"* — no "one line".
- The job line sits directly after the table, flush left, and names all three exclusions.
- The three-row table is otherwise byte-identical, placeholders included.
- `grep -n "one line" .claude/skills/asapp-resolve-review-issues/SKILL.md` returns only the four
  chat-output uses — the branch line at Step 1.2, the read-back at Step 1.4, the trade-off line at
  Step 2c.2, and the wrap-up at Step 3 — and nothing under `### Sources`.
- `git diff` touches exactly one file besides `TODO.md`.

## Amendment: the format moves to `review-report.md`

Raised by the developer after the change landed in `7cff4a41`: why cap the bullet in the skill when
every sibling cap lives in `.claude/rules/review-report.md`?

The cap was placed beside the shape it caps, and the shape was in the skill because
`2026-08-08-triage-gate-design.md` put the routes there — *"the **Applied:** bullet already lives in
the skill's `### Sources`. Its two siblings join it there."* That is path dependency, not a
principle, and it is the premise this amendment revisits.

The split is by concern, not by block:

| Concern | Home |
|---------|------|
| One outcome bullet, appended last, `Applied` / `Deferred` / `Ignored`, ≤15 words, names the outcome only | `.claude/rules/review-report.md` |
| Which route produces which bullet | `asapp-resolve-review-issues` |

The bullet's format is *what must hold* about a `docs/reviews/*.md` file, which is
`rule-authoring.md`'s test for rule material, and the rule is already path-scoped to exactly those
files. Routing is *how to do a task*, and the four routes exist only in the skill. `skill-authoring.md`
settles the rest: *"Conventions in `.claude/rules/*` are linked, never re-explained."*

It also closes a gap that predates this task. The rule's intro and its checkbox bullet both
contemplate the developer resolving findings by hand, yet the rule never said what a resolved
finding looks like — a hand-resolver reading only the rule learned nothing about outcome bullets.

### `.claude/rules/review-report.md`

A new `## Outcome Bullet` section, last, after `## Detail Block`:

````markdown
## Outcome Bullet

A settled finding is ticked and gains exactly one outcome bullet, appended last — one sentence of ≤15 words naming the outcome only, never the rationale, the files touched, or a before/after:

```markdown
    - **Applied:** <what changed>
    - **Deferred:** <why not now>
    - **Ignored:** <why not at all>
```
````

One lead-in sentence, then the shape. `rule-authoring.md` asks for bullet-phrase brevity and
label-then-list, so the cap and its exclusions ride the lead-in rather than trailing the list as a
second prose line. The shape sits in a fenced block indented four spaces, matching `## Detail Block`
and showing that the bullet nests inside the finding.

The `## Detail Block` checkbox bullet drops its second clause, now that the new section owns the
settled side: *"Write every checkbox **unchecked** — the developer ticks them as findings are
resolved."* becomes *"Write every checkbox **unchecked**."*

**Settled, not resolved.** Deferred and Ignored tick without a fix, so *resolved* misdescribes two of
the three. `review-report.md:52` already uses *settles* for exactly this act, so the intro's *"ticked
off as findings are resolved"* becomes *"…are settled"* — one term per concept, per
`rule-authoring.md`.

**The rule does not respell `- [ ]` → `- [x]`.** The skill's mark-done cell states that mechanic as
the action it performs; the rule states the end state. Spelling it in both was the one verbatim
overlap the move introduced.

### `.claude/skills/asapp-resolve-review-issues/SKILL.md`

The block keeps the routing and sheds the format. Placeholders go with it — repeating `<what
changed>` beside a rule citation is the restatement the move exists to remove.

```diff
-- **Report outcome bullet** — exactly one, appended last, one sentence of ≤15 words:
+- **Report outcome bullet** — which route produces which bullet; its format is `.claude/rules/review-report.md`:

 | Route | Bullet |
 |-------|--------|
-| Apply, or Explore through to a fix | `- **Applied:** <what changed>` |
-| Defer | `- **Deferred:** <why not now>` |
-| Ignore | `- **Ignored:** <why not at all>` |
-
-Name the outcome only — never the rationale, the files touched, or a before/after.
+| Apply, or Explore through to a fix | `**Applied:**` |
+| Defer | `**Deferred:**` |
+| Ignore | `**Ignored:**` |
```

The `### Sources` mark-done cell drops *"one outcome bullet last"*, which the rule now states:
*"ticking `- [ ]` → `- [x]` and appending its outcome bullet (see below)"*.

### What is not touched

`2026-08-08-triage-gate-design.md` keeps its rejected-alternative entry as written. It records what
was decided that day; this section records the revision.

The triage gate's route table still says *"mark done with the **Deferred:** bullet"*. It names the
bullet, never its format, so the citation holds.

### Verification

- `## Outcome Bullet` is the last section of `review-report.md`, and the rule stays under 100 lines.
- The three bullet shapes and the ≤15-word cap appear in `review-report.md` and nowhere else.
- The skill's table carries labels only — no placeholders, no cap, no exclusions.
- `grep -rn "15 words\|what changed" .claude/` returns only `review-report.md`.

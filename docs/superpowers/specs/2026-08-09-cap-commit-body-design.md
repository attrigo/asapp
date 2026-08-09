# Cap draft-commit-msg's Lead Paragraph and Bullets in Words

**Status**: Designed

## Context

`asapp-draft-commit-msg` defines the body shape but bounds neither part by length.
`SKILL.md:70-71`:

```markdown
- **Lead paragraph** (optional) — 1–3 sentences of why (motivation, root cause, constraint). One line, no hard-wrapping. Omit when the subject already conveys the why.
- **Bullets** (required once a body exists) — `-` + single space, first letter capitalized, imperative mood (same as the subject), no trailing period, one change per bullet on a single line (no nesting, no multi-line bullets).
```

Both constraints bound **structure**, not length. A sentence carries unbounded clauses; "a single
line" holds 40 words as readily as 10. The bodies drifted accordingly.

Measured over the last 80 commits — 53 with a lead paragraph, 300 bullets:

| | min | p25 | p50 | p75 | p90 | max |
|---|---|---|---|---|---|---|
| Lead words | 9 | 25 | 31 | 49 | 81 | 241 |
| Bullet words | 4 | — | 13 | 17 | 22 | 39 |

The midrange is healthy and the tail is not. The last ten commits sit entirely in that tail:
96-259 body words against a 15-100 baseline. `dc083981` opens with an 82-word lead and runs ten
bullets, one of them 20 words.

This repo has already diagnosed the same failure twice and fixed it the same way both times:

| Commit | Subject | The substitution |
|---|---|---|
| `b5f01a9` | *cap review findings to short, plain-language blocks* | "one plain sentence" → a per-field word cap |
| `9ad3212d` | *shorten resolve-review-issues' applied note to one line* | "one line" → one sentence of 15 words or fewer |

`9ad3212d` states the principle directly: a cap that bounds markdown structure does not bound
length, so the cap must be countable.

## The decision

Two countable caps, one per body part: **lead ≤ 30 words**, **each bullet ≤ 15 words**.

**30** sits on today's median lead (31), so commits in the healthy midrange are untouched and only
the 81-to-241-word tail is cut. It is also the scale the sibling skills already use — close-task's
anchor line is capped at 25 words, review-report.md's `Fix` at 20.

**15** sits just above the median bullet (13) and reproduces `9ad3212d`'s cap verbatim. Roughly
two thirds of existing bullets already pass.

**No bullet-count cap**, now or as a follow-up. The two caps bound each part; the number of parts
follows the change.

The sentence count does not survive alongside the word cap. Two bounds on one quantity, only one of
them countable, is what `skill-authoring.md`'s *say it once* forbids — and dropping the
structural one is precisely the substitution `9ad3212d` made.

## `.claude/skills/asapp-draft-commit-msg/SKILL.md`

### Body rules (lines 70-71)

```diff
-- **Lead paragraph** (optional) — 1–3 sentences of why (motivation, root cause, constraint). One line, no hard-wrapping. Omit when the subject already conveys the why.
+- **Lead paragraph** (optional) — the why (motivation, root cause, constraint) in 30 words or fewer. One line, no hard-wrapping. Omit when the subject already conveys the why.
-- **Bullets** (required once a body exists) — `-` + single space, first letter capitalized, imperative mood (same as the subject), no trailing period, one change per bullet on a single line (no nesting, no multi-line bullets).
+- **Bullets** (required once a body exists) — `-` + single space, first letter capitalized, imperative mood (same as the subject), no trailing period, one change per bullet in 15 words or fewer, on a single line (no nesting, no multi-line bullets).
```

The caps land here because *Body rules* is the home of every other body constraint, and Process
step 3 already delegates to it with *"Follow **Format** and **Body rules** (Reference)"*.

`One line, no hard-wrapping` and `on a single line (no nesting, no multi-line bullets)` both stay.
They govern rendering, which a word cap does not imply — a 30-word lead can still be hard-wrapped.

### Draft step (line 45)

```diff
-- When a body is needed, lead with a 1–3 sentence paragraph only if it carries a why the subject and bullets can't; otherwise go straight to bullets.
+- When a body is needed, lead with a paragraph only if it carries a why the subject and bullets can't; otherwise go straight to bullets.
```

The line's job is the decision — lead or no lead — not the size. The sentence count was the body
rule's second home; leaving it here would keep a dropped constraint alive in the step that runs
first.

## Referrer sweep

`grep -rn "1–3 sentence\|1-3 sentence\|Body rules\|lead paragraph" --include=*.md .claude/ docs/ CLAUDE.md`

No live surface restates the size. `examples.md:36,56` name the lead paragraph only to explain why
each example has one. `asapp-close-task` and `asapp-resolve-review-issues` invoke the skill rather
than restating its format, so both inherit the caps unchanged.

## Scope

**In scope:** the two sites above in `.claude/skills/asapp-draft-commit-msg/SKILL.md`, plus a
`TODO.md` subtask under *(ai) Sharpen the task workflow skills*.

**Out of scope:**

- **`examples.md` and the two inline examples in `SKILL.md`.** All already comply — longest example
  lead is 15 words, longest example bullet 11. Rewriting compliant examples to demonstrate a cap
  would change text that is already correct.
- **`docs/superpowers/specs/v0.4.0/2026-05-07-commit-msg-bulleted-body-design.md:55,125`.** An
  archived spec restating "1–3 sentences". It records what was decided in 0.4.0; editing it would
  falsify the record rather than update an instruction.
- **A total body cap.** Considered and declined: the per-part caps are what the developer asked
  for, and a third number bounding their sum would be a constraint no other skill in the repo uses.
- **A bullet-count cap.** Explicitly declined, per *The decision*.
- **The `commit-msg` git hook.** It validates the subject against the Conventional Commit regex and
  never reads the body. Enforcing a word cap there would reject commits the skill is meant to shape
  by drafting, not by rejection.

## Verification

- `grep -n 'sentence' .claude/skills/asapp-draft-commit-msg/SKILL.md` returns only line 50, the
  Output step's *"one sentence naming the chosen type and scope"*. No body rule counts sentences.
- *Body rules* states `30 words or fewer` and `15 words or fewer`, one cap per bullet.
- `git diff` touches only lines 45, 70, and 71 — no section added, moved, or reordered.
- Neither example in `SKILL.md` nor any example in `examples.md` changed.
- `SKILL.md` stays under 500 lines; the template's section order is unchanged.
- `git diff --name-only` lists exactly `.claude/skills/asapp-draft-commit-msg/SKILL.md`, `TODO.md`,
  and this spec.

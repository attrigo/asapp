# Cap close-task's Post-implementation Notes

**Status**: Designed

## Context

`asapp-close-task` Step 3 dispatches `documentation-engineer` to mark the task's spec implemented,
appending a `## N. Post-implementation notes` section that records where the implementation diverged
from the design. `SKILL.md:163-172` specifies it:

```markdown
## Post-implementation notes recipe

The `## N. Post-implementation notes` section states, in order:

1. **Opener** — "This spec and its plan (`<plan-path>`) were written before implementation. The core change shipped substantially as designed — <one sentence on what landed as specified>."
2. **Canonical source** — "the canonical implementation is the current state of <the real artifacts: files, configs, tests> on this branch, not this document."
3. **`Notable deltas:`** — a bullet per place the implementation diverged from the design. Each bullet: a **bold headline** naming the delta (and which spec section it reverses), then the *why*, anchored to the **durable artifacts** it touched — the files, classes, config keys, and tests.
4. **Closer** — "For future <area> edits, treat <the real artifacts> as the template; this spec is preserved as a record of the original design intent."
```

Measured across the 23 archived specs that carry a notes section (`docs/superpowers/specs/v0.4.0/`):

| | |
|---|---|
| Notes length | 193 – **716 words**, median 451 |
| Delta bullets | 1 – **11** |
| Specs whose notes cite commit hashes | 10 of 23, up to 15 hashes in one |

The worst case is `2026-06-14-circuit-breaker-http-clients-design.md`: 716 words, opening with a
**156-word paragraph** that recaps the spec section by section — §2.3 through §11, every config key
and test class named — before the deltas begin. Its five delta bullets then run 59 to 159 words and
cite six commit hashes between them.

### The recipe codified the shape, not the length

Those 23 notes all predate the recipe. It landed on 2026-07-07 in `95b6c4d9`, written from the notes
already in the repo, and it reproduced their structure faithfully: opener, canonical-source line,
deltas, closer. The 716-word example *is* the four-part shape. No note has been written under the
recipe yet — the ten specs on this branch are still `Designed`, their tasks unclosed — so the drift
is not evidence of the recipe failing. It is evidence of what the shape produces unbounded, and the
recipe prescribes that shape while bounding nothing.

Nothing in it pushes back on length. Part 1's `<one sentence on what landed as specified>` and part
3's *"the why"* are unbounded placeholders, no part carries a number, and no line says what the
section must leave out. This is the failure `2026-08-08-shorten-applied-note-design.md` diagnosed for
the outcome bullet — a constraint on structure standing in for a constraint on length — and the
answer there applies here: `.claude/rules/review-report.md:38-47` caps every finding field in words
and pairs each cap with a one-clause statement of the field's job.

## The decision

Replace the four-part prose recipe with an anchor line, capped delta bullets, and a cap table.

| Part | Cap | Its job |
|------|-----|---------|
| Anchor line | ≤25 words | names the durable artifacts that supersede this spec |
| Delta headline | ≤10 words, plus the spec section it revises | names what diverged |
| Delta body | ≤20 words | why it diverged, and the artifact that now holds it |
| Deltas | ≤10 bullets | one divergence each, most consequential first |

Worst case ~300 words against 716 observed, and every bullet individually readable.

### Why the opener and closer go

Both are templated boilerplate that appears near-verbatim in all 23 notes, and neither survives its
own cap.

The **opener** asserts two things. *"This spec and its plan were written before implementation"* is
true of every spec in the directory and is what `**Status**: Implemented` already records. *"The core
change shipped substantially as designed"* is the sentence that grew into the 156-word recap: asked to
summarize what landed as specified, an agent has the whole spec as raw material and no ceiling. What
a reader needs is the exceptions, which is what the deltas are.

The **closer** restates the anchor line. *"Treat <the real artifacts> as the template"* and *"the
canonical implementation is <the real artifacts>"* name the same artifacts and make the same point;
the remainder — *"preserved as a record of the original design intent"* — is framing that applies to
every archived spec.

The **anchor line** stays because it is the one fact a reader cannot infer: which files, classes,
config keys, and tests now hold the behavior this document describes.

### Why these numbers

- **10 bullets.** Set by the developer. It clears the observed maximum of 11 by one, so the cap binds
  only on a task that diverged further than any yet has — at which point the count is the signal.
- **20-word body.** Set by the developer. Tighter than `review-report.md`'s `What` (30 words), which
  must explain a defect to someone who has not seen it; a delta body explains a decision to someone
  reading the spec it revises, with the headline having already named it.
- **10-word headline.** `review-report.md`'s Title cap, doing the same job — naming one thing.
- **25-word anchor line.** Enough for four or five artifact names and the clause that frames them.

### Every cap reads as a ceiling

Raised by the developer against a first draft that wrote the caps as bare numbers: a word count and a
bullet count do not read alike. Nobody stretches prose to hit 25 words, but *"10 bullets"* in a table
of requirements reads as a quota, and an agent holding four real deltas will pad to ten by splitting
one delta across bullets or restating spec sections — reintroducing the recap the cap exists to
prevent. Two things answer it: `≤` on all four caps, the form `review-report.md:58` and
`asapp-resolve-review-issues:126-129` already use, and a bullet stating that the count follows the
divergences, not the cap.

### Terminology and one stale reference

The recipe says *"the real artifacts"* in parts 2 and 4 and *"the durable artifacts"* in part 3, for
the same concept. `skill-authoring.md` asks for one term per concept, and Step 2 already settled it:
*"Capture each delta by the durable artifacts it touched."* **Durable artifacts** is the term.

The anchor line also drops *"on this branch"*. The spec squash-merges onto `main`, where the phrase
no longer points anywhere — the same class of dead reference the hash prohibition exists to prevent.

## `.claude/skills/asapp-close-task/SKILL.md`

### The recipe section

Lines 163-172 are replaced in full:

````markdown
## Post-implementation notes recipe

The `## N. Post-implementation notes` section is an anchor line, then the deltas:

```markdown
The canonical implementation is <the durable artifacts: files, classes, config keys, tests>, not this document.

Notable deltas:

- **<what diverged> (revises §<n>).** <why, and the artifact that now holds it>
```

| Part | Cap | Its job — and only this |
|------|-----|-------------------------|
| Anchor line | ≤25 words | names the durable artifacts that supersede this spec |
| Delta headline | ≤10 words, plus the spec section it revises | names what diverged |
| Delta body | ≤20 words | why it diverged, and the artifact that now holds it |
| Deltas | ≤10 bullets | one divergence each, most consequential first |

- **The count is what diverged, not the cap** — never pad to ten. With no deltas the anchor line stands alone; omit `Notable deltas:`.
- **Never a spec recap, a file inventory, or a commit hash** — the task squash-merges into one commit on `main`, so a branch SHA is a dead reference there.
````

Against `skill-authoring.md`:

- **Shape then caps.** A fenced template followed by a cap table is what `review-report.md`'s
  *Detail Block* and `asapp-resolve-review-issues`' *Issue briefing* already use. The table maps part
  → cap → job, which is the tabular data the rule permits.
- **Low freedom tier.** close-task's tier. The prose around the shape shrinks; nothing prescriptive
  is vaguened.
- **No rationalization prose.** The 60-word hash paragraph becomes one clause. Its reasoning is kept,
  because the mechanism — the squash — is why the prohibition is not arbitrary, and 10 of 23 notes
  broke it.
- **Reverses → revises.** A delta may extend or refine the design, not only undo it.
- **Position.** The section stays an `## H2` between Process and Delegation, where its two
  reference-tier siblings sit.

### Step 3's status line

```diff
-- Changes the header `**Status**:` from `Proposed`/`Draft` → `Implemented`, and
+- Sets the header `**Status**:` to `Implemented`, and
```

`Proposed` and `Draft` appear in no spec — `superpowers:brainstorming` writes `Designed`. Naming only
the target decouples the step from the upstream skill's vocabulary, so a future change there cannot
stale it again. The following parenthetical, *"(If the spec is already `Implemented`, skip this step
and reuse the existing notes.)"*, still reads correctly.

## Referrer sweep

`grep -rn "Post-implementation\|post-impl\|Notable deltas\|real artifacts" .claude/ CLAUDE.md docs/superpowers/README.md`

Three referrers, all inside `asapp-close-task`, none needing an edit:

| Site | Text | Why it holds |
|---|---|---|
| Step 3 | *"per the *Post-implementation notes recipe*"* | the section keeps its name |
| Step 9 | *"its essence is now in the spec notes + the squash commit"* | names the notes, not their shape |
| Delegation | *"Status + post-impl notes (writing only, no code review)"* | routing only |

No rule, agent, or other skill mentions the notes. `documentation-engineer` carries no rule-routing
table, so the recipe reaches it through Step 3's dispatch either way.

## Scope

**In scope:** the recipe section and Step 3's status line, both in
`.claude/skills/asapp-close-task/SKILL.md`, plus `TODO.md:52`.

**Out of scope:**

- **Step 2 (*Analyze the task's context*).** Its analysis feeds the squash message as well as the
  notes; capping it would starve the message. Its own *"not commit hashes"* clause governs what to
  extract from the SDD record, and stays.
- **A `.claude/rules/` home for the format.** Weighed and declined: one writer, one appended section,
  and no existing rule governs `docs/superpowers/specs/*.md`. `skill-authoring.md` provides for
  skill-specific recipes as reference material.
- **Folding `Plan & report handling`, the recipe, and `Reverting` under one `## Reference` heading.**
  A three-section restructure of a skill this task is capping one section of. Converting only the
  recipe to an `###` would leave the file less consistent than it is.
- **The 23 archived notes.** Not retrofitted. They are the record of what was written, and the specs
  they head are closed.
- **The `**Status:**` / `**Status**:` punctuation split** across the archived specs (11 use the
  first form). Cosmetic, in closed specs, and Step 3 matches on the label either way.
- **The sibling subtasks at `TODO.md:53-57`** — commit-sized outcomes from the review and version
  skills, and Step 7's doc-commit wording. Neither touches the recipe.

## Verification

- The recipe section is a fenced template, a four-row cap table, and two bullets — no numbered parts,
  no opener, no closer.
- `grep -n "real artifacts\|on this branch\|reverses" .claude/skills/asapp-close-task/SKILL.md`
  returns nothing — all three occur only in the replaced lines.
- `grep -n "Proposed" .claude/skills/asapp-close-task/SKILL.md` returns nothing. `Draft` still
  matches Step 5's title, *"Draft the squash message"*, which is unrelated and unchanged.
- Every cap in the table carries `≤`; every row's job column is one clause.
- `grep -n "<=" .claude/skills/asapp-close-task/SKILL.md` returns nothing — the repo writes `≤`.
- Section order and the `## H2` level are unchanged; `SKILL.md` stays under 500 lines.
- `git diff` touches exactly `.claude/skills/asapp-close-task/SKILL.md` and `TODO.md`.

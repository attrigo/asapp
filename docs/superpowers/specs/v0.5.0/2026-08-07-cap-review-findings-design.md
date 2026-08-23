# Cap Review Findings to Short, Plain-Language Blocks

**Status**: Designed

## Context

`.claude/rules/review-report.md` is the shared finding format. Both review skills render their
reports from it, and `asapp-resolve-review-issues` reads it back as the brief for each fix. It is the
single root cause of finding verbosity: nothing else defines what a finding looks like.

The last report written under it, `docs/reviews/reuse-boot-bom-versions-review.md` (commit
`eea9339c`), is the ground truth. It ran 103 lines for eight findings. Its first finding spent ~330
words across seven bullets:

- **Why it matters** — 65 words, one grammatical sentence, three stacked clauses.
- **Evidence** — 70 words narrating two command outputs and a BOM claim.
- **Resolver notes** — 120 words across three topics: a code snippet, a comparison to the rule's own
  stated remedy, and an unsettled block-ordering decision.

The rule already said *"one plain sentence"* for Description and *"one line"* for Why it matters. It
did not hold, because a sentence carries unbounded clauses. Repeating the instruction more firmly
would not have changed the outcome.

Two further problems the rule is silent on. Six fields carry two redundancies — Location and Evidence
both address the same code, and Description and Why it matters answer one question — so a reviewer
with context to spill has two fields inviting it. And the report grew ~150 words of undocumented
prose between the counts line and the first table, plus a second preamble inside the nice-to-have
section.

## The decision

**Collapse the six fields to four, cap each in words, and put the brevity requirement in the
dispatch prompts where findings are actually written.**

Four files change. No file is created; no agent body is touched.

| File | Change |
|---|---|
| `.claude/rules/review-report.md` | 6 fields → 4, short labels, per-field caps, one-topic and plain-language constraints, prose budget |
| `.claude/skills/asapp-review-task/SKILL.md` | Step 2 gains a brevity bullet; Step 3 loses its plain-language sentence |
| `.claude/skills/asapp-review-version/SKILL.md` | Step 3 gains the same bullet; Step 4's seam dispatch names the caps and the renamed field |
| `.claude/skills/asapp-resolve-review-issues/SKILL.md` | Field citations follow the rename |

Three forces carry the change, and each fixes a distinct failure:

- **Word caps** give the reviewer something countable. *One sentence* did not.
- **One topic per field** stops clause-stacking fitting under a sentence count. A second topic is a
  second finding, never a second clause.
- **Merging the redundant pairs** removes the two padding sites rather than defending them. A
  consequence that must fit inside the defect's own sentence cannot run to 65 words, and a quote
  hanging off an address cannot narrate command output.

**Short labels are load-bearing, not cosmetic.** A bullet labeled **Recommended action:** invites a
paragraph; one labeled **Fix:** invites a sentence. The prose-y names were part of the problem.

### What is knowingly lost

**A separately justified consequence.** Splitting Description from Why it matters forced the
reviewer to state an impact, and a reviewer who cannot state one has a taste finding. Accepted: the
merged form is stronger discipline, not weaker — *"X is unpinned, so a Boot bump breaks Y"* cannot be
written without the consequence, whereas a separate bullet can be filled with padding and was.

**Reviewer-recorded measurements.** Evidence's command outputs sometimes proved a finding without
opening a file. Accepted: `asapp-resolve-review-issues` Step 2b dispatches `Explore` from the
finding's **Where** to confirm the issue anyway, so the measurement was re-derived at fix time
regardless.

### Alternatives rejected

- **State the caps in both dispatch prompts verbatim.** `skill-authoring.md` requires a format shared
  between skills to live in one reference file both link to. Inlining puts the numbers in three files
  and they drift on the first change.
- **Put the caps in `code-reviewer`'s body.** `security-auditor` writes findings too, so the block
  would be duplicated; `code-reviewer` is also dispatched for work that produces no report; and
  `agent-authoring.md` keeps bodies free of project format specs.
- **Cap the whole finding block instead of each field.** A 120-word block budget is legal with 95 of
  those words in Evidence. The block stays unreadable and the offending field is untouched.
- **Ban jargon outright** as the reading of *plain-language*. Paraphrasing `kotlin-stdlib` into "the
  build's own Kotlin library" is longer than naming it and leaves nothing to grep for. Plain language
  bites on reviewer voice, not on the subject.
- **Drop Watch as well, leaving three fields.** The gotcha is context the reviewer already holds; the
  resolver would rediscover it, or the fix lands in the wrong place and a later review flags it.
- **Forbid all prose in the report body.** A reviewer that verified four dimensions clean has nowhere
  to record it, so the next review redoes them, and a report with few findings reads as a shallow
  review rather than a clean one.

## `.claude/rules/review-report.md`

The frontmatter, the intro line, and the whole **Summary Table** section (14–24) are unchanged —
including the `<axis>` column split and the version review's theme ordering.

### Sections (8–12)

One bullet is appended:

```markdown
- Add no other prose — at most one `Also checked, clean:` line after the counts, naming in ≤20 words
  what was verified and found clean.
```

It states only what is new. Naming the counts line and the sections again would restate the two
bullets above it.

The four existing bullets stand.

### Detail Block (26–40)

The section is replaced in full:

````markdown
## Detail Block

```markdown
- [ ] **S1 — <title>**
    - **Where:** <file:line or `Class#method`; the offending line quoted after it, when it helps>
    - **What:** <the defect, so its consequence>
    - **Fix:** <what to do>
    - **Watch:** <one gotcha the fix must respect — optional>
```

Every bullet is **one sentence, one topic**, within its cap:

| Field | Cap | Its job — and only this |
|-------|-----|-------------------------|
| Title | 10 words | names the defect |
| Where | the sites, bare | `file:line` / `Class#method`; a list when it spans sites (both sides, for a seam); a verbatim quoted line — never command output |
| What | 30 words | the defect and the concrete consequence it causes |
| Fix | 20 words | what to do |
| Watch | 20 words | one gotcha, constraint, or ordering the fix must respect |

- **A second topic is a second finding** — never a second clause. Cut it when it is rationale rather
  than a defect.
- **State the defect, not your reading of it** — no meta-commentary on the code's own rationale, no
  hedging, no citation padding. Stack terms belong where they *are* the subject.
- **Where / What / Fix** are always present; **Watch** only when it earns its place.
- Severity is the section, tracked by the ID prefix (`M#` / `S#` / `N#`), not a column.
- Write every checkbox **unchecked** — the developer ticks them as findings are resolved.
````

The two decisions taken on the worst offenders survive the merge rather than being dropped:
Evidence's *verbatim, no command narration* became Where's quote clause, and Resolver notes' *one
gotcha* became Watch.

The rule grows from 41 lines to 51 — well inside `rule-authoring.md`'s ~100-line limit — while
carrying the caps, the constraints, and the prose budget.

## `.claude/skills/asapp-review-task/SKILL.md`

| Line | Element | Change |
|---|---|---|
| 67 | Step 2, *Every review … must* | The existing context bullet absorbs the requirement: *the fields in `.claude/rules/review-report.md`* becomes *read `.claude/rules/review-report.md` first and hold every field to the shape and caps it defines*. It keeps its rationale clause, which explains why the reviewer records context it does not itself need. **No second bullet** — `skill-authoring.md` links a rule's conventions rather than re-explaining them, so the caps, the one-topic rule, and the second-topic rule are never repeated here. |
| 78 | Step 3 | The closing sentence *Write in terms a non-specialist could act on.* is deleted. The rule owns plain language now, so `asapp-review-version` inherits it for free. The rest of the line stands. |
| 64–66, 97 | Step 2's other bullets, Step 5's report pointer | **Unchanged.** |
| — | Unchanged | Steps 0–1, Steps 3–4's presentation shape, Step 6, Delegation, Guardrails. |

## `.claude/skills/asapp-review-version/SKILL.md`

| Line | Element | Change |
|---|---|---|
| 81 | Step 3, *Tell each reviewer to* | The same absorption as `asapp-review-task:67`, without the rationale clause. |
| 94 | Step 4, seam dispatch | *Classify like every finding; set **Theme** …; record Location as a list covering both sides* becomes *Classify like every finding, to the shape and caps in `.claude/rules/review-report.md`; set **Theme** …; record **Where** as a list covering both sides*. The seam reviewer is a separate dispatch, so it cannot inherit Step 3's bullet, and it names the rule rather than pointing at "the same caps" — nothing on the page anchors that phrase. |
| 104 | Step 5, *Findings* | **Unchanged** — it already points at the rule. |
| — | Unchanged | Steps 0–2, Step 3's lenses and depth guidance, Step 4's hunt-the-seams framing, Steps 5–7, Delegation, Guardrails. |

## `.claude/skills/asapp-resolve-review-issues/SKILL.md`

The rename is mechanical; no step changes shape.

| Line | Element | Change |
|---|---|---|
| 46 | Step 1.4, report mode | *Each finding's **Location**, **Description**, **Why it matters**, optional **Evidence**, **Recommended action**, and optional **Resolver notes** are the brief.* becomes *Each finding's **Where**, **What**, **Fix**, and optional **Watch** are the brief.* |
| 47 | Step 1.4, report mode | *from the finding's **Location*** becomes *from the finding's **Where***. |
| 59 | Step 2b, report-mode sub-bullet | *start from the finding's Location* becomes *start from the finding's **Where***. |
| 62 | Step 2c.1, report-mode sub-bullet | *its Description, Why it matters, and Location* becomes *its **What** and **Where***. |
| 63 | Step 2c.2 | *Fold in any Resolver notes on the finding* becomes *Fold in any **Watch** note on the finding*, and the trailing parenthetical is deleted — see below. |
| 61 | Step 2c.1's *what · why it matters · where* block | **Unchanged** — the subtask at `TODO.md:51` owns the Propose block's shape. |
| 89 | Sources, report row | **Unchanged** — the **Applied:** bullet is the subtask at `TODO.md:58`. |

### The contradiction the rename surfaced

Line 63 today reads *"A report nice-to-have may carry no **Recommended action** — propose one anyway;
don't assume one was given."* The rule lists Recommended action as always present, so the skill
guards against a state the format forbids. The conflict predates this change; the rename forces a
call.

**Fix stays mandatory and the parenthetical is deleted.** A finding with nothing proposed is not
actionable, and Step 2c proposes solutions with the developer regardless of what the finding
suggested.

## Referrer sweep

| File | Line | Edit |
|---|---|---|
| `TODO.md` | 47 | Mark the subtask `- [X]` and drop its three now-resolved Notes (48–50). |

No other file cites the finding fields. `grep -rn "review-report" .claude/ CLAUDE.md` returns only
the five skill lines above plus this rule; `.claude/agents/**` names no field.

## Scope

**In scope:** the rule rewrite, four edits across the two review skills, five in the resolve skill
(four renames plus the rename-and-delete at line 63), and the `TODO.md` line above.

**Out of scope:**

- **What either review skill prints in chat**, and `asapp-resolve-review-issues`' *what · why ·
  where* Propose block — the subtask at `TODO.md:51` owns both. This task changes the format the
  chat renders from, not how much of it is rendered.
- **The `Applied:` bullet** — the subtask at `TODO.md:58`. It is the worst single offender in the
  reference report (~250 words on N3) and is deliberately left alone.
- **The `Suggested disposition: defer` bullet** the reference report carries, which the rule never
  defined. The subtask at `TODO.md:55` settles where deferred findings live; half-fixing it here
  would be undone.
- **Summary table columns.** `Kind` is load-bearing in `asapp-review-task`'s core principle, so
  dropping it ripples through Steps 3–5. Effort and Impact drive the fix-or-defer decision.
- **The report title, anchor line, and disclaimers.** Skill-owned, above the rule's body.
- **`code-reviewer.md` and `security-auditor.md`.** Format specs stay out of agent bodies.
- **Existing reports.** `docs/reviews/` holds none on this branch; nothing is retrofitted.

## Verification

- `.claude/rules/review-report.md` names exactly four fields, each with a cap, and no longer contains
  the strings `Description`, `Why it matters`, `Evidence`, `Recommended action`, or `Resolver notes`.
- `grep -rn "Resolver notes\|Why it matters\|Recommended action" .claude/` returns nothing. Run it
  case-sensitively: the lowercase `**why it matters**` in `asapp-resolve-review-issues:61` is the
  Propose block's own wording and deliberately survives. Do not grep `Location`, `Description`, or
  `Evidence` repo-wide either — each has unrelated hits in `ports-adapters.md`, `testing-core.md`,
  `api-designer.md`, and `skill-authoring.md`.
- Both review skills tell the reviewer to read the rule — required, because the rule's `paths:` glob
  is `docs/reviews/*.md` and never auto-loads in a subagent reviewing a code diff.
- `asapp-review-version` Step 4's seam dispatch names the rule itself rather than inheriting.
- **No skill restates the rule.** No cap number, and no copy of the one-topic or second-topic rule,
  appears anywhere under `.claude/skills/`; every one lives only in the rule. Each of the six skill
  references links to it instead.
- `asapp-resolve-review-issues` cites only the four new field names, and no step claims a field the
  rule marks mandatory may be absent.
- The rule stays under `rule-authoring.md`'s ~100 lines; all three skills stay under
  `skill-authoring.md`'s 500 and keep their section order.
- No build or test impact: no production code, resources, or build files change.

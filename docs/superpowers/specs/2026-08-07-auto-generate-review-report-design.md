# Auto-Generate a Full Findings Report From Both Review Skills

**Status**: Designed

## Context

The two review skills disagree about where a finding lands.

`asapp-review-version` asks nothing: it reviews, then writes every finding to
`docs/reviews/v<ver>-readiness-report.md`. `asapp-review-task` stops mid-run and asks which findings
to act on, then splits them by the answer — **apply now** into
`docs/reviews/<task-slug>-review.md`, **defer** into `TODO.md` as new top-level entries, confirming a
new bucket or scope when none fits (Steps 3–5).

Three problems with the split.

**The defer decision is taken at the worst moment.** At Step 4 nothing has been explored. The
developer decides on a 10-word title, an effort letter, and an impact letter — and then decides again,
per issue, when `asapp-resolve-review-issues` briefs the fix and proposes solutions. The first
decision costs an interactive round trip and is superseded by the second.

**Routing to `TODO.md` discards the detail.** A finding is a Where / What / Fix block; a `TODO.md`
entry is one imperative phrase of ~10 words with no implementation terms (`todo.md` Wording). The
sites, the consequence, and the proposed fix are dropped at the one moment they are free to keep —
the reviewer is still holding them.

**Practice already left the skill behind.** The last report written,
`docs/reviews/restore-services-bill-of-materials-review.md` (commit `219ddf28`), records all eight
findings and opens with *"Every finding is recorded here — nothing was routed to `TODO.md`."* Two of
them carry a `Disposition: defer` bullet that `.claude/rules/review-report.md` never defined. The
cap-findings spec saw that bullet and deliberately left it alone, naming this subtask as its owner.

## The decision

**Generate the report unasked, record every finding in it, and let the reviewer only *suggest*
deferring — through one optional field. The decision moves to fix time.**

Six files change, all Markdown. No file is created; no agent body is touched.

| File | Change |
|---|---|
| `.claude/rules/review-report.md` | one optional field, `**Defer:**`, capped at 20 words |
| `.claude/skills/asapp-review-task/SKILL.md` | the selection gate and the `TODO.md` write are removed |
| `.claude/skills/asapp-review-version/SKILL.md` | one wrap-up sentence; the field arrives via the rule |
| `.claude/rules/todo.md` | the deferred-findings routing bullet is deleted |
| `.claude/skills/asapp-close-task/SKILL.md` | one stale reason clause about a missing report |
| `TODO.md` | this subtask ticked; the triage-gate subtask promoted directly beneath it |

Three forces carry the change:

- **One artifact, one shape.** The report is the complete record of a review. `TODO.md` stops being a
  review sink, and both skills produce the same thing.
- **The suggestion travels with its evidence.** A `Defer:` line sits directly under the Where / What /
  Fix that justify it, so deciding later costs no rediscovery.
- **Deciding once, later, beats deciding twice.** `asapp-resolve-review-issues` already walks findings
  one at a time and already asks. Removing the earlier ask removes a decision, not a safeguard.

**The field's wording is scope-neutral and that is load-bearing.** *"A separate concern from the work
under review"* covers a task branch and a release range equally, which is what lets
`asapp-review-version` inherit the field without a word of its own.

### What is knowingly lost

**Deferred findings stop being tracked as tasks.** A `TODO.md` entry gets picked up in some later
version; a `Defer:` note in a closed task's report does not. Accepted: `asapp-close-task` commits the
report to `main`, so the record survives in git, and a finding that genuinely deserves a version slot
can be promoted by hand. The old behaviour bought tracking by putting a roadmap entry behind every
nice-to-have a reviewer had an opinion about.

**A review no longer ends in a decision.** Nothing is settled when `asapp-review-task` finishes; the
whole set moves to the resolve skill. Accepted — that is the point of the change.

### Alternatives rejected

- **A trailing `## Recommended follow-ups` section** holding the deferred findings — the literal
  reading of this subtask's original Note. It needs a second table with a Severity column, a second ID
  prefix, and a rule for which sections the resolve skill enumerates. Worse, it re-implements the
  apply-now / defer decision at report-write time, which is the thing being removed.
- **A `Disposition` column in the summary table, alongside the field.** Scannable, but it is a second
  home for one fact, and it would have to be kept in step with the chat table as well.
- **Keep the gate; write both dispositions into one report.** The `TODO.md` write disappears but the
  interactive step survives and now buys nothing at all.
- **Wire `asapp-resolve-review-issues` to the field now.** Its per-issue loop would offer the choice
  at *c. Propose* — after *b. Explore* has already run. The subtask promoted directly below this one
  puts the gate *before* exploration, and would rewrite the line. Same call the cap-findings spec made
  about the disposition bullet.
- **Drop the `Kind` column too, now that `Disposition` is gone.** `Kind` is `asapp-review-task`'s
  `<axis>` in the rule and load-bearing in its core principle.
- **Let a zero-findings review write no report.** Then a missing report means either "clean" or "never
  reviewed", and `asapp-close-task` cannot tell which.

## `.claude/rules/review-report.md`

The frontmatter, intro, **Sections**, and **Summary Table** (1–26) are unchanged — including the
`<axis>` column split and the version review's theme ordering.

### Detail Block (28–51)

One line is appended to the block, after **Watch**:

```markdown
    - **Defer:** <why this is a separate concern, not part of the work under review — optional>
```

One row is appended to the field table:

| Field | Cap | Its job — and only this |
|-------|-----|-------------------------|
| Defer | 20 words | why the finding is a separate concern rather than part of the work under review |

Line 49 absorbs the new field:

```markdown
- **Where / What / Fix** are always present; **Watch** and **Defer** only when they earn their place.
```

And one bullet is added after it:

```markdown
- **Defer** is a suggestion, not a routing instruction — its presence says the reviewer judges the
  finding out of scope here; the developer settles it at fix time.
```

That bullet is the only place the field's semantics are stated. Both review skills already tell their
reviewers to read this rule and hold every field to it, so neither repeats a word of it.

The rule grows from 52 lines to 54 — well inside `rule-authoring.md`'s ~100-line limit.

## `.claude/skills/asapp-review-task/SKILL.md`

| Line | Element | Change |
|---|---|---|
| 8 | Description, Do-NOT | *it only reviews and logs … to fix the logged findings* → *it only reviews and reports … to fix the reported findings*. |
| 16 | Intro | *…present prioritized findings, then route the ones you select — **apply-now** to a report file, **deferred** to `TODO.md`* → *…present prioritized findings, then record every one of them in a report file*. The closing *Runs before the manual close.* stands. |
| 18 | Core principle | **Kind** survives; **disposition** stops being an axis. Becomes: every finding has a **kind** — an **issue** (something is wrong) or an **improvement** (something could be better) — and every finding is recorded in the report; a finding the reviewer judges a separate concern carries a **Defer** note, and `asapp-resolve-review-issues` settles each one with the developer afterwards. |
| 30–36 | Step 0 | Five tracking tasks become four: (1) locate and determine review scope (Step 1); (2) run the review and consolidate (Step 2); (3) present the findings (Step 3); (4) write the report and wrap up (Steps 4–5). |
| 38 | Step 0 | *Keep task 3 `in_progress` across the wait for the user's selection…* is **deleted** — there is no wait left. |
| 66 | Step 2 | `, and **disposition** (apply now / defer)` is dropped; severity, effort, and impact stay. |
| 67 | Step 2 | **Unchanged** — it already sends the reviewer to the rule, which now carries **Defer**. |
| 75 | Step 3, table header | The `Disposition` column is dropped, leaving `ID · Title · Kind · Severity · Effort · Impact`. |
| 78 | Step 3, legend | `· **Disposition** apply now / defer` is dropped. |
| 79 | Step 3 | The apply-now / defer explainer line is **deleted** in full. |
| 81–83 | Step 4, *Ask which to act on, then wait* | **Deleted in full.** |
| 85–87 | Step 5 head | Becomes `### 4. Write the report`; *Route **only the selected findings**…* is deleted. One line takes its place: write it on **every** run — including a trivial inline review and one that found nothing — because the report is the record that the review happened. |
| 89 | Step 4, path | The *`**Apply now →** …`* framing becomes a plain *Write to `docs/reviews/<task-slug>-review.md`*. The slug rule, the create-if-absent rule, and the overwrite rule are unchanged. |
| 93 | Step 4, disclaimer | *apply-now findings only (deferred routed to `TODO.md`)* → *every finding is recorded here — nothing is routed to `TODO.md`*. The code-review-only half stands. |
| 95 | Step 4 | **Unchanged** — it already points at the rule and names the `Kind` column. |
| 97–102 | Step 5, *Defer → `TODO.md`* | **Deleted in full** — the bucket / Backlog routing and the new-scope confirmation go with it. |
| 104–107 | Step 6 | Becomes `### 5. Wrap-up`. *Summarize what was routed where…* → the report path, the finding counts, and which findings carry a **Defer** note. The *nothing was committed* reminder and the next-steps line stand. |
| 121 | Guardrails | *The only writes are the report … **and `TODO.md`**, and only after the user selects findings* → **The only write is the report** at `docs/reviews/<task-slug>-review.md` — log nothing to `TODO.md`. This is now `asapp-review-version`'s guardrail verbatim. |
| — | Unchanged | Usage, Step 1, Step 2's dispatch / depth / lenses, Delegation, the other three Guardrails. |

## `.claude/skills/asapp-review-version/SKILL.md`

| Line | Element | Change |
|---|---|---|
| 119 | Step 7, Wrap-up | *Findings are theirs to route (fix now or defer), ticking the report's checkboxes.* → *Findings are theirs to work through — `asapp-resolve-review-issues` or by hand — ticking the report's checkboxes.* Routing is no longer a thing that happens. |
| 81, 94 | Step 3 and Step 4 dispatches | **Unchanged.** Both already require the reviewer to read `.claude/rules/review-report.md` and hold every field to it, so **Defer** arrives with no edit. |
| — | Unchanged | Everything else, including the verdict, the per-theme summary, and the report path. |

That the shared shape costs this skill one sentence is the evidence the change landed in the right
file.

## Referrer sweep

| File | Line | Edit |
|---|---|---|
| `.claude/rules/todo.md` | 63 | Delete the **Deferred review findings** bullet. |
| `.claude/rules/todo.md` | 62 | Drop the now-pointless `**Manual notes** —` label; the bullet stands as the plain statement that hand-logged issues go as nested bullets under the task they concern. |
| `.claude/skills/asapp-close-task/SKILL.md` | 53 | *(may be absent if the task had no apply-now review findings)* → *(absent only if no review was run)*. Its three other conditional mentions of the report (122, 133, 161) stay correct as written. |
| `TODO.md` | 49–51 | Tick the subtask `- [X]`; drop its two now-resolved Notes. |
| `TODO.md` | 60–64 | Move the triage-gate subtask and its four Notes to sit directly beneath the ticked line, ahead of *Shorten resolve-review-issues' applied note*. Add one Note: for a report issue the gate reads the finding's **Defer** note — the reviewer's suggestion, which the review skills record but never act on. |

No other file cites the routing. `grep -rn "apply now\|apply-now\|[Dd]isposition" .claude/ CLAUDE.md
TODO.md` hits only `asapp-review-task` (twelve lines) and `asapp-close-task:53`; `.claude/rules/todo.md`
names `asapp-review-task` only on line 63; no agent body mentions a report field or a disposition.

## Scope

**In scope:** the one field added to the rule, the gate-and-routing removal across
`asapp-review-task`, one sentence in `asapp-review-version`, two bullet edits in `todo.md`, one clause
in `asapp-close-task`, and the `TODO.md` tick plus subtask promotion.

**Out of scope:**

- **`asapp-resolve-review-issues`.** The promoted triage-gate subtask owns reading the **Defer** note,
  and it puts the question before exploration. Until it lands, the resolve skill walks every un-ticked
  finding as it does today and the developer declines the ones they do not want fixed.
- **The `Applied:` bullet** — its own subtask, immediately after the gate.
- **`asapp-close-task`'s Step 7 wording** — the *Generalize close-task's doc-commit step wording*
  subtask owns the title, progress entry, and commit template. Only the stale reason clause at line 53
  is corrected here, because this change is what makes it false.
- **Summary table columns**, the report title and anchor line, and the counts line. `Kind`, `Effort`,
  and `Impact` all stand.
- **`code-reviewer.md` and `security-auditor.md`.** Format specs stay out of agent bodies.
- **Existing reports.** `docs/reviews/` holds none on this branch; nothing is retrofitted.

## Verification

- `grep -rn "apply now\|apply-now\|[Dd]isposition" .claude/ CLAUDE.md TODO.md` returns nothing.
- `grep -n "AskUserQuestion" .claude/skills/asapp-review-task/SKILL.md` returns nothing — the skill
  runs start to finish without a prompt.
- No line in `asapp-review-task` describes a write to `TODO.md`; the only surviving mentions are the
  task lookup in Usage / Step 1 and the *log nothing to `TODO.md`* guardrail.
- `.claude/rules/review-report.md` names five fields, with **Defer** capped at 20 words and marked
  optional in the same bullet as **Watch**; it stays under ~100 lines.
- **No skill restates the field.** Neither its cap nor its suggestion-not-routing semantics appears
  anywhere under `.claude/skills/`; both live only in the rule.
- `asapp-review-version`'s Step 3 and Step 4 dispatch prompts are byte-identical to before.
- `.claude/rules/todo.md` no longer names `asapp-review-task`, and its **Review findings** section
  keeps its one remaining bullet.
- All three skills keep their `skill-authoring.md` section order and stay under 500 lines;
  `asapp-review-task` gets shorter.
- No production code, resources, or build files change: no build or test impact.

# Trim What the Review and Resolve Skills Show in Chat

**Status**: Designed

## Context

Three chat surfaces are in scope, and they fail in two different ways.

**The review skills print too much.** `asapp-review-task` Step 3 (line 78) prints a detail block per
finding underneath the summary table. On the eight-finding reference report that is roughly forty
lines of `Where / What / Fix / Watch` restating a table the developer has already read, before a
single disposition is chosen. `asapp-review-version` Step 5 already resolved this at line 100 —
*"each section's summary table in chat; the full report (detail blocks included) is written in Step
6"* — but its closing bullet at line 104 points at `.claude/rules/review-report.md`, the rule whose
job is to define detail blocks. The step contradicts itself and the wrong reading is the verbose one.

**The resolve skill prints the wrong thing.** `asapp-resolve-review-issues` Step 2c.1 assembles a
what / why / where bullet list from (a) and (b), and in report mode line 62 instructs it to *"lift
this from the finding"*. A finding is capped at 30 words and was written by a reviewer holding the
whole file in context. Restating it carries no information the developer did not already read in the
report. Meanwhile step (b) dispatches `Explore`, which does go and read the code, and its report is
compressed into those same bullets and discarded.

The same issue is then stated up to three times before one decision: step (a) prints its intent in
one or two lines, the main context summarizes (b), and (c.1) prints the block.

### The reframe

The subtask is titled *trim*, and its notes at `TODO.md:49–51` propose a one-line gist per finding
and *"a few plain bullets"* for the Propose block. Both were tested against the developer during
design and both were rejected.

- The gist duplicates the table's **Title**, which already names the defect in ten words. Chat gets
  the table and nothing else.
- The Propose block's problem was never its length. A developer resolving an issue does not hold the
  code in mind, and three bullets paraphrasing a terse finding make that worse, not better. **Length
  was the symptom; provenance was the cause.** The block was built from the finding instead of from
  the code.

So this task cuts one surface and rebuilds the other. `TODO.md:49–51` are superseded and are removed
with the subtask.

## The decision

**Chat gets the summary table from the review skills. The resolve skill's context block becomes a
four-part briefing generated from `Explore`, never lifted from the finding.**

Three files change. No file is created; `.claude/rules/review-report.md` is untouched.

| File | Change |
|---|---|
| `.claude/skills/asapp-review-task/SKILL.md` | Step 3 loses its per-finding detail block |
| `.claude/skills/asapp-review-version/SKILL.md` | Step 5's Findings bullet stops contradicting line 100 |
| `.claude/skills/asapp-resolve-review-issues/SKILL.md` | Step 2a goes silent, 2b's dispatch changes, 2c.1 becomes the briefing, and `## Sources` becomes a `## Reference` section that defines it |

Four forces carry the resolve-skill change, and each fixes a distinct failure:

- **The briefing is built from (b), never from the finding.** This is the load-bearing change. A
  pointer restated is still a pointer; only the exploration has anything new to say.
- **Real code, not a description of it.** *"The two writes are not in one transaction"* costs the
  reader a scroll and a reconstruction. Six quoted lines cost neither.
- **Fixed parts with countable caps.** *"As many bullets as the decision needs"* has no failure
  state; four labelled parts at ≤10 lines or 2–3 sentences each do.
- **One print per issue.** Step (a)'s intro goes silent, so the briefing is the only place the issue
  is stated.

### The four parts

| Part | Its job | Cap |
|---|---|---|
| **What we have** | the site, and the real code at it | ≤10 lines of code |
| **What's wrong** | the defect and its direct outcome | 2–3 sentences |
| **Why** | the rule or standard the code breaks | 2–3 sentences |
| **How to fix** | the recommended approach as a sketch, then its blast radius | ≤10 lines of code |

Three style constraints, each traceable to a concrete failure caught during design:

- **Name every subject outright.** *"The two writes"* forces the reader back up to the code block to
  learn which two. No sentence may require scrolling up to parse.
- **Stop at the direct, verified outcome.** The draft ran *"an account with no credentials that
  can't log in and blocks re-registration on that username"* — three consequences deep, the last one
  assuming a unique constraint nobody had checked. Speculation dressed as detail.
- **No em dashes.** Colon after each label, plain sentences inside. Scoped to the briefing output,
  not to the skill file's own prose.

**Why** cites in a fixed order: a `.claude/rules/*` rule, then a named standard, then a URL only when
neither exists. Without it `Explore` has web access and every issue grows a research step, when most
are settled by a project rule.

### What is knowingly lost

**Detail at `asapp-review-task`'s selection point.** Step 4 asks for dispositions and Step 5 writes
the report, so once the detail blocks go, the developer selects from ten-word titles with the detail
on no surface they can read. Accepted deliberately: the subtask at `TODO.md:52` auto-generates the
full report from both skills, which puts the detail on disk before the question is asked. Adding a
stopgap here would be deleted by that subtask.

**Step 2a's printed intro.** Accepted: it stated the issue a second time. The subtask at
`TODO.md:63` deliberately re-adds a print at that position for its triage gate, which answers a
different question — *is this worth doing?* rather than *how do we fix it?* — and reads the finding
as written rather than the code.

**A shorter Propose block.** The briefing runs longer than the three bullets it replaces. Accepted:
this was the developer's explicit call. The block is the decision surface and must carry enough to
decide on.

### Alternatives rejected

- **A one-line gist per finding under the review tables** — the wording at `TODO.md:49`. Rejected by
  the developer: the **Title** column already names the defect, so the gist restates it one row
  lower.
- **Keep three bullets, cap them harder.** The failure was not length. A tighter paraphrase of a
  30-word finding still carries nothing the developer has not read.
- **Give findings briefing-grade detail in `.claude/rules/review-report.md`.** A finding is a pointer
  plus the reviewer's verdict — enough to identify the issue, aim `Explore` at the code, and judge
  whether it is worth doing at all. A briefing is a decision surface generated from the code at the
  moment of choosing. Different jobs, different shapes. It would also churn a format the subtask at
  `TODO.md:47` settled two commits ago, and terse findings are what keep `TODO.md:63`'s triage gate
  free of duplication with the briefing.
- **Show the fix as a full patch rather than a sketch.** The chat version and the subagent's version
  drift, and the fix gets authored twice.
- **Show code for every option, not just the recommended one.** Three patches per issue restores the
  wall of text. Alternatives exist to be compared, not implemented.
- **Carry a worked example in the skill.** It is the strongest form of instruction, but it means
  pasting real classes into `SKILL.md`, which `rule-authoring.md` forbids on drift grounds, and the
  house style for format specs (see `review-report.md`'s own detail block) is an angle-bracket
  template. The template plus the caps carries the shape.
- **Put the briefing spec in a sibling reference file.** `skill-authoring.md` pushes out material
  that is heavy or *shared*; this is ~25 lines used by one skill. `## Reference` is the template's
  home for skill-specific output formats.
- **Write the report before Step 4 so the detail is available at selection.** That is the subtask at
  `TODO.md:52`, not this one.

## `.claude/skills/asapp-review-task/SKILL.md`

| Line | Element | Change |
|---|---|---|
| 73 | Step 3, table intro | *A **summary table** sorted by severity (highest first) — the scannable index:* becomes *A **summary table** sorted by severity (highest first) — the whole chat output:* |
| 78 | Step 3, detail blocks | **Deleted**, with its blank line. The fields still reach the report: Step 2 already requires the reviewer to capture them per `.claude/rules/review-report.md`, and Step 5 writes them out. |
| 75–76, 80–81 | the table header, the legend bullets | **Unchanged.** The **Disposition** column carries Step 4's question and stays. |
| — | Unchanged | Steps 0–2, Steps 4–6, Delegation, Guardrails. |

## `.claude/skills/asapp-review-version/SKILL.md`

| Line | Element | Change |
|---|---|---|
| 104 | Step 5, *Findings* | *see `.claude/rules/review-report.md` (summary column **Theme**)* becomes *each severity section's **summary table** only (summary column **Theme**); the detail blocks belong to the written report, per `.claude/rules/review-report.md`* |
| 100 | Step 5, opening line | **Unchanged** — it is already correct and is the reading line 104 must stop undercutting. |
| — | Unchanged | Steps 0–4, Steps 6–7, Delegation, Guardrails. |

The version skill needs one edit rather than a deletion because it never printed detail blocks by
design; it only left a bullet that could be read as licensing them.

## `.claude/skills/asapp-resolve-review-issues/SKILL.md`

### Step 2, the per-issue loop (57–63)

| Line | Element | Change |
|---|---|---|
| 57 | **a. Understand** | *read it; state its intent and purpose in one or two lines. If genuinely unclear, …* becomes *read it. If genuinely unclear, **stop and ask** (`AskUserQuestion`) before exploring. Print nothing; the briefing in (c) is the single print.* The ask-when-unclear gate survives; only the print goes. |
| 58 | **b. Explore (delegate)** | *It returns a concise findings report — not file dumps.* becomes *Ask it for the briefing's raw material (see **Issue briefing**): the code at the site, the failure and its direct outcome, the project rule or standard the code breaks, and the fix approach with what it touches. Not file dumps.* |
| 59 | 2b's report-mode sub-bullet | **Unchanged** — the finding's **Where** still aims the exploration; that is the finding's remaining job. |
| 61 | **c. Propose**, item 1 | Replaced: *Print the briefing (plain text, before any `AskUserQuestion`) — the four parts of **Issue briefing**, built from (b).* |
| 62 | 2c.1's report-mode sub-bullet | **Deleted** and replaced by one clause on line 61: ***Never lift it from the finding**; the finding only aimed the exploration.* |
| 63 | **c. Propose**, item 2 | *naming each trade-off briefly* becomes *recommended first, one line of trade-off each*. The **Watch** sentence stands. |
| — | Unchanged | Step 1, Steps 2d–2i, Step 3, the Sources content, Delegation, Guardrails. |

Nothing is added to Guardrails. *Never lift from the finding* lives in its Process step;
`skill-authoring.md` gives each rule one home.

### The `## Reference` section

`skill-authoring.md`'s template reserves `## Reference` for skill-specific output formats and
mapping tables, which is what the existing `## Sources` section already is. Rather than add a second
reference-shaped section beside it, **`## Sources` is renamed to `## Reference` and its body demoted
to `### Sources`**, with `### Issue briefing` added alongside. The two in-text pointers (Step 2g and
the skill's opening line) read *"see **Sources**"* and still resolve; no other edit follows from the
rename.

````markdown
## Reference

### Sources

<the existing Sources body, unchanged: intro line, the two-row table, the two bullets>

### Issue briefing

Printed once per issue at Step 2c.1, built from (b)'s exploration. Four parts, in order:

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
| What's wrong | 2–3 sentences |
| Why | 2–3 sentences |
| How to fix | ≤10 lines of sketch — the key lines, never a full patch — plus one sentence of blast radius |

- **Why cites in this order:** a `.claude/rules/*` rule, then a named standard, then a URL only when neither exists.
- **Name every subject outright** — never "the two writes", "it", "the above". No sentence may require scrolling up to parse.
- **Stop at the direct, verified outcome** — no chained downstream consequences, no speculation.
- **No em dashes in the briefing** — colon after each label, plain sentences inside.
````

The `<n>/<total>` counter is the one thing the briefing carries that the issue itself does not: the
loop processes issues strictly in order and the developer has no other position marker.

## Referrer sweep

| File | Line | Edit |
|---|---|---|
| `TODO.md` | 48 | Mark the subtask `- [X]` and drop its three superseded Notes (49–51). |

No other file constrains these chat surfaces. `grep -rn "detail block\|context block\|lift this
from" .claude/ CLAUDE.md` returns only the five lines named above plus
`.claude/rules/review-report.md:12`, which describes the **report file** and is correct as written.
`Explore` is a built-in agent, so its brief lives in the dispatching skill; `.claude/agents/**`
mentions no chat surface.

## Scope

**In scope:** two edits in `asapp-review-task`, one in `asapp-review-version`, five in
`asapp-resolve-review-issues` plus its `## Sources` → `## Reference` rename and the new
`### Issue briefing` block, and the `TODO.md` line above.

**Out of scope:**

- **`.claude/rules/review-report.md`.** Byte-identical. The finding format is deliberately not
  changed — see *Alternatives rejected*.
- **Everything the resolve loop prints after the question** — the approval diff at Step 2f, the
  subagent's fix report at 2e, and the commit message at 2h. Raised during design and explicitly
  parked by the developer.
- **The auto-generated findings report** — `TODO.md:52`. It is what closes the selection-detail gap
  this task accepts.
- **The `Applied:` bullet** — `TODO.md:55`.
- **Commit-sized review outcomes** — `TODO.md:58`.
- **The triage gate** — `TODO.md:63`. It re-adds a print at Step 2a on purpose; this task only
  removes the redundant one that exists today.
- **Summary table columns** in either review skill. **Kind** is load-bearing in
  `asapp-review-task`'s core principle and **Disposition** carries Step 4's question.
- **Report titles, anchor lines, and disclaimers.** Skill-owned and unrelated to chat output.

## Verification

- `asapp-review-task` Step 3 instructs the table and nothing else; no step in the file tells the
  agent to print finding fields in chat.
- `grep -n "detail block" .claude/skills/asapp-review-task/SKILL.md` returns nothing.
  `asapp-review-version:100` **keeps** its mention — it says detail blocks go to the report — so
  grep that file by hand rather than asserting zero hits.
- `asapp-review-version` Step 5 yields *summary table in chat* under both readings; line 104 no
  longer points at the rule as a chat instruction.
- `grep -n "lift this from" .claude/skills/` returns nothing.
- `asapp-resolve-review-issues` Step 2a contains no instruction to print, and Step 2c.1 is the only
  step in the loop that prints before the question.
- The briefing's parts, caps, and style rules appear in exactly one place — `### Issue briefing`. No
  Process step restates a cap.
- `asapp-resolve-review-issues` has no `## Sources` heading; its content survives verbatim under
  `### Sources`, and both *"see **Sources**"* pointers still resolve.
- `.claude/rules/review-report.md` is unchanged: `git diff --name-only` does not list it.
- All three skills keep the `skill-authoring.md` section order, and
  `asapp-resolve-review-issues` stays well under 500 lines (126 → 146).
- `TODO.md:48` is `- [X]` and lines 49–51 are gone; the sibling subtasks at 52, 55, 58, 61 and 63
  are untouched.
- No build or test impact: no production code, resources, or build files change.

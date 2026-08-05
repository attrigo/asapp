# Retire the Code Reviewer's Rule-Routing Table

**Status**: Designed

## Context

`code-reviewer.md` lines 29–43 carry a block called *Project rule routing* — fourteen hand-written
rows copying the `paths:` frontmatter out of `.claude/rules/*.md`:

```
- `maven.md`: paths `**/pom.xml`
- `domain-design.md`: paths `**/domain/**/*.java`
```

It is the only place in the repo that duplicates rule globs, and `agent-authoring.md` grants it a
named exemption to do so (line 75):

> Sole exception: `code-reviewer` enumerates `.claude/rules/*` globs, since it works from
> `git diff`, not file reads.

The subtask at `TODO.md:43` reads *"reconcile the code reviewer's rule routing with the rule globs"*.
Auditing all nineteen rules against the fourteen rows found the copy already stale:

1. **One row is wrong.** `configuration.md` declares `central-config/*.properties`; the table says
   `central-config/application*.properties`. Commit `2b0fd595` widened the rule's glob **and edited
   `code-reviewer.md` in the same commit**, without syncing the copy. The table now under-routes
   every non-`application*` properties file in `central-config/`.
2. **Five rules have no row.** `agent-authoring.md`, `rule-authoring.md`, `skill-authoring.md`,
   `review-report.md`, and `todo.md` are absent. On a branch touching only `.claude/**` and
   `TODO.md` — the shape of the last three commits — the table routes the reviewer to nothing, and it
   reviews on generic instinct while three rules written for exactly those files sit unread.

Reconciling row by row would fix both. The question this task actually settles is whether the rows
should exist at all.

## The decision

**Delete the table. Rely on the read-triggered rule load that already fires.**

The exemption rests on one premise: this agent never reads files. That premise is false.
`asapp-review-task` Step 2 tells every reviewer *"read the full changed files, not just the hunks"*;
`asapp-review-version` Step 3 says the same. Reading a changed `pom.xml` loads `maven.md` into the
reviewer's own context, which is precisely what the table re-derives by hand.

So the copy is not the mechanism — it is a second, drift-prone description of a mechanism that works
without it. Two lines up from the exemption, the same rule states the general case:

> **Don't restate auto-loaded context.** `CLAUDE.md` and matching rules load into every subagent;
> never repeat their content or list rule paths.

Deleting the table makes that absolute and removes the only carve-out in it.

### What has to replace it

Nothing about coverage — everything about *how* the file is opened. Rule load is triggered by the
`Read` tool. It does **not** fire for content pulled through Bash: `git show`, `git diff -- <path>`,
`cat`. A diff-driven reviewer holding Bash can inspect a changed file and never trigger its rule.

The table was quietly papering over that gap. Removing it without naming the mechanic would widen
it. So one behavioral instruction replaces fifteen rows of data:

> Open each changed file with `Read`, not through a shell command — that is what loads the rules
> governing that path.

That instruction cannot go stale, because it names no glob.

**It states the tool, not the depth.** Rule load is matched on the `Read` invocation's path, so an
`offset`/`limit` read triggers it exactly as a whole-file read does. How deep to read is review
*depth* — `asapp-review-task` Step 2 and `asapp-review-version` Step 3 both set it explicitly, and
scaling it to diff size is `TODO.md:50`. `agent-authoring.md` forbids a body restating skill steps,
so the body carries the mechanic and states no depth at all, not even a pointer to where depth is
set.

### What is knowingly lost

**Deleted paths.** A file the diff removes cannot be read, so no rule loads for it. Accepted rather
than patched: reviewing a deletion means confirming referrers were swept, not auditing the removed
file's compliance. A fallback that re-derives globs from rule frontmatter would restore it, and is
deliberately not proposed — it reintroduces the routing detail this task removes, to serve a case
that does not need it.

**Advance knowledge.** The reviewer no longer knows which rules exist before it starts reading. It
does not need to: it reads the file, then reviews it.

### Alternatives rejected

- **Fix the fourteen rows and add five more.** The literal reading of the subtask. Cheap today,
  stale again on the next glob edit — and the precedent says that edit will not remember the mirror,
  because `2b0fd595` had both files open and still missed it. It also keeps a nineteen-row table in a
  body that `agent-authoring.md` caps at ~240 lines.
- **Keep the table, add a sync check to `claude-docs-maintainer`.** Moves the drift from silent to
  detected, at the cost of a standing audit obligation for data that need not be duplicated.
- **Have the reviewer derive the table at review time** (list `.claude/rules/`, grep the `paths:`
  frontmatter, glob-match every changed path). Never drifts and covers deleted paths, but spends a
  discovery pass rebuilding what the harness hands over for free on the reads the reviewer performs
  anyway.

## `code-reviewer`

Every edit is a deletion or a rewording; no block is added.

| Line | Element | Change |
|---|---|---|
| 13 | `When invoked` step 2 | `Map each changed file to applicable .claude/rules/*.md via path globs` becomes: open each changed file with `Read`, which loads the rules governing that path. Still 4 steps; step 1 still context discovery. |
| 20 | Checklist | `Rule-mapping coverage explicit per changed file` becomes `Rule coverage stated per changed file` — the discipline survives, the table's vocabulary does not. Stays 10 bullets. |
| 29–43 | *Project rule routing* | Deleted whole, with its trailing blank line. |
| 70 | *Diff interpretation* | `Path-based rule routing` becomes `Rule load per changed path`. |
| 75–76 | *Rule-mapping discipline* | Retitled *Rule-selection discipline* — nothing maps any more; what the block governs is which loaded rule to cite. `Path-glob matching per file` becomes `Governing rule loaded by reading the file`. The other seven bullets are citation discipline and stand unchanged. |
| 159 | Phase 1 prose | `route each path to the applicable project rules` becomes: open each changed file with `Read` so the rules governing it load. No depth claim. |
| 164–166 | *Preparation priorities* | `Map paths to rule globs` → `Each changed file opened with Read`; `Load matched rule text` → `Loaded rules noted per path`; `Note unmatched paths` → `Note paths no rule governs`. |
| 171–179 | *Rule mapping* | Retitled *Rule coverage*. `Glob-match every path` → `Every changed path accounted for`; `Unmapped path flagged` → `Ungoverned path flagged`; `Mapping table emitted` → `Coverage table emitted`. The remaining five bullets stand. |
| 214 | Phase 3 checklist | `Unmapped paths called out` becomes `Ungoverned paths called out`, matching the phase 1 wording. |
| — | Unchanged | `name`, `description`, `tools`, `model: sonnet`, `color: orange`, the opening paragraph, and the *Rule citations* block at 222–231 (pure citation discipline, no routing). |

Domain blocks go 12 → 11, inside the 5–12 band. Expected landing: 241 → ~225 lines, inside the
150–240 band.

## Referrer sweep

The exemption is referenced in exactly one file; the wider `code-reviewer` referrers (five agents,
four skills, `docs/superpowers/README.md`) describe its role, never its routing, and need no edit.

| File | Line | Edit |
|---|---|---|
| `.claude/rules/agent-authoring.md` | 75 | Delete the `Sole exception: …` sentence from the *Stack-agnostic* bullet. The rule becomes absolute, with no agent exempt. |
| `.claude/rules/agent-authoring.md` | 77 | Sharpen the *Don't restate auto-loaded context* bullet: `matching rules load into every subagent` becomes `path-matching rules load into every subagent on read`. The prohibition now states the mechanic it depends on. |
| `.claude/agents/claude-docs-maintainer.md` | 138 | `Work with code-reviewer on the rule routing it needs to review an instruction-surface diff` becomes `Work with code-reviewer on the glob coverage an instruction-surface diff depends on`. The reviewer is no longer handed routing; what it depends on is globs that actually reach their surfaces — which this agent owns. |
| `TODO.md` | 43 | Mark the subtask `- [X]`. |

## Scope

**In scope:** the nine `code-reviewer` edits and the four sweep rows above.

**Out of scope:**

- **`configuration.md`'s glob itself.** `central-config/*.properties` is correct as written; only the
  copy of it was wrong, and the copy is being deleted.
- **Which reviewer handles a markdown-only diff.** Deleting the table admits all nineteen rules,
  including the three authoring rules, so `code-reviewer` gains the routing an instruction-surface
  diff needs. Whether `claude-docs-maintainer` should review such a diff *instead* is the dispatch
  question at `TODO.md:46`.
- **Read depth.** The body names the tool that loads the rule and leaves depth to whichever skill
  dispatches it. Both review skills instruct full-file reads today; making that proportional to the
  diff is `TODO.md:50`.
- **The sibling spec** (`2026-08-05-review-roster-reconciliation-design.md`), which states the table
  stays and defers it here. A landed spec records what was designed then; it is not retro-edited.
- **A `TODO.md` Decisions entry.** The parent note routes a keep-as-is outcome to Decisions. This is
  not keep-as-is — a block is deleted and a rule loses its only exception — so this spec is the
  record.

## Verification

- `grep -rn '\*\*/\*' .claude/agents/` returns nothing: no agent body carries a rule glob.
- `grep -rn "not file reads\|Sole exception" .claude/` returns nothing.
- `code-reviewer.md` still matches `agent-authoring.md`'s seven-part skeleton in order, with counts
  honored: 4 `When invoked` steps, 10 checklist bullets, 11 domain blocks, 3 workflow phases, a
  `Delivery notification` string, 6 Integration bullets, a closing priority statement.
- The body names `Read` as the load trigger and still requires a coverage statement, but states no
  glob and no rule filename outside a citation instruction.
- Frontmatter keys stay in order with `tools` inline; no em-dashes introduced.
- Terminology is uniform across the reworded bullets: *ungoverned* for a path no rule matches,
  *loaded* for a rule that reached the reviewer.
- No build or test impact: no production code, resources, or build files change.

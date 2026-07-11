---
paths:
  - ".claude/skills/**/*.md"
---

# Skill Authoring

The repo standard for `.claude/skills/**/SKILL.md`. Aligns with the `superpowers:writing-skills` skill and Anthropic's [skill best-practices](https://platform.claude.com/docs/en/agents-and-tools/agent-skills) — read those for rationale; apply this.

## Template

Fixed section order. Optional sections appear only when the skill needs them.

```markdown
---
name: asapp-<verb>
description: >          # third person · what + when · Triggers + Do NOT · no step enumeration
  ...
---

# <Title>

<One sentence: what it does.> **Core principle:** <one sentence, when a load-bearing principle exists.>

## Usage
- `/asapp-<verb>` — ...
- `/asapp-<verb> <arg>` — ...

## Process
### 1. <Imperative step>
...

## Reference        (optional — skill-specific specs: output formats, recipes, mapping tables)
## Delegation       (optional — the agent/tool table, when the skill delegates)
## Guardrails       (the must-not-violate invariants only)
## Common mistakes  (optional — see Conventions)
```

## Freedom tier

Match specificity to fragility ("degrees of freedom"). Set the tier per skill; it governs how terse the prose gets.

- **High freedom** — judgment / heuristic work (review, refine, prepare). Give direction, trust the agent. Terse text; no step-by-step hand-holding.
- **Low freedom** — fragile exact sequences (release, close-task, resolve). Keep commands exact, ordered, explicit. Trim the prose around them; never vaguen the commands.

## Style

- **Imperative, second person, declarative.** No rationalization prose. Bullet-phrase brevity; label-then-list.
- **Say it once.** Each rule lives in one home — its Process step, or Guardrails. Never restate it across Process + Guardrails + a mistakes table.
- **Guardrails = invariants only.** The never / always constraints that cause real damage (never push, never commit to main, review-only). Not a mirror of Process.
- **Reference, don't restate.** Conventions in `.claude/rules/*` (scopes, TODO wording / shapes) are linked, never re-explained. A format shared between skills lives in one reference file both link to.
- **Tables for tabular data only** — input→output maps (module→scope, plan-state→handling). Not for restating rules as "mistakes".
- **Trust the reader.** Agents are smart; add only what they don't already know. Cut any line that doesn't justify its token cost.
- **Stable terminology.** One term per concept throughout.

## Conventions

- **Progress tracking** — a Process opens with a distinct `### 0. Set up progress tracking` step by default. Omit it only when the process is **straightforward** (single-pass, e.g. `asapp-draft-commit-msg`) or is built on a **loop that's hard to track** as discrete tasks (e.g. `asapp-resolve-review-issues`).
- **Common mistakes** (optional) — only genuinely counterintuitive traps, capped ~5. Delete any row that restates a Process step or Guardrail.
- **Progressive disclosure** — push heavy or shared reference material into a sibling file, linked one level deep from SKILL.md. Give a file over ~100 lines a short contents list.
- **Naming** — keep the `asapp-<verb>` name; it is the slash-command id. Never rename for style.
- **Description** — third person; what + when; include Triggers and Do-NOT. Do not enumerate the Process steps — agents follow an enumerated description instead of reading the body.
- **Paths** — forward slashes only.

## Size

Keep SKILL.md under 500 lines. Length is an outcome of the rules above, not a target to fill.

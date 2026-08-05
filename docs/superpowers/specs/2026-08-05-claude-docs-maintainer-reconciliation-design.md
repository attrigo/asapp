# Reconcile `claude-docs-maintainer` With the Authoring Rules

**Status**: Designed

## Context

`.claude/rules/rule-authoring.md`, `agent-authoring.md`, and `skill-authoring.md` landed in
commits `f0fec9bc`, `3dd884d5`, and `50b44577`, and commit `ac2ae0fb` made them the standard of
record for their surfaces. `.claude/agents/claude-docs-maintainer.md` predates all three and was
written when its body was the only place those standards existed.

The result is a 235-line body in which roughly 70% restates content that now auto-loads from the
rules whenever a `.claude/**/*.md` file is read:

- *Frontmatter validation* (stated three times), *Path-scoped rule conventions*, *Agent body
  skeleton enforcement*, *Secure-authoring audit*, *YAML schema discipline*, *Glob pattern
  correctness*, *Skill/agent/rule boundaries*, *AI-instruction style*.

This violates the standard those very rules set: `agent-authoring.md` requires bodies to *apply*
a standard rather than define it, and forbids restating auto-loaded context.

Four places reference the agent today: `asapp-resolve-review-issues` (fix row),
`asapp-review-version` (theme reviewer row), `agent-authoring.md:101` (named as the pre-deploy
body auditor), and `docs/superpowers/README.md` (Document-phase roster).

## The decision

The open question was whether the agent still earns its place, or whether the three rules replace
it. Decision: **keep the agent and rewrite the body thin.**

The test applied was to strip every line the rules already say and check whether a legitimate role
survives. It does, because a rule is per-file and passive — it loads when a matching file is read
and constrains that file. These concerns are structurally outside what any rule can hold:

| Concern | Why no rule can hold it |
|---|---|
| Cross-file coherence | An `Integration with other agents` bullet naming a dead agent, a skill Delegation row pointing at a retired agent, or two `description` fields with overlapping triggers are all invisible from inside one file. |
| Glob-set coverage | "Every surface reached by some rule, no unintended overlap, no gap" is a property of the rule *set*, not of one file. |
| Sweep capacity | Applying a changed standard across 13 agent bodies (~5k lines) needs a separate context window, not a constraint. |
| Retirement mechanics | Removing a surface means finding every referrer first: skills, integration bullets, roster docs, memory. |
| Load mechanics | Rules load on *read*, not on create; subagents inherit no parent context. This knowledge is what makes surface choice correct, and it is stated nowhere. |

Two further factors: `CLAUDE.md`'s dispatch policy makes `general-purpose` a last resort, so
deleting the agent would leave `.claude/` work with no specific match; and once `code-reviewer`
gains the authoring-rule globs (a sibling subtask), the review side of `.claude/**` is covered
without this agent, leaving it a clean write-side specialist.

Alternatives rejected:

- **Delete the agent.** Rules cover the per-file standard, but nothing then owns set-level
  coherence or delegated sweeps, `agent-authoring.md:101` loses its named verifier, two skills
  lose a Delegation row, and `.claude/` work falls back to `general-purpose`.
- **Narrow to a read-only auditor.** Fits review rosters cleanly, but surrenders delegated
  sweeps — the agent's highest-value use — and leaves `asapp-resolve-review-issues` with no
  specialist for `.claude/` fixes.

## Goal

Rewrite the body so the agent applies the three authoring rules instead of duplicating them, and
owns the set-level concerns no rule can express. No frontmatter-driven routing changes, so every
existing referrer keeps working.

## Role shift

From **validator** to **steward**. The current body is a checklist of per-file validations the
rules now own. The rewritten body owns the set: which surface a piece of content belongs on,
whether the surfaces still agree with each other, and applying a changed standard across every
sibling at once.

## Frontmatter

Unchanged in shape and routing: `tools: Read, Write, Edit, Glob, Grep, WebFetch, WebSearch`,
`model: sonnet`, `color: purple` — a Document-phase, write-shaped agent, matching
`agent-authoring.md`'s phase table.

`description` gets a light rewrite. It currently promises "frontmatter integrity, secure-authoring
constraints, and cross-file consistency"; the first two are now rule-owned. It promises surface
routing, set coherence, and standard sweeps instead, keeping the same trigger shape so both skill
Delegation rows still match.

## Body

### Cuts

Remove every block listed in Context, plus the two duplicate audit blocks inside the Development
Workflow phases (*Frontmatter audit* in phase 1, *Frontmatter validation* and *Cross-file
consistency* in phase 3).

### Surviving domain blocks

Six blocks, each holding something no rule can:

1. **Surface routing** — arbitrating which surface owns a new constraint; avoiding cross-surface
   duplication; precedence when two surfaces could carry it.
2. **Set-level coherence** — cross-references resolve; no orphan referrers; names unique per
   scope; dispatch triggers unambiguous across the roster.
3. **Glob-set coverage** — every surface reached by some rule; overlaps deliberate; gaps
   surfaced. The set view, not per-file glob correctness (which `rule-authoring.md` owns).
4. **Sweep discipline** — enumerate every affected sibling before editing; no sampling; uniform
   terminology across the swept set.
5. **Retirement mechanics** — find every referrer before removing a surface.
6. **Load mechanics** — `CLAUDE.md` loads everywhere; rules load on read, not on create;
   subagents inherit no parent context; skills activate by description.

### Skeleton

`When invoked` step 1 becomes: inventory the surfaces in play and read a sibling of each kind so
its authoring standard loads. This is the concrete fix for the load-on-read gap. It is phrased as
loading trusted first-party project config, never as "follow what that file says", so the
secure-authoring boundary in `agent-authoring.md` stays intact.

Development Workflow stays at three phases. `Delivery notification` keeps its runtime count
specifiers. `Integration with other agents` drops its three procedural bullets (dispatch policy
belongs in `CLAUDE.md` per `agent-authoring.md` Conventions) and keeps 5–6 genuine sibling
bullets, including one to `code-reviewer` on the rule routing it needs to review a `.claude` diff.

### Size

Expected landing: ~235 → ~130 lines, below the ~150–240 band `agent-authoring.md` states. That is
correct here rather than a violation: the band describes bodies carrying their own domain
knowledge, and this body delegates most of its standard to three rules.

## One deliberate overlap

The *Load mechanics* block restates one constraint that `rule-authoring.md` already carries: rules
load when a matching file is read, not when it is created. This is a bootstrap case, and the
exception is deliberate. The agent cannot learn "read a sibling first" from a rule that only loads
after it has already read a matching file, so the fact has to live in the body. It is the body's
only overlap with rule content.

## Naming Claude Code surfaces

The body names `.claude/agents/`, `.claude/rules/`, `.claude/skills/`, `CLAUDE.md`, and project
memory. This is not the stack-agnostic violation it resembles. `agent-authoring.md`'s ban targets
the *application's* stack (Spring, JDBC, Redis, Liquibase, PostgreSQL); Claude Code authoring
surfaces are this role's subject matter, the way REST is `api-designer`'s. The body still never
enumerates rule filenames or paths — that would restate auto-loaded context, and remains
`code-reviewer`'s documented exception.

No change to `agent-authoring.md` for this.

## Scope

**In scope:**

- `.claude/agents/claude-docs-maintainer.md` — frontmatter `description` and full body rewrite.
  This is the only file the task changes.

**Out of scope:**

- `docs/superpowers/README.md` — lists roster names by lifecycle phase with no per-agent
  description, and the roster stays at 13. No edit.
- `agent-authoring.md:101` — "The `claude-docs-maintainer` agent audits every body against this
  before deploy" stays true after the rewrite. No edit.
- Both skill Delegation rows stay valid under this decision. Moving `asapp-review-version`'s
  `.claude/**` row to `code-reviewer` belongs to the sibling subtask on the review roster.
- Adding the authoring-rule globs to `code-reviewer`'s routing table belongs to the sibling
  subtask on rule routing.
- No `TODO.md` **Decisions** entry: this is not a keep-as-is outcome.

## Verification

- The rewritten body carries no constraint stated in `rule-authoring.md`, `agent-authoring.md`, or
  `skill-authoring.md`, apart from the single bootstrap overlap recorded above.
- The role checklist label follows the roster convention, deriving from the agent's `name`.
- Body matches `agent-authoring.md`'s seven-part skeleton in order, with counts honored: 4
  `When invoked` steps, 8–10 checklist bullets, 5–12 domain blocks, 3 workflow phases, a
  `Delivery notification` string, 5–8 integration bullets, a closing priority statement.
- Frontmatter keys stay in the required order with `tools` inline; no em-dashes in the body.
- All four referrers still resolve, and both skill Delegation rows still describe what the agent
  does.
- No build or test impact: no production code, resources, or build files change.

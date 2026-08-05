# Reconcile the Review Roster With What the Review Skills Need

**Status**: Designed

## Context

The review roster is three agents — `code-reviewer` (sonnet), `architect-reviewer` (opus), and
`security-auditor` (opus). `asapp-review-task` dispatches the first two on *every* task branch and
adds the third for security-relevant diffs; `asapp-review-version` uses all three as theme
specialists. Each subagent gets its own context, so a task review reads the same diff and the same
files two or three times over, at two model tiers.

The driving concern is that cost: two reviewers firing on every diff, with enough output overlap
that `asapp-review-task` Step 2 needs a dedupe step to merge them. The question is whether the
extra findings justify the latency and tokens.

Auditing the bodies against the skills that dispatch them surfaced five mismatches that predate the
cost question:

1. **Direct contradiction.** Both skills tell every reviewer *"judge the code on its own merits —
   ignore specs / plans; no drift findings."* `architect-reviewer` is built on the opposite: its
   `description` ends "drift from the agreed design", its checklist demands "Agreed-design baseline
   cited", phase 1 loads that baseline, and its delivery notification counts drift findings.
2. **Phantom baseline.** `security-auditor` anchors on "the project threat catalog / threat
   checklist" in its body, phase 1, and two domain blocks. No such document exists in the repo.
3. **Stack in a description.** `security-auditor` names "JWT/Redis token-store drift";
   `agent-authoring.md` requires descriptions to enumerate concerns stack-agnostically.
4. **Verbatim duplication.** `security-auditor` repeats *Finding classification* and *Remediation
   guidance* word for word (lines 109–127 ≡ 196–214); `code-reviewer` repeats
   *Community-standard fallback* (75–83 ≡ 232–240) and *Finding severity* (135–144 ≡ 212–220);
   `architect-reviewer` repeats its drift and layering blocks.
5. **Procedural dispatch bullets.** All three carry `Run in parallel with …` and bullets naming the
   `requesting-` / `receiving-code-review` skills. `agent-authoring.md` puts dispatch policy in
   `CLAUDE.md` and restricts Integration bullets to siblings named by `name`; the sibling task on
   `claude-docs-maintainer` already stripped exactly these.

## The decision

**Fold `architect-reviewer` into `code-reviewer`. Keep `security-auditor`.**

The test applied is the sibling spec's: strip everything the rules already carry, and see whether a
role survives.

For `architect-reviewer`, little does. Its four load-bearing lenses — layering integrity,
port/adapter taxonomy, exception placement, transaction scope — are what `architecture.md`,
`ports-adapters.md`, and `error-handling.md` state, and `code-reviewer` already routes all three by
glob. It declines to report those findings today only because its own body says
`Macro and system concerns escalated, not absorbed`. That is a body constraint, not a capability
limit.

The strongest evidence is inside `asapp-review-version` itself: the cross-cutting seam pass
(Step 4), the most structural pass in the whole system, already dispatches `code-reviewer` rather
than `architect-reviewer`. The skill author had already concluded that structural work does not
need the separate agent.

For `security-auditor`, nothing is rule-covered. There is no `security.md` in `.claude/rules/`, and
a threat-class catalog is a poor fit for the per-path declarative shape a rule takes. Its body is
the only home that knowledge has, so deleting it would drop the lens entirely rather than relocate
it.

The roster is therefore **asymmetric on purpose**: architecture is rule-covered so the agent goes;
security is not, so the agent stays.

### What is knowingly lost

`architect-reviewer`'s genuinely non-rule-covered blocks: scalability impact, technical-debt
accrual, cross-service coherence, and module-boundary integrity — plus the opus tier on structural
diffs. These rarely fire on a task-sized diff, and `asapp-review-version` already reaches
cross-service coherence through theming plus the seam pass.

### Alternatives rejected

- **Collapse to `code-reviewer` alone.** Cheapest and simplest, but the security lens has no rule
  to fall back on; recovering it would mean authoring a security rule first.
- **Keep all three, gate dispatch by change nature.** Solves cost by not dispatching rather than by
  deleting — but the gating is already the subtask at `TODO.md:48`, leaving this subtask as body
  cleanup and no roster answer.
- **Fold both, bump `code-reviewer` to opus.** One pass with all three lenses at full depth, but
  every review however trivial then costs opus, working against the motive for the change.

## Model tier

`code-reviewer` **stays sonnet**. What it absorbs is the rule-covered part — layering, ports,
exception tiering — which is systematic citation work, exactly what `agent-authoring.md` assigns to
sonnet. The judgment-heavy architect content is being dropped, not absorbed, so the tier that went
with it is not needed.

That makes `agent-authoring.md`'s Review row stale, so the row is amended rather than the agent.

## `code-reviewer`

The fold is mostly deletion. `architect-reviewer`'s ten domain blocks split three ways: four are
rule content and must not migrate (`agent-authoring.md` forbids restating auto-loaded context),
four are the dropped scope, and the remainder are drift blocks the skills explicitly reject. The
agent gains one block, not ten.

| Element | Change |
|---|---|
| `description` | Widen past "line-level" to cover rule compliance, structural fit, naming, tests, and performance. Keep the cite-a-rule-or-standard clause. |
| Opening paragraph | "line-level diff review" becomes line-level and structural. |
| `When invoked` | Step 3 widens past "line-by-line"; still 4 steps, step 1 still context discovery. |
| Checklist | Delete `Macro and system concerns escalated, not absorbed`. **Keep** `Security concerns escalated, not absorbed` — `security-auditor` survives. |
| New domain block | *Structural fit*, ~8 phrases: dependency direction, layer leaks, port/adapter placement, exception tier, transaction boundary, module boundary, cross-file coherence, blast radius. |
| Dedupe | Keep one copy each of *Community-standard fallback* and *Finding severity*. |
| Integration | Drop `Run in parallel with …`, `Escalate macro and system concerns to architect-reviewer`, and both review-skill bullets. Three survive, so backfill to 5–8 genuine sibling bullets. |
| Unchanged | `name`, `tools`, `model: sonnet`, `color: orange`, and the project-rule routing table. |

The routing table stays because it is `code-reviewer`'s documented exception in
`agent-authoring.md`: the agent works from `git diff`, not file reads, so rules do not auto-load for
it. Extending that table with the authoring-rule globs is the separate subtask at `TODO.md:44`.

Expected landing: ~251 → ~240 lines, inside the 150–240 band.

## `security-auditor`

| Element | Change |
|---|---|
| `description` | Drop `JWT/Redis` (stack) and `drift from the agreed security design` (contradicts both skills). Concerns become: endpoint authentication coverage, filter-chain ordering, token-store drift, credential handling, input-validation gaps, sensitive-data exposure. |
| Opening paragraph | "audit … against the threat checklist" becomes audit against known regression classes. |
| Phantom baseline | Phase 1's "load the threat checklist that the change must be audited against" becomes "enumerate the regression classes the change could touch". The *Threat-checklist audit* block re-anchors on the classes the body itself carries; `Cite threat checklist baseline` and `Project threat catalog` phrases go. |
| Dedupe | Keep one copy each of *Finding classification* and *Remediation guidance*. |
| Integration | Drop `Run in parallel with …`, both review-skill bullets, and `Escalate macro-level findings to architect-reviewer` (dead name). Keep the `security-designer` and `code-reviewer` bullets; backfill to 5–8. |
| Unchanged | `name`, `tools`, `model: opus`, `color: orange`. |

Expected landing: ~225 → ~190 lines.

## Referrer sweep

`architect-reviewer` is named in nine live files, and `agent-authoring.md`'s phase table names its
role without using the dispatch id. Every one is edited in this task; leaving any would point a
skill or agent at an id that no longer resolves.

| File | Line | Edit |
|---|---|---|
| `.claude/agents/architect-reviewer.md` | — | `git rm` |
| `.claude/agents/code-reviewer.md` | 243, 245 | Drop the dispatch and escalation bullets |
| `.claude/agents/spring-boot-developer.md` | 209 | `Output reviewed by code-reviewer and security-auditor` |
| `.claude/agents/test-automator.md` | 205 | `Support code-reviewer and security-auditor with verifiable evidence` |
| `.claude/skills/asapp-close-task/SKILL.md` | 75, 189 | `` `code-reviewer` / `Explore` `` |
| `.claude/skills/asapp-resolve-review-issues/SKILL.md` | 98 | `` `Explore`, or `code-reviewer` `` |
| `.claude/skills/asapp-review-task/SKILL.md` | 55, 112–113 | Step 2 Always becomes `code-reviewer` alone (line-level quality and structural fit); the two Delegation rows collapse into `\| Code quality and structural fit \| code-reviewer \|` |
| `.claude/skills/asapp-review-version/SKILL.md` | 71, 139 | Drop the architecturally-significant escalation clause and the Delegation row |
| `.claude/rules/agent-authoring.md` | 67 | Review row: phase `(code · security)`, model `sonnet (code) · opus (security)` |
| `docs/superpowers/README.md` | 27 | Review roster becomes `code-reviewer`, `security-auditor`; roster 13 → 12 |
| `TODO.md` | 46, 49 | Delete the now-resolved gating Warning; the line-49 note drops `architect-reviewer` |

## Scope

**In scope:** the eleven rows above.

**Out of scope:**

- `docs/superpowers/specs/v0.4.0/**` — archived release specs are a historical record of what was
  designed then, not live routing. No edit, matching how the sibling spec treated its referrers.
- **The dispatch gating itself** (`TODO.md:48`). This task removes a dead name from
  `asapp-review-task` Step 2 because the skill would otherwise dispatch a nonexistent agent. Making
  `security-auditor` conditional on the change's nature, and scaling review effort to diff size,
  stay with that subtask. Its note simplifies to: always run `code-reviewer`; add
  `security-auditor` only for security-relevant changes.
- **`code-reviewer`'s rule-routing table** (`TODO.md:44`) — adding the authoring-rule globs is the
  sibling subtask.
- **Writing a security rule.** `security-auditor` keeps its knowledge in its body; extracting it
  into `.claude/rules/` is not proposed here.
- **A `TODO.md` Decisions entry.** The parent note routes a keep-as-is outcome to Decisions. This is
  not keep-as-is — an agent is deleted and two bodies are rewritten — so the spec is the record.

## Verification

- `grep -rn "architect-reviewer" .claude/ docs/superpowers/README.md TODO.md` returns nothing.
- Both surviving review bodies match `agent-authoring.md`'s seven-part skeleton in order, with
  counts honored: 4 `When invoked` steps, 8–10 checklist bullets, 5–12 domain blocks, 3 workflow
  phases, a `Delivery notification` string, 5–8 Integration bullets, a closing priority statement.
- Neither body restates a constraint from a `.claude/rules/` file, with `code-reviewer`'s routing
  table as the one documented exception.
- No block appears twice within either body.
- Frontmatter keys stay in the required order with `tools` inline; no em-dashes in either body.
- Both skills' Delegation tables resolve to agents that exist, and neither asks a reviewer for drift
  findings any more.
- No build or test impact: no production code, resources, or build files change.

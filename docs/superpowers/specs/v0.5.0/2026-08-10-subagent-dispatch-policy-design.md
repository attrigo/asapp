# Scope the Subagent Dispatch Rule to Agent Choice, Not Count

**Status**: Designed

## Context

Skills dispatched subagents the developer did not expect — too many of them, and some spawned on
Claude's own initiative with nothing having asked for them.

The instruction surfaces are the cause. Three of them carry dispatch guidance, and all three answer
the same narrow question:

`CLAUDE.md:36-37` — *"For every Agent tool call, pick the most specific match from `.claude/agents/`;
`general-purpose` is a last resort."*

Memory `feedback-subagent-dispatch` — the same rule, expanded across *"a Superpowers skill … any
other skill … any process Claude Code runs where it decides — or is told — to spin up a subagent."*

The nine `asapp-*` skills — a `## Delegation` table each, mapping a concern to an exact agent.

**Every one of them answers *which agent*. None answers *whether* or *how many*.** So the two
unanswered questions fell to improvisation, and the memory's phrasing — *"any process where Claude
**decides** … to spin up a subagent"* — reads as tacit permission to make that decision.

The skills compound it unevenly. `asapp-review-version:79` is exact: *"Dispatch **one `code-reviewer`
per theme** … Keep **≤5 running at once**."* `asapp-review-task:55` is not: *"dispatch review
subagents **in parallel** over the branch diff"* — plural, uncounted. One `code-reviewer` over the
whole diff and five of them sliced per file both satisfy that sentence.

## The decision

**Split the one question into three — *whether*, *which*, *how many* — and answer each separately.
Keep the policy in memory, not `CLAUDE.md`.**

Seven files change, all Markdown. No file is created. No agent body is touched.

| File | Change |
|---|---|
| `~/.claude/…/memory/feedback_subagent_dispatch.md` | rewritten — the three axes, now the policy's single home |
| `~/.claude/…/memory/MEMORY.md` | index line carries the operative rule, not a hook |
| `CLAUDE.md` | `## Subagent dispatch` deleted |
| `.claude/rules/agent-authoring.md` | one now-false sentence dropped |
| `.claude/rules/skill-authoring.md` | one authoring clause added |
| `.claude/skills/asapp-review-task/SKILL.md` | the uncounted dispatch step gets its counts |
| `.claude/skills/asapp-close-task/SKILL.md` | one implicit count made explicit |

Three forces carry the change:

- **Three questions, three answers.** Conflating them is what let *which agent* silently license both
  *whether* and *how many*. Naming the axes separately makes each answerable — and makes a silent
  axis an obvious omission rather than an invitation.
- **The instruction that asked is the authority.** A skill knows its own shape: how many themes, how
  many issues, how many units. It decides whether to dispatch and how far to fan out. Claude decides
  only which agent, and only when the skill declined to say.
- **One home, chosen by what the rule is *about*.** A rule about a checked-in artifact belongs in
  git; a rule about how one developer prefers to work belongs in memory. This is the latter.

## Where the policy lives

The policy moves out of `CLAUDE.md` and into memory, on the developer's call. Two reasons, both held
after review:

1. `CLAUDE.md` is loaded into every session unconditionally and is kept as short as possible. Ten
   lines of dispatch procedure is not what that budget is for.
2. It is a personal working convention, not a property of this codebase. Another developer on the
   same repo could hold different dispatch rules without either of them being wrong.

**The counter-argument, and why it did not survive.** `CLAUDE.md` loads deterministically; memory
files load on recall, and the policy must be in context at the moment a dispatch is decided. An
earlier draft also claimed `CLAUDE.md` was needed because it propagates into subagents that
themselves dispatch — **that claim is false.** No agent in `.claude/agents/` carries the `Agent` tool
(verified across all twelve `tools:` lines), and `Explore` and `Plan` are defined as *all tools
except `Agent`*. Only the main context can dispatch, so subagent propagation is irrelevant here.

What remains is the recall risk alone, and it is mitigated in place: `MEMORY.md` — the index loaded
every session — carries the operative rule in compressed form rather than a teaser hook, so the
binding constraint is present even when the full file is not recalled.

## The policy

```markdown
Three separate decisions on every dispatch. Never let one answer another.

**The instruction** is whatever asked for the work — the driving skill (`asapp-*`, or third-party
like Superpowers), or the developer directly.

- **Whether** — dispatch only when the instruction says to. Otherwise work inline; never dispatch on
  your own initiative.
- **Which** — by what the instruction says:
    - **Names agents** — dispatch exactly those, native ones included (`Explore`, `Plan`). Nothing
      else, nothing extra.
    - **Asks without naming** — pick the most specific match across `.claude/agents/` and the native
      agents (`Explore` to search, `Plan` to design); `general-purpose` only when nothing fits.
    - **Names `general-purpose`** — treat as unnamed and pick as above.
- **How many** — one instance per named agent, unless the instruction says to fan out ("one per
  theme", "≤5 in flight").
```

**Why "instruction" and not "skill".** An earlier draft phrased all three axes as "what the
skill says", then added a trailing line claiming the policy also binds plain requests with no skill
running — which left *Which* and *How many* with no subject in exactly that case. Naming the source
once, as either the skill or the developer, removes the contradiction and the trailing line with it.

Two clauses are load-bearing and neither is obvious.

**A named `general-purpose` counts as unnamed.** Superpowers' `executing-plans` and
`subagent-driven-development` both name `general-purpose` outright. Read literally under *Which*,
that would dispatch `general-purpose` and discard the curated roster — reversing a preference held
since 2026-05-25. Treating it as *unnamed* routes it to the specialist match instead, while every
genuinely named agent is still dispatched literally. This is what lets *Which* stay a single rule
rather than a rule plus an exception list.

**An unstated count means one.** The observed failure was excess agents, not wrong ones. Defaulting
to one makes fan-out something a skill must *say*, and the skills that genuinely want it already do —
`asapp-review-version` ("one per theme", "≤5 running at once") and `asapp-prepare-version` ("two
subagents per unit … ≤5 units in flight") are unaffected.

## The surfaces that pointed at the old home

`.claude/rules/agent-authoring.md:106` closes its **Dispatch fit** bullet with *"The dispatch policy
itself lives in `CLAUDE.md`."* Once the section is deleted that sentence points at nothing, so it is
dropped. The rest of the bullet — triggers must truthfully match the role, overlapping agents need
distinct triggers — is unaffected and stays.

The bullet is not re-pointed at memory. A checked-in rule should not cite a personal memory file it
cannot see or validate.

## The authoring clause

`.claude/rules/skill-authoring.md` gains one clause under **Conventions**:

```markdown
- **Dispatch steps** — name the agent and state the count ("one `X` per <unit>, ≤N at once"). An
  unstated count means one.
```

This stays project-level and checked in, even though the policy it complements is personal. It is
hygiene about writing an unambiguous skill: a dispatch step that names neither agent nor count is
underspecified against *any* dispatch policy, not just this one. It is written self-contained for
that reason — it states the default rather than citing where the default comes from.

Its purpose is temporal. The policy fixes how existing skills are read; this clause stops the next
skill from reintroducing the ambiguity.

## The skill audit

All nine `asapp-*` skills were read against the policy. Seven need no change — the vague wording is
concentrated in one step.

| Skill | State | Action |
|---|---|---|
| `asapp-review-task:55` | *"dispatch review subagents **in parallel**"* — names `code-reviewer` and a conditional `security-auditor`, states no count | **Fix** |
| `asapp-close-task:75` | *"Delegate by default to `Explore`"* — count implicit | **Tighten** |
| `asapp-review-version:79,104` | *"one `code-reviewer` per theme … ≤5 running at once"* | none |
| `asapp-prepare-version:50` | *"two subagents per unit … ≤5 units in flight"* | none |
| `asapp-refine-task:52` | *"a **single** `Explore` subagent"* | none |
| `asapp-resolve-review-issues:52,62` | `Explore`; *"the most specific specialist subagent"* — singular, and a deliberate *Which* case | none |
| `asapp-release`, `asapp-draft-commit-msg`, `asapp-improve-changelog` | no dispatch language at all | none |

The three skills with no dispatch language are correct as they stand: under *Whether*, silence means
never dispatch, which is what those single-pass and exact-sequence skills intend.

`asapp-review-task:55` becomes:

```markdown
- **Otherwise** — dispatch **in parallel** over the branch diff (design specs excluded); each returns
  concise findings, not file dumps:
  - **Always** — **one** `code-reviewer` (line-level quality and structural fit).
  - **Only when security-relevant files changed** (auth / security config, JWT / token handling,
    filter chains, crypto, secrets, new endpoints) — **one** `security-auditor`.
```

The count moves onto each sub-bullet rather than collapsing the step into one sentence. The
conditional is what makes this step's count vary — one agent or two — so the count belongs beside the
condition that decides it, and `skill-authoring.md` asks for label-then-list over prose. The plural
"review subagents" is dropped from the lead-in, since the sub-bullets now carry the agents.

`asapp-close-task:75` changes *"Delegate by default to `Explore`"* to *"Delegate by default to a
single `Explore`"*. Redundant under the one-instance default, but this is the skill the developer
runs at the end of every task, and stating it costs two words.

`asapp-review-version:134` — the guardrail *"Delegate all reviewing to subagents"* — was considered
and deliberately left alone. It is an instruction not to review inline, not a dispatch step, and
Steps 3 and 4 already state exact counts.

## What is not done

**The memory entry is rewritten, not deleted.** An earlier draft deleted it on the reasoning that
`CLAUDE.md` would carry the policy. That reversed with the placement decision.

**Two memories stay untouched.** `feedback-mvn-permissions` and
`feedback-compress-flow-small-authoring` are personal by the same test applied here, and neither
concerns dispatch. The rewritten entry links the first, which binds a constraint into every dispatch
prompt.

**No agent body changes.** Dispatch is decided by the caller, never by the agent being called.

**The archived v0.4.0 spec is left as written.** `docs/superpowers/specs/v0.4.0/2026-05-13-claude-code-subagents-design.md`
names the `## Subagent dispatch` directive three times (lines 145, 1218, 1230), including a closing
note that calls it canonical. It is a shipped historical record of what 0.4.0 designed, and it is not
edited — this document supersedes it on dispatch. That is also why the verification below is scoped
to `.claude/`: archived specs are expected to cite surfaces that later moved.

## Verification

The change is entirely Markdown, so there is nothing to compile or test. It is verified by reading:

- `CLAUDE.md` has no `## Subagent dispatch` section, and no other line references one.
- No file under `.claude/` cites `CLAUDE.md` for dispatch policy.
- `MEMORY.md`'s index line states the rule, readable without opening the memory file.
- Every dispatch step across the nine skills names its agent; a count appears wherever it is not one.
- The seven unedited skills are unchanged.

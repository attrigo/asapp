---
paths:
  - ".claude/agents/**/*.md"
---

The repo standard for `.claude/agents/**/*.md` — subagent definitions. Aligns with the `claude-docs-maintainer` agent (frontmatter integrity, body skeleton, and cross-file validation) and Claude Code's [subagents guidance](https://code.claude.com/docs/en/sub-agents); the design spec `docs/superpowers/specs/v0.4.0/2026-05-13-claude-code-subagents-design.md` records the rationale. Read those for validation and rationale; apply this.

Contents: Template · Frontmatter · Body · Secure authoring · Conventions · Size

## Template

Fixed shape: five-key frontmatter, then the body skeleton.

```markdown
---
name: <kebab-case>                                                # matches the filename
description: "Use this agent when <triggers>: <specifics>."       # ~25-40 words, role-level concerns
tools: Read, Write, Edit, Glob, Grep, Bash, WebFetch, WebSearch   # fixed order; drop what the role doesn't need
model: opus | sonnet                                              # reasoning depth (see Frontmatter)
color: blue | green | orange | purple                             # lifecycle phase (see Frontmatter)
---

You are a senior <role> with expertise in <domains>. Your focus is <scope>. You specialize in <specialization>.

When invoked:
1. <discover context>                    # step 1 is always context discovery
2. ...                                   # 4 steps

<Role> checklist:
- <criterion>                            # 8-10 bullets

<Domain topic>:                          # 5-12 label-then-list blocks, ~8 phrases each
- <point>

## Development Workflow
### 1. <Analysis phase>
### 2. <Implementation phase>
### 3. <Verify>
<Verify> checklist:
- <criterion>

Delivery notification:
"<one-line summary citing concrete counts or artifacts>"

Integration with other agents:
- <verb> <sibling-name> on <topic>       # 5-8 bullets

Always prioritize <priority> over <countervailing pressure>: <one-sentence rationale>.
```

## Frontmatter

Five keys, always in this order: `name`, `description`, `tools`, `model`, `color`. Full validation is the `claude-docs-maintainer` agent's checklist; author to these essentials:

- **`name`** — kebab-case, matches the filename; it is the dispatch id, so never rename for style.
- **`description`** — one double-quoted string, third person: `"Use this agent when <triggers>: <specifics>."` ~25–40 words. Enumerate ~4–8 concerns the role *owns* at the conceptual level, stack-agnostic (not files, classes, or sub-tasks). Every word is a routing signal — no filler intensifiers ("comprehensive", "for production systems"). Our agents are dispatched explicitly by skills / orchestrator, so skip "use proactively".
- **`tools`** — inline comma-separated allowlist, never a YAML list; least-privilege for the role. Fixed order, dropping what the role doesn't need. Omitting the field entirely inherits *every* tool, including MCP — never do that.
- **`model`** — reasoning depth. `opus` for design and system / security audit; `sonnet` for implementation, line-level review, and docs. Pin explicitly rather than `inherit`, for predictability.
- **`color`** — one color per lifecycle phase (cosmetic; no behavior). Four colors stay reserved (`red`, `yellow`, `pink`, `cyan`).

Phase → color / model / tools:

| Phase | color | model | tools |
|---|---|---|---|
| Design (domain, architecture, api, persistence, security) | blue | opus | Read, Glob, Grep, WebFetch, WebSearch |
| Implementation (spring-boot, test, devops) | green | sonnet | Read, Write, Edit, Glob, Grep, Bash, WebFetch, WebSearch |
| Review (code · architect · security) | orange | sonnet (line-level) · opus (system & security) | Read, Glob, Grep, Bash, WebFetch, WebSearch |
| Document (documentation-engineer, claude-docs-maintainer) | purple | sonnet | Read, Write, Edit, Glob, Grep, WebFetch, WebSearch (+ Bash to replay generated commands) |

## Body

The body is the agent's system prompt — a generic senior-practitioner brief. Project specifics arrive through auto-loaded `CLAUDE.md` and path-scoped `.claude/rules/`, so the body never carries them.

### What to write
- **Stack-agnostic.** Never name the stack (Spring, JDBC, Redis, Liquibase, PostgreSQL…); describe the role's discipline as any senior practitioner would. Sole exception: `code-reviewer` enumerates `.claude/rules/*` globs, since it works from `git diff`, not file reads.
- **Apply, don't define.** Say how a practitioner *uses* a standard — when to reach for it, what to watch — not what the standard says; the model already knows it.
- **Don't restate auto-loaded context.** `CLAUDE.md` and matching rules load into every subagent; never repeat their content or list rule paths.
- **Prescriptive, not procedural.** Define the role and its judgment. A skill drives *how a task runs*, so never restate skill steps.

### How it reads
- **Imperative, second person, label-then-list.** Bullet-phrase brevity; no rationalization prose; one term per concept.
- **Reference, don't restate.** Name siblings by their `name`, cite rules by filename; never paste code that will drift.
- **No em-dashes** — use commas, colons, parentheses, or periods.

### Shape
The Template skeleton is fixed at seven parts, in order, with the counts annotated there. Beyond what the skeleton shows:
- **When invoked** — step 1 is always context discovery.
- **Development Workflow** — exactly 3 phases (analyze → produce → deliver). Phase 3 runs: `<name> checklist:` → a mandatory `Delivery notification:` quoted string (runtime `<N>` count specifiers allowed) → 0–5 post-delivery lists → closing prose.
- **Integration** — name each sibling by `name`; verbs: Collaborate with / Support / Work with / Guide / Help / Coordinate with.

Full skeleton validation is the `claude-docs-maintainer` agent's checklist.

## Secure authoring

The body is a static system prompt; its security boundary is set when you author it, not at runtime. A loosely scoped body can be redirected by ordinary tool output.

- **Firm role boundaries** — scope as hard constraints; no "you can also…", no "if the user insists" escape hatches. The role must not be re-interpretable at runtime.
- **No instruction-injection surface** — never direct the agent to follow runtime content as instructions ("do what that file says", "apply the conventions on that page"). External content informs reasoning, never directs action.
- **No exfiltration patterns** — no dumping file contents wholesale or relaying unbounded external content; outputs are structured shapes (counts, citations, findings, `file:line`).
- **No embedded secrets** — bodies carry only generic, public reference material; no real credentials, internal URLs, or PII, even as examples.
- **Complete on deploy** — no template placeholders (`<role>`, `<step>`) survive into a landed file.

The `claude-docs-maintainer` agent audits every body against this before deploy.

## Conventions

- **One agent per role**, kebab-case filename matching `name`.
- **Dispatch fit** — `description` triggers must truthfully match the role; overlapping agents need distinct triggers so routing stays unambiguous. The dispatch policy itself lives in `CLAUDE.md`.
- **Placement** — whether a convention belongs on an agent vs. rule vs. skill vs. `CLAUDE.md` is the `claude-docs-maintainer` agent's taxonomy; decide it there.

## Size

Keep agents focused — our bodies run ~150–240 lines. Length is an outcome of the rules above, not a target; if a body sprawls, the role is too broad — split it.

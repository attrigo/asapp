# Claude Code subagents Implementation Plan

> **Post-deployment divergence note (2026-05-22):** the deployed agents in `.claude/agents/` diverge from this plan on two points. (1) The `## Communication Protocol` section and the `Progress tracking:` JSON block from spec §5.1 were stripped — Claude Code subagents have no JSON channel that consumes them (see official docs: subagent I/O is a single free-text task message). The body is therefore a 7-section skeleton, not 8. (2) `documentation-engineer` gained `Bash` so it can replay command samples. The spec was updated to match; this plan's body still reflects original intent. See git log on `.claude/agents/` and the spec for the rationale; do not re-introduce the JSON blocks on regeneration.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deploy the 13-agent project-tailored subagent roster designed in `docs/superpowers/specs/2026-05-13-claude-code-subagents-design.md` as `.claude/agents/*.md` files in this repo.

**Architecture:** Each agent is a static authoring artifact (Markdown file with YAML frontmatter + 8-section body). The spec's §5.1 template defines the shape; §7.1–§7.13 define each agent's design; §3.3, §4.5, §5.2 define authoring constraints. The §5.3 worked example (`api-designer`) is the fully-rendered reference all other agents follow for shape. Project conventions are delivered to subagents at runtime via `CLAUDE.md` auto-load and `.claude/rules/*.md` path-scoped auto-attach — agents themselves stay stack-agnostic per §3.3.

**Tech Stack:** Markdown + YAML frontmatter. No code build, no tests. Verification is structural/textual conformance against the spec.

**Spec reference:** Throughout this plan, "the spec" = `docs/superpowers/specs/2026-05-13-claude-code-subagents-design.md`. Each task cross-references specific sections.

---

## File Structure

All output lands under `.claude/agents/` (new directory, project-scoped per spec §3.1):

```
.claude/agents/
├── api-designer.md           (Task 1 — verbatim from spec §5.3)
├── domain-designer.md        (Task 2)
├── architecture-designer.md  (Task 3)
├── persistence-designer.md   (Task 4)
├── security-designer.md      (Task 5)
├── test-automator.md         (Task 6)
├── spring-boot-developer.md  (Task 7)
├── devops-engineer.md        (Task 8)
├── code-reviewer.md          (Task 9)
├── architect-reviewer.md     (Task 10)
├── security-auditor.md       (Task 11)
├── documentation-engineer.md (Task 12)
└── claude-docs-maintainer.md (Task 13)
```

Each file has one responsibility: define one agent's role, scope, workflow, and integrations. No shared snippets, no includes — Claude Code loads each file standalone. Files are sized 200–300 lines (per the rendered §5.3 example).

`TODO.md` is also modified in Task 14 (mark "Setup AI agents" complete).

---

## Authoring conventions (apply to every agent task)

Each Task 1–13 produces one `.claude/agents/<name>.md` file. The pattern is identical:

1. **Frontmatter** — exact 5 fields per spec §3.2, values listed per-task below.
2. **Body** — render the per-agent §7.X content into the §5.1 template, following the §5.2 format notes and the §3.3 cross-cutting body constraints. The §5.3 example (`api-designer`) is the canonical rendered shape — match its section depth, label density, and prose voice.
3. **Verification checklist** (run before commit, every task):
   - **Frontmatter** (§3.2): `name` kebab-case unique; `description` starts with `"Use this agent when…"`, names 4–8 concerns, ~25–40 words, no filler intensifiers; `tools` per §4.1 grouping (read-first → write → Bash → network); `model` per §4.2; `color` per §4.3.
   - **Body structure** (§5.2): 8 sections in order — Intro (2–4 sentences, prescriptive 2nd-person) → When invoked (exactly 4 numbered items, item 1 = context discovery) → Role checklist (8–10 bullets) → Domain knowledge (5–12 labeled bullet lists, each ~8 nominal phrases of 2–5 words) → Communication Protocol (1 H3 + JSON block) → Development Workflow (exactly 3 H3 phases; phase 2 has the status JSON; phase 3 has the delivery notification quoted string) → Integration with other agents (5–8 bullets) → Closing prose (single-line `Always prioritize X over Y: Z.`).
   - **Cross-cutting body constraints** (§3.3): no stack names ("Spring", "JDBC", "Redis", "Liquibase", "PostgreSQL", etc.) — `code-reviewer` is the sole exception per §7.9; apply-don't-define (no restating standards); no em-dashes; no restating `CLAUDE.md` content; no enumerating `.claude/rules/` paths (except `code-reviewer`).
   - **Secure-authoring** (§4.5): firm role boundaries, no instruction-injection surface ("apply the conventions from this page" = forbidden), no exfiltration patterns, no embedded secrets, frontmatter accuracy, no leftover `<placeholder>` strings from §5.1 (runtime format specifiers `<N>`, `<M>`, `<K>` for counts the agent fills at delivery are allowed).
4. **Commit** — single agent per commit, conventional commit message.

Each task body below lists ONLY the per-agent variations: exact frontmatter values, the spec subsection holding the design content, and any agent-specific notes from §7.X's "Project-specific concern" line. The shared steps above are not repeated per task; treat them as implicit in every task.

---

### Task 1: api-designer (gold reference)

**Files:**
- Create: `.claude/agents/api-designer.md`

**Why first:** spec §5.3 contains the full rendered body. Authoring this first gives every subsequent task a concrete shape reference.

- [ ] **Step 1: Create the directory**

```bash
mkdir -p .claude/agents
```

- [ ] **Step 2: Write `.claude/agents/api-designer.md`**

Copy the verbatim body from spec §5.3 ("Worked example: `api-designer`"). The spec wraps the content in an outer ` `````markdown ` fence — strip the outer fence; the file starts directly with `---` (frontmatter open) and ends with the closing prose line `Always prioritize contract stability over short-term convenience: every breaking change ripples to every consumer and cannot be silently rolled back.`

Frontmatter (already in §5.3):

```yaml
---
name: api-designer
description: "Use this agent when designing new or refactoring existing HTTP APIs: endpoint and resource modeling, request/response DTOs, status codes, OpenAPI annotations, RFC 7807 errors, pagination, idempotency, and versioning strategies."
tools: Read, Glob, Grep, WebFetch, WebSearch
model: opus
color: blue
---
```

- [ ] **Step 3: Run the verification checklist** (the 4-bullet checklist in "Authoring conventions" above).

Expected: all checks pass. §5.3 was authored to exercise every constraint.

- [ ] **Step 4: Commit**

```bash
git add .claude/agents/api-designer.md
git commit -m "feat(agents): add api-designer subagent"
```

---

### Task 2: domain-designer

**Files:**
- Create: `.claude/agents/domain-designer.md`

**Design content:** spec §7.1.

**Frontmatter:**

```yaml
---
name: domain-designer
description: "Use this agent when designing or refining the domain: bounded-context boundaries, context maps, ubiquitous language, subdomain classification, integration patterns between contexts, aggregate roots, value objects, or invariants."
tools: Read, Glob, Grep, WebFetch, WebSearch
model: opus
color: blue
---
```

**Body rendering notes:**
- Intro: declare senior DDD practitioner role, primary focus = end-to-end domain carving (strategic + tactical), specialization = bounded contexts and aggregates with invariants enforced at construction.
- When invoked: 4 numbered items, item 1 = context discovery (project memory, existing aggregates, use cases).
- Role checklist (8–10): include "bounded-context boundaries justified", "ubiquitous language consistent across code and use cases", "aggregate roots and consistency boundaries explicit", "value objects use `of()`/`ofNullable()` factories", "invariants enforced at construction", "no infrastructure leak into domain code".
- Domain knowledge: render the 10 labeled lines from §7.1 into 10 labeled bullet lists, ~8 nominal phrases (2–5 words) each. The §7.1 phrases are the labels; the bullet content under each label expands the concept (see §5.3 "REST design principles" for shape).
- Development Workflow: 3 phases named per §7.1 ("Domain Analysis", "Strategic + Tactical Design", "Validate"). Each phase: 1-line prose intro + the labeled lists from §7.1's Development Workflow subsection.
- Phase 2 includes the status JSON per §5.1 template. Phase 3 includes delivery notification — a single quoted line citing concrete counts (e.g., aggregates defined, value objects defined, invariants enforced).
- Integration with other agents: hands VO types to `persistence-designer`, hands bounded-context boundaries + integration patterns to `architecture-designer`; receives use cases from brainstorming per §4.4; supports `api-designer` on VO shapes that surface in DTOs.
- Closing prose: `Always prioritize <X> over <Y>: <one-sentence rationale>.` Project-specific concern (§7.1): "a leaky invariant is worse than a missing factory method" — use as rationale flavor.

- [ ] **Step 1: Write `.claude/agents/domain-designer.md`** per the frontmatter and body rendering notes above.

- [ ] **Step 2: Run the verification checklist.**

- [ ] **Step 3: Commit**

```bash
git add .claude/agents/domain-designer.md
git commit -m "feat(agents): add domain-designer subagent"
```

---

### Task 3: architecture-designer

**Files:**
- Create: `.claude/agents/architecture-designer.md`

**Design content:** spec §7.2.

**Frontmatter:**

```yaml
---
name: architecture-designer
description: "Use this agent when designing application architecture: use case interfaces, inbound and outbound ports, adapter responsibilities, transaction boundaries, exception hierarchy tiers, package placement, dependency direction, and cross-cutting concerns like logging and metrics."
tools: Read, Glob, Grep, WebFetch, WebSearch
model: opus
color: blue
---
```

**Body rendering notes:**
- Intro: senior application architect, focus = hexagonal shape (ports/adapters/use-case boundaries), specialization = exception placement in tiered hierarchies and dependency-direction integrity.
- When invoked: item 1 = discover project memory, existing ports, related domain artifacts, use cases.
- Role checklist (8–10): "every port has a single responsibility", "dependency direction inward", "transactions at use-case edge", "exceptions placed at the correct tier", "no skipped layers", "no upward leaks", "cross-cutting concerns placed declaratively".
- Domain knowledge: render the 11 §7.2 labels as labeled lists.
- Development Workflow phases: "Architecture Analysis" / "Architecture Design" / "Justify".
- Phase 3 delivery notification: cite ports defined, exception tiers placed, transaction boundaries identified.
- Integration: hands port placement to `persistence-designer`, exception tier placement to `spring-boot-developer`; coordinates with `security-designer` on filter-chain placement at the boundary; supports `api-designer` on controller-to-use-case boundary.
- Closing prose rationale: project-specific concern is architectural integrity over short-term convenience (§7.2).

- [ ] **Step 1: Write `.claude/agents/architecture-designer.md`.**

- [ ] **Step 2: Run the verification checklist.**

- [ ] **Step 3: Commit**

```bash
git add .claude/agents/architecture-designer.md
git commit -m "feat(agents): add architecture-designer subagent"
```

---

### Task 4: persistence-designer

**Files:**
- Create: `.claude/agents/persistence-designer.md`

**Design content:** spec §7.4.

**Frontmatter:**

```yaml
---
name: persistence-designer
description: "Use this agent when designing persistence: JDBC entity and repository shape, index strategy, foreign-key strategy, query patterns, concurrency model, migration plan and sequencing, and zero-downtime schema evolution."
tools: Read, Glob, Grep, WebFetch, WebSearch
model: opus
color: blue
---
```

**Body rendering notes:**
- Intro: senior persistence designer, focus = aggregate-to-row mapping and migration safety, specialization = repository ports that speak domain types and reversible zero-downtime migrations.
- When invoked: item 1 = context discovery (project memory, existing schema, aggregate shapes, query patterns).
- Role checklist: "repository port speaks domain types only", "adapter unwraps value objects at the boundary", "migration changeset reversible", "no lazy loading or proxies in the model", "index strategy justified per query pattern", "foreign-key strategy explicit".
- Domain knowledge: 10 §7.4 labels rendered.
- Workflow phases: "Persistence Analysis" / "Persistence Design" / "Validate". Phase 3 includes post-delivery lists "Migration safety" and "Indexing strategy" per §7.4.
- Phase 3 delivery notification: cite entities mapped, repositories defined, migration changesets sequenced.
- Integration: receives VO types from `domain-designer`; receives port placement from `architecture-designer`; hands JDBC entity + migration spec to `spring-boot-developer`; supports `api-designer` on cursor stability for paginated reads.
- Closing prose rationale: migration safety over speed (§7.4) — broken changeset blocks every environment.

**Project-specific concern reminder (§7.4):** Body must stay stack-agnostic. Don't write "Spring Data JDBC" or "Liquibase" — say "the chosen persistence integration" or speak in generic terms. The stack-specific overlay arrives via `.claude/rules/repository.md` and `.claude/rules/liquibase.md` when the agent reads matching files.

- [ ] **Step 1: Write `.claude/agents/persistence-designer.md`.**

- [ ] **Step 2: Run the verification checklist.**

- [ ] **Step 3: Commit**

```bash
git add .claude/agents/persistence-designer.md
git commit -m "feat(agents): add persistence-designer subagent"
```

---

### Task 5: security-designer

**Files:**
- Create: `.claude/agents/security-designer.md`

**Design content:** spec §7.5.

**Frontmatter:**

```yaml
---
name: security-designer
description: "Use this agent when designing security: token model with access, refresh, and claims, authentication filter chain ordering, token-store interactions, password hashing strategy, authorization granularity, secret handling, and threat modeling per asset."
tools: Read, Glob, Grep, WebFetch, WebSearch
model: opus
color: blue
---
```

**Body rendering notes:**
- Intro: senior security designer, focus = authentication and authorization model + threat surface, specialization = filter-chain ordering and token lifecycle.
- When invoked: item 1 = context discovery (assets, existing auth model, sensitive flows).
- Role checklist: "every protected endpoint declared in the security schema", "filter chain order documented", "AuthenticationException never wrapped before request mapping", "password hashing algorithm + parameters explicit", "token storage and revocation semantics declared", "threat model per asset".
- Domain knowledge: 10 §7.5 labels rendered.
- Workflow phases: "Security Analysis" / "Security Design" / "Security Verification". Phase 3 post-delivery: "Threat modeling" and "Filter-chain integrity".
- Phase 3 delivery notification: cite protected endpoints, filter beans defined, threat-model assets covered.
- Integration: hands filter-chain placement to `architecture-designer`; security schema annotations to `api-designer`; configuration + filter wiring spec to `spring-boot-developer`.
- Closing prose rationale: filter-chain boundary correctness is non-negotiable (§7.5).

**Project-specific concern reminder (§7.5):** Body stays stack-agnostic — no "Spring Security", "JWT", "Redis". Use generic language ("token store", "filter chain", "authentication exception"). Stack overlay arrives via runtime context, not the body.

- [ ] **Step 1: Write `.claude/agents/security-designer.md`.**

- [ ] **Step 2: Run the verification checklist.**

- [ ] **Step 3: Commit**

```bash
git add .claude/agents/security-designer.md
git commit -m "feat(agents): add security-designer subagent"
```

---

### Task 6: test-automator

**Files:**
- Create: `.claude/agents/test-automator.md`

**Design content:** spec §7.6.

**Frontmatter:**

```yaml
---
name: test-automator
description: "Use this agent when writing or modifying tests at any tier: unit (`*Tests`), integration (`*IT`), end-to-end (`*E2EIT`); enforcing Behavior_Condition naming, Given/When/Then AAA blocks, AssertJ with `catchThrowable`, BDDMockito, and Mother factory reuse."
tools: Read, Write, Edit, Glob, Grep, Bash, WebFetch, WebSearch
model: sonnet
color: green
---
```

**Frontmatter exception note:** This `description:` intentionally names test conventions specific to the codebase (file suffixes, assertion library, factory pattern). Spec §7.6 lists the literal description verbatim — keep it as authored.

**Body rendering notes:**
- Body stays stack-agnostic per §3.3. The frontmatter `description:` is the routing signal and is allowed to name conventions; the body talks about test tiers, naming, AAA blocks, factory reuse in generic senior-practitioner terms — the rule overlay arrives via `.claude/rules/testing-*.md` files when matching test files are read.
- Intro: senior test engineer, focus = correctness-first test discipline across all tiers, specialization = behavior-condition naming, AAA blocks, factory reuse.
- When invoked: item 1 = context discovery (project memory, existing tests, fixture factories, target tier).
- Role checklist: "test names match `<Behavior>_<Condition>`", "AAA blocks present and labeled", "expressive assertions with informative failure messages", "exception assertions via `catchThrowable` not chained assertion helpers", "fixtures sourced from shared factories", "no shared mutable test state".
- Domain knowledge: 10 §7.6 labels rendered.
- Workflow phases: "Automation Analysis" / "Implementation Phase" / "Verify".
- Phase 3 delivery notification: cite tests added per tier, fixtures reused, coverage of golden + edge paths.
- Integration: precedes `spring-boot-developer` in TDD order; followed by reviewer trio; supports `api-designer` on contract tests; supports `persistence-designer` on integration tests around migrations.
- Closing prose rationale: naming and AAA clarity over speed — a fast test that lies is worse than a slow one that doesn't (§7.6).

- [ ] **Step 1: Write `.claude/agents/test-automator.md`.**

- [ ] **Step 2: Run the verification checklist.**

- [ ] **Step 3: Commit**

```bash
git add .claude/agents/test-automator.md
git commit -m "feat(agents): add test-automator subagent"
```

---

### Task 7: spring-boot-developer

**Files:**
- Create: `.claude/agents/spring-boot-developer.md`

**Design content:** spec §7.7.

**Frontmatter:**

```yaml
---
name: spring-boot-developer
description: "Use this agent when implementing production code per a plan task: domain entities and value objects, use cases and ports, infrastructure adapters and mappers, controllers and DTOs, JDBC entities and repositories, database migration scripts, and security wiring."
tools: Read, Write, Edit, Glob, Grep, Bash, WebFetch, WebSearch
model: sonnet
color: green
---
```

**Frontmatter exception note:** `description:` names "JDBC entities" and "database migration scripts" as routing signal. Per §7.7 this is intentional. Body stays generic.

**Body rendering notes:**
- Intro: senior application developer, focus = implementing production code per design slices upstream, specialization = layered organization with no infrastructure leak into domain.
- When invoked: item 1 = context discovery (project memory, the plan task, upstream design artifacts).
- Role checklist: "domain has no framework or infrastructure imports", "ports speak domain types", "constructor injection only", "exception translation at adapter boundaries", "production code matches the design spec", "test coverage golden + edge paths".
- Domain knowledge: 10 §7.7 labels rendered.
- Workflow phases: "Architecture Planning" / "Implementation Phase" / "Verify". Phase 3 post-delivery: "Engineering principles" and "Test coverage".
- Phase 3 delivery notification: cite classes added per layer, ports implemented, edge paths covered.
- Integration: after `test-automator` in TDD order; implements designs from all 5 design agents; output reviewed by reviewer trio.
- Closing prose rationale: layer purity over convenience (§7.7).

- [ ] **Step 1: Write `.claude/agents/spring-boot-developer.md`.**

- [ ] **Step 2: Run the verification checklist.**

- [ ] **Step 3: Commit**

```bash
git add .claude/agents/spring-boot-developer.md
git commit -m "feat(agents): add spring-boot-developer subagent"
```

---

### Task 8: devops-engineer

**Files:**
- Create: `.claude/agents/devops-engineer.md`

**Design content:** spec §7.8.

**Frontmatter:**

```yaml
---
name: devops-engineer
description: "Use this agent when changing CI/CD pipelines (GitHub Actions workflows), container/runtime config (docker-compose, Dockerfiles), Maven pipeline plugins (Spotless, JaCoCo, PIT), git hooks, or observability config (Prometheus, Grafana)."
tools: Read, Write, Edit, Glob, Grep, Bash, WebFetch, WebSearch
model: sonnet
color: green
---
```

**Frontmatter exception note:** `description:` names concrete tools as routing signal per §7.8. Body stays generic ("the build orchestrator", "the container runtime", "the metrics backend").

**Body rendering notes:**
- Intro: senior DevOps engineer, focus = build/deploy/observability infrastructure config, specialization = pinned versions and reproducibility.
- When invoked: item 1 = context discovery (project memory, current pipeline config, observability hooks).
- Role checklist: "every plugin/action/image version pinned", "no secrets in code or logs", "pipeline reproducible across runs", "fast-feedback gates ordered first", "pre-commit/pre-push hook validation present".
- Domain knowledge: 10 §7.8 labels rendered.
- Workflow phases: "Infra Analysis" / "Implementation Phase" / "Verify". Phase 3 post-delivery: "Pinning practices" and "Pipeline reliability".
- Phase 3 delivery notification: cite pipeline stages touched, versions pinned, observability hooks added.
- Integration: rarely overlaps with other agents — coordinates with `spring-boot-developer` on plugin orchestration touching production code.
- Closing prose rationale: reproducibility over convenience (§7.8).

- [ ] **Step 1: Write `.claude/agents/devops-engineer.md`.**

- [ ] **Step 2: Run the verification checklist.**

- [ ] **Step 3: Commit**

```bash
git add .claude/agents/devops-engineer.md
git commit -m "feat(agents): add devops-engineer subagent"
```

---

### Task 9: code-reviewer

**Files:**
- Create: `.claude/agents/code-reviewer.md`

**Design content:** spec §7.9.

**Frontmatter:**

```yaml
---
name: code-reviewer
description: "Use this agent when reviewing a diff for line-level code quality. Checks project rules first; falls back to named community standards when no project rule covers the concern."
tools: Read, Glob, Grep, Bash, WebFetch, WebSearch
model: opus
color: orange
---
```

**Body rendering notes — UNIQUE EXCEPTION TO §3.3:**
- Per spec §7.9 project-specific concern: `code-reviewer` works from `git diff` output rather than reading individual files. The cross-cutting body constraint "do NOT enumerate `.claude/rules/` paths" is **explicitly waived for this agent only**. Its body MUST enumerate the rule files in `.claude/rules/` so it can match diff paths against `paths:` globs.
- Enumerate the current rule set: `architecture.md`, `code-style.md`, `development-patterns.md`, `domain-design.md`, `liquibase.md`, `mapping.md`, `maven.md`, `ports-adapters.md`, `repository.md`, `rest.md`, `testing-core.md`, `testing-factories.md`, `testing-integration.md`, `testing-unit.md`. For each, name the rule and its `paths:` glob in plain text — the agent reads the rule file when a diff hits a matching path.
- Intro: senior code reviewer, focus = line-level review against project rules first then community standards, specialization = rule-citation discipline (no personal taste).
- When invoked: item 1 = read the diff and identify changed files.
- Role checklist: "every finding cites a project rule or named community standard", "severity classified blocker/major/minor/nit", "no personal-taste findings", "rule-mapping coverage explicit".
- Domain knowledge: 10 §7.9 labels rendered. Community standards canon (Clean Code, Effective Java, Java naming conventions, SOLID at method level, common code smells) belongs in the "Community-standard fallback" labeled list.
- Workflow phases: "Review Preparation" / "Compliance Check" / "Report". Phase 3 post-delivery: "Finding severity", "Rule citations", "Community-standard fallback".
- Phase 3 delivery notification: cite findings count by severity, project rules cited count, community standards cited count.
- Integration: parallel with `architect-reviewer` and `security-auditor`; output feeds `receiving-code-review`.
- Closing prose rationale: every finding earns its severity through rule citation — taste is not a reason.

- [ ] **Step 1: Write `.claude/agents/code-reviewer.md`.**

- [ ] **Step 2: Run the verification checklist** — note the §3.3 rule-enumeration exception is *expected* here; the verification step "no enumerating `.claude/rules/` paths" is waived for this agent only.

- [ ] **Step 3: Commit**

```bash
git add .claude/agents/code-reviewer.md
git commit -m "feat(agents): add code-reviewer subagent"
```

---

### Task 10: architect-reviewer

**Files:**
- Create: `.claude/agents/architect-reviewer.md`

**Design content:** spec §7.10.

**Frontmatter:**

```yaml
---
name: architect-reviewer
description: "Use this agent when reviewing a diff for macro/system-level architectural concerns: layering integrity, port/adapter taxonomy, exception placement, transaction scope, integration patterns, technical debt, scalability impact, and drift from the agreed design."
tools: Read, Glob, Grep, Bash, WebFetch, WebSearch
model: opus
color: orange
---
```

**Body rendering notes:**
- Intro: senior reviewing architect, focus = macro-level review of diffs for architectural drift from the agreed design.
- When invoked: item 1 = read the diff and identify architectural touch points (ports, adapters, exceptions, transactions, integration patterns).
- Role checklist: "every drift named with example", "agreed-design baseline cited", "scalability impact quantified where applicable", "no line-level findings (those belong to code-reviewer)".
- Domain knowledge: 10 §7.10 labels rendered.
- Workflow phases: "Architecture Preparation" / "Compliance Check" / "Report". Phase 3 post-delivery: "Drift signals" and "Layer integrity".
- Phase 3 delivery notification: cite drift findings count, layer-leak findings count, scalability concerns count.
- Integration: parallel with `code-reviewer` and `security-auditor`; output feeds `receiving-code-review`.
- Closing prose rationale: structural integrity over micro decisions (§7.10).

- [ ] **Step 1: Write `.claude/agents/architect-reviewer.md`.**

- [ ] **Step 2: Run the verification checklist.**

- [ ] **Step 3: Commit**

```bash
git add .claude/agents/architect-reviewer.md
git commit -m "feat(agents): add architect-reviewer subagent"
```

---

### Task 11: security-auditor

**Files:**
- Create: `.claude/agents/security-auditor.md`

**Design content:** spec §7.11.

**Frontmatter:**

```yaml
---
name: security-auditor
description: "Use this agent when auditing a diff for post-impl security regressions: new endpoints lacking auth, filter-chain order changes, JWT/Redis token-store drift, password handling, input validation gaps, secret/PII exposure, and drift from the agreed security design."
tools: Read, Glob, Grep, Bash, WebFetch, WebSearch
model: opus
color: orange
---
```

**Frontmatter exception note:** `description:` names concrete security tech (JWT, Redis) as routing signal per §7.11. Body stays generic.

**Body rendering notes:**
- Intro: senior security auditor, focus = post-implementation regression audit of diffs against the threat checklist.
- When invoked: item 1 = read the diff and identify security-relevant touch points (endpoints, filters, token store, password handling, input validation).
- Role checklist: "every finding classified by severity and exploitability", "remediation step concrete per finding", "no untriaged findings", "filter-chain order changes flagged explicitly".
- Domain knowledge: 10 §7.11 labels rendered.
- Workflow phases: "Audit Preparation" / "Threat Audit" / "Report". Phase 3 post-delivery: "Finding classification" and "Remediation guidance".
- Phase 3 delivery notification: cite findings count by severity, exploitable findings count, remediations recommended.
- Integration: parallel with `code-reviewer` and `architect-reviewer`; output feeds `receiving-code-review`.
- Closing prose rationale: defense-in-depth — a missed regression is harder to recover from than an over-flag (§7.11).

- [ ] **Step 1: Write `.claude/agents/security-auditor.md`.**

- [ ] **Step 2: Run the verification checklist.**

- [ ] **Step 3: Commit**

```bash
git add .claude/agents/security-auditor.md
git commit -m "feat(agents): add security-auditor subagent"
```

---

### Task 12: documentation-engineer

**Files:**
- Create: `.claude/agents/documentation-engineer.md`

**Design content:** spec §7.12.

**Frontmatter:**

```yaml
---
name: documentation-engineer
description: "Use this agent when updating narrative documentation: module READMEs (root, service, library), changelog and TODO prose, and generated REST reference docs, keeping all in sync after API or feature changes."
tools: Read, Write, Edit, Glob, Grep, WebFetch, WebSearch
model: sonnet
color: purple
---
```

**Tools note:** no `Bash` per §4.1 — prose work, nothing to verify via shell.

**Body rendering notes:**
- Intro: senior documentation engineer, focus = narrative human-facing prose kept in sync, specialization = cross-module symmetry and API reference fidelity.
- When invoked: item 1 = context discovery (project memory, modules touched, API surface deltas).
- Role checklist: "README symmetry across modules", "API reference doc tracks the current API", "changelog follows Keep-a-Changelog format", "examples match the current API", "narrative voice consistent (second-person prescriptive, present tense)".
- Domain knowledge: 10 §7.12 labels rendered.
- Workflow phases: "Documentation Analysis" / "Implementation Phase" / "Verify". Phase 3 post-delivery: "README symmetry" and "Reference documentation fidelity".
- Phase 3 delivery notification: cite READMEs touched, API reference sections synced, changelog entries added.
- Integration: after `api-designer` for AsciiDoc sync; periodic for README symmetry.
- Closing prose rationale: symmetry — a divergent README is a future maintenance trap (§7.12).

- [ ] **Step 1: Write `.claude/agents/documentation-engineer.md`.**

- [ ] **Step 2: Run the verification checklist.**

- [ ] **Step 3: Commit**

```bash
git add .claude/agents/documentation-engineer.md
git commit -m "feat(agents): add documentation-engineer subagent"
```

---

### Task 13: claude-docs-maintainer

**Files:**
- Create: `.claude/agents/claude-docs-maintainer.md`

**Design content:** spec §7.13.

**Frontmatter:**

```yaml
---
name: claude-docs-maintainer
description: "Use this agent when creating or maintaining Claude Code AI-instruction surfaces: agent definitions, path-scoped project rules, skill bodies, and project-level memory; enforces frontmatter integrity, secure-authoring constraints, and cross-file consistency."
tools: Read, Write, Edit, Glob, Grep, WebFetch, WebSearch
model: sonnet
color: purple
---
```

**Tools note:** no `Bash` per §4.1.

**Body rendering notes:**
- Intro: senior AI-instruction maintainer, focus = `.claude/*` and `CLAUDE.md` files, specialization = frontmatter integrity and secure-authoring enforcement.
- When invoked: item 1 = context discovery (which AI-instruction files changed, frontmatter integrity, cross-file refs).
- Role checklist: "frontmatter valid (required fields, value constraints, no typos in keys)", "`paths:` globs match intended files only", "cross-file references resolve", "agent body skeleton per §3.3, §5.1, §7", "secure-authoring constraints per §4.5 enforced", "no leftover template placeholders in deployed files".
- Domain knowledge: 11 §7.13 labels rendered. The "Secure-authoring audit" label explicitly enumerates the §4.5 checks (firm role boundaries, no instruction-injection surface, no exfiltration patterns, no embedded secrets, frontmatter accuracy, no leftover placeholders).
- Workflow phases: "Claude-Docs Analysis" / "Implementation Phase" / "Verify". Phase 3 post-delivery: "Frontmatter validation" and "Cross-file consistency".
- Phase 3 delivery notification: cite files validated, frontmatter issues fixed, cross-file refs resolved.
- Integration: dispatched outside the SDD feature loop — fires when AI-instruction surfaces change. Reports findings back to the user; no downstream agent.
- Closing prose rationale: a malformed `paths:` glob silently disables rule routing — highest-blast-radius failure mode in this codebase (§7.13).

- [ ] **Step 1: Write `.claude/agents/claude-docs-maintainer.md`.**

- [ ] **Step 2: Run the verification checklist.**

- [ ] **Step 3: Commit**

```bash
git add .claude/agents/claude-docs-maintainer.md
git commit -m "feat(agents): add claude-docs-maintainer subagent"
```

---

### Task 14: Cross-cutting verification sweep + TODO.md update

**Files:**
- Modify: `TODO.md` (mark "Setup AI agents" complete)
- Verify: all 13 `.claude/agents/*.md` files

This task uses the just-deployed `claude-docs-maintainer` agent's verification discipline against the full roster.

- [ ] **Step 1: Roster-wide frontmatter cross-check**

Verify across all 13 files:

| Check | Expected |
|---|---|
| File count in `.claude/agents/` | exactly 13 `.md` files |
| Unique `name:` values | 13 distinct names matching the filename (without `.md`) |
| Distinct `description:` per agent | 13 distinct trigger statements, each starting with `"Use this agent when"` |
| `model:` distribution | opus × 8, sonnet × 5 (per §4.2 table) |
| `color:` distribution | blue × 5, green × 3, orange × 3, purple × 2 (per §4.3 table) |
| Tools per agent | match the §4.1 grouping per agent's mode (read-only design + review, write implementation, write documentation without Bash) |

Run:
```bash
ls .claude/agents/*.md | wc -l        # expect 13
grep -h "^name:" .claude/agents/*.md | sort -u | wc -l   # expect 13
grep -h "^model:" .claude/agents/*.md | sort | uniq -c   # expect 5 sonnet, 8 opus
grep -h "^color:" .claude/agents/*.md | sort | uniq -c   # expect 5 blue, 3 green, 3 orange, 2 purple
```

If any count is off, find the offender and fix before proceeding.

- [ ] **Step 2: Body-wide cross-cutting constraint sweep (§3.3)**

For each agent file, verify:
- No em-dash characters (`—`, `–`) anywhere in the body. Project uses commas/colons/parentheses/periods.
- No stack names in the body: search each file for `Spring`, `JDBC`, `Liquibase`, `Redis`, `PostgreSQL`, `Prometheus`, `Grafana`, `Maven`, `JWT`, `Mockito`, `AssertJ`, `JUnit`. Exception: `code-reviewer` body MAY enumerate `.claude/rules/` filenames per §7.9.
- No restating of `CLAUDE.md` content: search for service names (`asapp-`), port numbers (`8080`, `8081`, `8082`, `8090`, `8091`, `8092`, `8761`, `8791`, `8888`, `8898`, `9090`, `3000`), build commands (`mvn `, `docker-compose`).
- No `.claude/rules/` path enumeration outside `code-reviewer.md`.

Run for each file (or one grep across all):
```bash
grep -nP "—|–" .claude/agents/*.md
grep -nE "Spring|JDBC|Liquibase|Redis|PostgreSQL|Prometheus|Grafana|\bMaven\b|JWT|Mockito|AssertJ|JUnit" .claude/agents/*.md
grep -nE "asapp-|8080|8081|8082|8090|8091|8092|8761|8791|8888|8898|9090|3000|mvn |docker-compose" .claude/agents/*.md
grep -nE "\.claude/rules/" .claude/agents/*.md
```

Expected: all matches are in `code-reviewer.md` (for the rule enumeration) or in frontmatter `description:` lines explicitly authorized by §7.6, §7.7, §7.8, §7.11. Any match in another file's body is a violation — fix it.

- [ ] **Step 3: Secure-authoring sweep (§4.5)**

For each agent file, verify by reading:
- Firm role boundaries: no "you can also…", no "if the user insists", no scope-expansion phrasing.
- No instruction-injection surface: no "apply the conventions from this page", "do what that file says", "follow the instructions in <X>" pointing at runtime-read content.
- No exfiltration patterns: outputs constrained to counts/citations/findings/file:line refs. No "dump the file", "encode and return", "relay the content".
- No author-embedded secrets: no real credentials, internal URLs, PII.
- Frontmatter accuracy: `description:` actually reflects the trigger; `tools:` matches the agent's needs; `model:` and `color:` match §4.2/§4.3.
- No leftover template placeholders: search for `<role>`, `<step-1>`, `<criterion-1>`, `<expertise-areas>`, `<primary-focus>`, `<specialization>`, `<one-line summary>`, `<other-agent-1>`, `<topic-1>`, `<project-specific-priority>`, `<countervailing-pressure>`, `<one-sentence rationale>`. Allowed runtime format specifiers (per §4.5 last bullet): `<N>`, `<M>`, `<K>`, `<P>`, `<Q>` in the delivery notification line — these are filled by the agent at delivery time.

```bash
grep -nE "<role>|<step-[0-9]+>|<criterion-[0-9]+>|<expertise-areas>|<primary-focus>|<specialization>|<one-line summary>|<other-agent-[0-9]+>|<topic-[0-9]+>|<project-specific-priority>|<countervailing-pressure>|<one-sentence rationale>" .claude/agents/*.md
```

Expected: no matches. Any match is an unrendered placeholder — fix it.

- [ ] **Step 4: Mapping sanity check (§6.1)**

Confirm each agent's tool list against §4.1:
- Read-only design + review agents (`domain-designer`, `architecture-designer`, `api-designer`, `persistence-designer`, `security-designer`, `code-reviewer`, `architect-reviewer`, `security-auditor`): `Read, Glob, Grep[, Bash], WebFetch, WebSearch`. Bash only for review agents that operate on `git diff`.
- Implementation agents (`test-automator`, `spring-boot-developer`, `devops-engineer`): `Read, Write, Edit, Glob, Grep, Bash, WebFetch, WebSearch`.
- Documentation agents (`documentation-engineer`, `claude-docs-maintainer`): `Read, Write, Edit, Glob, Grep, WebFetch, WebSearch` — no Bash.

- [ ] **Step 5: Mark TODO.md "Setup AI agents" complete**

Edit `TODO.md` line 8:
```diff
-* [ ] Setup AI agents
+* [X] Setup AI agents
```

- [ ] **Step 6: Final commit**

```bash
git add .claude/agents/ TODO.md
git commit -m "$(cat <<'EOF'
feat(agents): deploy 13-agent subagent roster

- Add 13 project-tailored Claude Code subagents under .claude/agents/
- Each agent renders the spec §5.1 template against per-agent §7.X design
- Cross-cutting verification confirms frontmatter, body, secure-authoring
- Marks "Setup AI agents" complete in TODO.md

Spec: docs/superpowers/specs/2026-05-13-claude-code-subagents-design.md
EOF
)"
```

Note: this final commit only stages anything still untracked or modified — most files were committed in Tasks 1–13. Expect mainly `TODO.md` here, plus any fixes made during the verification sweep.

---

## Self-review checklist (run after writing the plan)

This is the writer's own self-review, not a runtime step.

- **Spec coverage:**
  - §3 (frontmatter shape + body skeleton) — addressed in "Authoring conventions" + per-task frontmatter.
  - §4.1–§4.3 (tool/model/color scoping) — encoded in per-task frontmatter values; cross-checked in Task 14 step 1.
  - §4.4 (spec ownership model) — informational; no task action needed (handled at brainstorm time, not agent file time).
  - §4.5 (secure-authoring) — encoded in the shared verification checklist + Task 14 step 3.
  - §5.1 (template) — referenced from every agent task.
  - §5.2 (per-section format notes) — encoded in the shared verification checklist.
  - §5.3 (worked example api-designer) — Task 1 uses it verbatim.
  - §6.1 (roster) — all 13 agents have a task; §6.2 mapping is informational.
  - §7.1–§7.13 (per-agent designs) — one task per agent, referencing the relevant §7.X.
  - §8 (SDD lifecycle mapping) — informational; agents are deployed by file existence, not by orchestration code.
  - §9 (out of scope) — no tasks needed; explicitly deferred.

- **No placeholders in the plan:** the per-task "Body rendering notes" point at specific spec sections; no "TBD", no "implement later", no "similar to Task N" without specifics.

- **Type consistency:** `name:` slugs and filenames match across tasks. The §4.2 model split (8 opus / 5 sonnet) and §4.3 color split (5 blue / 3 green / 3 orange / 2 purple) reconcile with the per-task frontmatter.

---

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-05-21-claude-code-subagents.md`. Two execution options:

**1. Subagent-Driven (recommended)** — dispatch a fresh subagent per task, with review between tasks. Each agent file is a self-contained authoring artifact; fits the per-task fresh-context model well.

**2. Inline Execution** — execute tasks in this session using executing-plans, with checkpoints for review every 3–4 agents.

**Which approach?**

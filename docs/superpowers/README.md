# docs/superpowers

Spec-driven development artifacts produced and consumed by the [superpowers](https://github.com/obra/superpowers) skills.

## Layout

- `plans/` — Implementation plans (`YYYY-MM-DD-<slug>.md`) written before non-trivial work and executed task-by-task.
- `specs/` — Design specs (`YYYY-MM-DD-<slug>-design.md`) that some plans reference.

## Workflow

- `superpowers:brainstorming` — explore intent and requirements before designing.
- `superpowers:writing-plans` — draft a plan from a spec or requirements.
- `superpowers:executing-plans` or `superpowers:subagent-driven-development` — land the plan.
- `superpowers:verification-before-completion` — verify before claiming done.

Plans and specs are committed so future sessions can resume the work.

## Subagents

Project-tailored subagents live in `.claude/agents/` (Claude Code requires that exact path, so they sit outside this directory). They are what `superpowers:subagent-driven-development` dispatches to during plan execution.

The 13-agent roster groups by lifecycle phase:

- **Design** (blue): `domain-designer`, `architecture-designer`, `api-designer`, `persistence-designer`, `security-designer`
- **Implementation** (green): `test-automator`, `spring-boot-developer`, `devops-engineer`
- **Review** (orange): `code-reviewer`, `architect-reviewer`, `security-auditor`
- **Documentation** (purple): `documentation-engineer`, `claude-docs-maintainer`

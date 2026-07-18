---
paths:
  - ".claude/rules/**/*.md"
---

The repo standard for `.claude/rules/**/*.md` — path-scoped project rules.

## Template

Fixed shape. The H1, intro, contents list, and Further reading are optional (see Conventions); include each only when it earns its place.

```markdown
---
paths:
  - "<glob>"          # quoted; forward slashes; one concern's files
---

# <Title>             # optional — only if it adds beyond the filename

<Intro>               # optional — one line on what this rule governs

<Contents>            # optional — short section list; only when the rule tops ~100 lines

## <Section>          # declarative constraints, grouped by concern
- <constraint>

## Further reading    # optional — whole-rule background links, when in doubt
- <link>
```

## Frontmatter (`paths:`)

The `paths:` glob is routing-critical — a wrong glob silently disables the rule (the highest-blast-radius failure here). Full frontmatter / glob validation is the `claude-docs-maintainer` agent's checklist; author to these essentials:

- Quote every glob — YAML reserves `*` and `{`; unquoted patterns can silently fail.
- Forward slashes only.
- `**` matches any depth; `*` matches one segment and never crosses `/`. Anchor at a boundary to match a directory anywhere: `**/infrastructure/**`.
- Brace-expand related types: `"**/*.{ts,tsx}"`.
- Match the intended files and no more; list several globs when one concern spans distinct locations (see `rest.md`).
- Rules load when a matching file is read, not created; project-level only (user-level `paths:` rules aren't loaded).

## Style

### What to include
- **Declarative, not procedural.** A rule states *what must hold*; a skill states *how to do a task*. Keep procedure out.
- **Only what earns its place.** State the decisions and constraints the model wouldn't already follow or infer from the code; omit defaults and obvious facts, and cut any line that doesn't change behavior.
- **Don't do a linter's job.** Omit what Spotless / format checks already enforce; capture only conventions tools can't.

### How it reads
- **Terse, imperative, no rationalization prose.** Bullet-phrase brevity; label-then-list.
- **Tables for tabular data only** — input→output maps, not prose constraints.
- **Stable terminology.** One term per concept.

### No duplication
- **Say it once.** Each constraint lives in one rule — never restate it across rules.
- **Reference, don't restate.** Cite sibling rules by filename (`ports-adapters.md`); point at code with `file:line`; never paste code that will drift.

## Conventions

- **One concern per file**, named by a kebab-case filename (`rest.md`, `testing-core.md`).
- **Start a new rule only for a recurring, self-contained concern** whose files share a glob; fold a one-off into the nearest existing rule rather than creating a thin file.
- **H1 optional, only if it earns its place** — omit it when it would just restate the filename; add a single first-line heading only when it frames a scope the filename doesn't give, and never let it understate the file's scope.
- **`##` sections** group constraints by concern.
- **Links** — keep point-specific citations inline at their use. Reserve the optional `## Further reading` section at the end for whole-rule references — broader conventions or standards to fall back on where this rule is silent — not incidental background.
- **Placement** — which surface a convention belongs on (rule vs. skill vs. agent vs. `CLAUDE.md`) is the `claude-docs-maintainer` agent's taxonomy; decide it there.

## Size

- Keep rules tight and surgical.
- Keep a rule well under ~100 lines — a limit, not a goal. Nearing it, add a short contents list and check it still covers one concern (split it if not).
- Let length follow from the rules above; never pad to fill or trim to hit a number.

## Further reading

- Claude Code's [memory & rules guidance](https://code.claude.com/docs/en/memory) on how path-scoped rules load.

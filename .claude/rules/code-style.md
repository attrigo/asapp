---
paths:
  - "**/main/**/*.java"
---

Conventions for hand-written Java that tooling leaves to the author — annotation order, formatting, Javadoc, comments.

## Annotation Ordering

Order stacked annotations by semantic role: component role → configuration/routing → persistence → validation → mapping.

## Formatting

- Add a blank line after the opening `{` when a method or constructor signature wraps across multiple lines.

## Javadoc

- `@since` is mandatory on all production public classes and interfaces — the version the type was introduced in, without the `-SNAPSHOT` suffix (e.g. `@since 0.4.0`); never bump it on a later edit
- `@see` only for external references — framework/library classes and specs (e.g. RFC links)
- An `@Override` carries no Javadoc when it would only repeat the inherited contract; use `{@inheritDoc}` plus the added detail only to document behavior beyond it

## Comments

- One line, plain language — never a multi-line or prose block
- State the non-obvious why (a hidden constraint, a workaround, a subtlety); never restate what the code already says
- Doesn't apply to Javadoc — see `## Javadoc` above

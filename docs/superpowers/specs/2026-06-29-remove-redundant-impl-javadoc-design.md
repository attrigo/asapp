# Remove Redundant Implementation Javadoc

## Context

Across the three services, ~50 implementation files carry `@Override` methods whose
Javadoc duplicates the contract already documented on the port/interface they implement:

- **Use-case services** (`application/<aggregate>/in/service/`) — method Javadoc is
  character-identical to the input port (e.g. `ReadTaskService` vs `ReadTaskUseCase`).
- **Output adapters** (`infrastructure/<aggregate>/out/`) — identical to the output port
  (e.g. `TaskRepositoryAdapter` vs `TaskRepository`).
- **REST controllers** (`infrastructure/<aggregate>/in/`) — a *shorter* copy of the API
  interface Javadoc. Since commit `af887ad9` folded each `@Operation` description and
  response-code list into the `*Api` interface Javadoc, the controller copy is now a strict
  subset of the interface — and has already drifted (`createTask` `@return` reads
  "information" on the controller, "identifier" on `TaskApi`).

Java inherits a method's doc comment automatically when the overriding method has **no**
comment of its own — the generated Javadoc renders the supertype text as *"Description
copied from …"* and IDEs resolve it on hover. Re-stating the contract on the implementation
therefore adds zero information and creates a drift surface.

## Goal

Remove implementation-method Javadoc that merely repeats the contract already documented on
the implemented port/interface, and record the convention so new code does not reintroduce
it. No behavior or generated-doc changes.

## Criterion: redundant-subset

Remove an overriding method's Javadoc when the interface (or superclass) method Javadoc
**already conveys everything the implementation's Javadoc says** (interface ⊇ implementation).
This covers both the byte-identical case (services, adapters) and the subset case
(controllers, whose summary/`@param`/`@return` are fully covered by the richer interface doc).

**Keep** the implementation's Javadoc when it documents behavior *beyond* the contract
(implementation-specific notes, extra `@throws`, caching, thread-safety, etc.). In that case
the comment stays and uses `{@inheritDoc}` to inherit the contract and extend it. This is the
safety valve — only pure duplicates/subsets are stripped.

## Scope

**In scope** — overriding methods that implement a *project-owned* port/interface:
- Use-case service implementations.
- Output adapters and direct port implementations (e.g. `JwtIssuer implements TokenIssuer`).
- REST controllers implementing their `*Api` interface.

**Out of scope:**
- **Class-level and constructor Javadoc** — not overrides; they describe the implementation
  itself (e.g. *"Adapter implementation of `TaskRepository` for JDBC persistence"*) and stay.
- **Overrides of framework/library types** — `Object.equals/hashCode/toString` on domain
  entities, `OncePerRequestFilter.doFilterInternal`, `AuthenticationEntryPoint.commence`,
  etc. These do not duplicate a project contract; their comments (if any) are
  implementation-specific. Judged individually, never bulk-removed.

## Removal mechanism

Delete the redundant Javadoc block entirely; rely on automatic inheritance. Do **not**
replace it with a bare `{@inheritDoc}` comment — that adds noise for no gain (`{@inheritDoc}`
is reserved for the keep-and-extend case above). The project currently has zero
`{@inheritDoc}` usages; this keeps it that way except where a method genuinely extends the
contract.

The `@Override` annotation always remains.

## Controller drift fix

When removing controller method Javadoc, the `TaskApi.createTask` `@return` mismatch
("information" vs "identifier") resolves automatically — the controller copy disappears and
the interface text becomes the single source. No interface text is changed by this task; if
any controller Javadoc is found to contradict its interface, the interface is authoritative.

## Convention rule

Add one bullet to the `## Javadoc` section of `.claude/rules/code-style.md`:

> - Don't repeat an interface or superclass method's Javadoc on an overriding implementation —
>   omit the comment and let Javadoc inherit it. Add a comment only when the implementation
>   documents behavior beyond the contract, and then use `{@inheritDoc}` to inherit and extend.

## Verification

- `mvn clean install` — compiles; Javadoc still generates (inheritance resolves the removed
  comments). Run `-Pfull` to exercise the javadoc + style-check profile.
- `mvn spotless:apply` — formatting unaffected by comment removal, but run to be safe.
- Spot-check generated Javadoc for one service/adapter/controller method to confirm the
  *"Description copied from …"* text appears.
- No test changes expected (no production behavior changes).

## Out of scope / non-goals

- No new automated check (Checkstyle/ArchUnit) to enforce the convention — the rule in
  `code-style.md` is sufficient; a reliable "no Javadoc on `@Override`" check is brittle and
  would false-positive on the keep-and-extend case.
- No changes to interface/port Javadoc content.

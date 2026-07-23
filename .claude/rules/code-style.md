---
paths:
  - "**/main/**/*.java"
---

Conventions for hand-written Java that tooling leaves to the author — annotation order, formatting, Javadoc.

## Annotation Ordering

Group annotations strictly in this order (semantic role):
1. Component role: `@RestController`, `@Configuration`, `@Component`, `@Mapper`, `@ApplicationService`
2. Configuration/routing: `@RequestMapping`, `@GetMapping`/`@PostMapping`, `@ResponseStatus`
3. Persistence: `@Table`, `@Id`, `@Column`, `@Embedded`
4. Validation: `@NotNull`, `@NotBlank`, `@Size`, `@Valid`
5. Mapping: `@Mapping`

## Formatting

- Add a blank line after the opening `{` when a method or constructor signature — or a record header — wraps across multiple lines; a single-line signature gets none.

## Javadoc

- `@since` is mandatory on all production public classes and interfaces — use the module's current POM version without the `-SNAPSHOT` suffix (e.g., `@since 0.5.0`)
- `@see` only for external references — framework/library classes and specs (e.g. RFC links)
- Reference project types inline with `{@link}`
- Class/interface summaries lead with a role noun: "Application service…", "Adapter…", "Entity…"
- Domain types lead with behavior instead: "Represents…", "Defines…"
- Don't restate an interface/superclass method's contract on an `@Override`; document only behavior beyond it (using `{@inheritDoc}`)

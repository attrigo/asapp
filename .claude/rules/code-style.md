---
paths:
  - "**/main/**/*.java"
---

# Code Style

## Annotation Ordering

**Production classes** — strictly in this order:
1. Component role: `@RestController`, `@Entity`, `@ApplicationService`
2. Configuration/routing: `@RequestMapping`, `@Scope`, `@Profile`
3. Persistence: `@Table`, `@Id`, `@Column`
4. Serialization: `@JsonProperty`, `@JsonIgnore`
5. Validation: `@NotNull`, `@Size`, `@Valid`
6. Mapping: `@Mapping`, `@InheritInverseConfiguration`

## Javadoc

- Production public classes and interfaces: `@since` is mandatory — use the module's current POM version (e.g., `@since 0.2.0`)
- `@see` ONLY for framework/library classes (Spring, MapStruct) — **never** for internal project classes
- Summary line must start with a verb: "Stores…", "Validates…", "Handles…"

---
paths:
  - "**/main/**/*.java"
---

# Code Style

## Annotation Ordering

Group annotations strictly in this order (semantic role):
1. Component role: `@RestController`, `@Entity`, `@ApplicationService`
2. Configuration/routing: `@RequestMapping`, `@Scope`, `@Profile`
3. Persistence: `@Table`, `@Id`, `@Column`
4. Serialization: `@JsonProperty`, `@JsonIgnore`
5. Validation: `@NotNull`, `@Size`, `@Valid`
6. Mapping: `@Mapping`, `@InheritInverseConfiguration`

## Javadoc

- `@since` is mandatory on all production public classes and interfaces — use the module's current POM version (e.g., `@since 0.2.0`)
- `@see` ONLY for framework/library classes (Spring, MapStruct)
- Summary line must start with a verb: "Stores…", "Validates…", "Handles…"

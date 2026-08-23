# Keep the Domain and Application Layers Free of Framework Dependencies

**Status**: Implemented

## Context

`DependencyDirectionTests` (shipped in the sibling subtask) polices traffic between the four declared
layers of each service. It cannot see anything else, and that is structural rather than an oversight:
every clause it declares is *inbound* — `mayOnlyBeAccessedByLayers`, `mayNotBeAccessedByAnyLayer` — and
an inbound clause asks "which classes access this layer?". The candidate accessors are only the classes
ArchUnit imported, which `@AnalyzeClasses(packages = "com.attrigo.asapp.<service>")` limits to the
service itself. Spring, Jackson, jakarta, Nimbus, and the `libs/` modules are never candidates.
`ensureAllClassesAreContainedInArchitecture()` does not close the gap either: it constrains where the
service's own classes *live*, not what they depend on.

So today a domain record can gain `@Table` and `@Id` and quietly become a Spring Data entity, or an
application service can inject `RestClient` directly and bypass its output port, and the build stays
green. This installs the outbound half of the same guardrail.

The layers are clean as of this change. Verified across all three hexagonal services:

- **domain** imports only `java.time.*`, `java.util.*`, and same-service domain types. No annotations
  beyond `@Override`.
- **application** adds only `org.slf4j.{Logger,LoggerFactory}` and
  `org.springframework.transaction.annotation.Transactional`.

As with the direction guardrail, this fixes no violation — it installs a ratchet while it is still
green and cheap to add, ahead of the Gradle, OAuth, and Modulith refactors queued behind it.

## The decision

**One `DependencyIsolationTests` per hexagonal service, holding two allowlist rules — one per inner
layer.**

| File | Change |
|---|---|
| `services/asapp-tasks-service/src/test/…/tasks/architecture/DependencyIsolationTests.java` | new |
| `services/asapp-users-service/src/test/…/users/architecture/DependencyIsolationTests.java` | new |
| `services/asapp-authentication-service/src/test/…/authentication/architecture/DependencyIsolationTests.java` | new |
| `.claude/rules/architecture.md` | one line stating the allowance |
| `TODO.md` | line reworded to its delivered scope and ticked; follow-up task added |

**No build changes.** `archunit-junit5` is already managed at 1.4.1 in the root `pom.xml` and already
test-scoped in all three service poms.

**`config-service`, `discovery-service`, and the two `libs/` modules are out of scope** — none has a
layered structure, so there is no inner layer to isolate. Same reasoning as the direction guardrail.

## The rules

```java
@AnalyzeClasses(packages = "com.attrigo.asapp.tasks", importOptions = ImportOption.DoNotIncludeTests.class)
class DependencyIsolationTests {

    @ArchTest
    static final ArchRule domainDependsOnlyOnTheJdk =
            classes().that().resideInAPackage("..domain..")
                     .should().onlyDependOnClassesThat()
                     .resideInAnyPackage("..domain..", "java..");

    @ArchTest
    static final ArchRule applicationDependsOnlyOnTheJdkLoggingAndTransactions =
            classes().that().resideInAPackage("..application..")
                     .should().onlyDependOnClassesThat()
                     .resideInAnyPackage("..domain..", "..application..", "java..", "org.slf4j..",
                             "org.springframework.transaction.annotation..");

}
```

The three files differ only in the `package` declaration and the `@AnalyzeClasses` package. Layer
patterns are relative (`..domain..`), matching the style already used by both sibling fitness
functions. Rules are declared strictest first.

## Why an allowlist rather than a denylist

A denylist naming `org.springframework.web..`, `jakarta..`, `com.fasterxml..` and friends needs no
maintenance when a benign dependency appears — but it silently permits everything nobody thought to
name. That is precisely the blind spot the OAuth and Modulith refactors would walk through, since both
introduce libraries that do not exist in the tree today.

The allowlist inverts the default: an unlisted dependency fails. Its cost is that a legitimate new
application dependency means editing three files, which is friction on a decision that should be
deliberate and rare.

`java..` does not match `javax..` or `jakarta..`, so those are rejected by construction.

## `@Transactional` needs a package allowance, not a class allowance

Measured, not assumed. `@Transactional` contributes **three** bytecode dependencies —
`Transactional`, `Isolation`, and `Propagation` — because ArchUnit counts an annotation's *member*
types as dependencies even when every member is left at its default. An allowance naming only the
`Transactional` class fails with 12 violations across the authentication service, all of them
`org.springframework.transaction.annotation.*`.

Hence `"org.springframework.transaction.annotation.."`, the package.

## The application allowance is provisional

Both allowances encode conventions that already exist rather than inventing policy: `ports-adapters.md`
sanctions `@Transactional` on command use cases and logging for critical multi-step orchestrations, and
keeps `@ApplicationService` deliberately Spring-free so application services never carry `@Service`.

They are still framework types inside the layer that is supposed to be pure. The intended end state is
`domain`-and-`application` → JDK only, matching the domain rule. Getting there means moving transaction
management and logging out of the application layer, which is its own unit of work — so this change
ships the allowance and a 0.5.0 task to remove it:

```markdown
- [ ] (architecture) Decouple the application layer from every framework dependency
    - [ ] Move transaction management out of the application layer
    - [ ] Move logging out of the application layer
    - [ ] Tighten the dependency isolation rule to allow only the JDK
```

The third subtask is what closes the loop: without it the allowlist stays loose after the dependencies
are gone, and the rule silently permits their return.

That task also **conflicts with the `(persistence)` task above it** — "Wrap authentication user create
and update in a transaction" adds two more `@Transactional` usages that this task then removes. Recorded
as a `**Warning:**` on the new task so the two get sequenced deliberately rather than discovered
mid-refactor.

## Relationship to `DependencyDirectionTests`

Complementary, with a small overlap. What each one uniquely catches:

| Violation | Direction | Isolation |
|---|---|---|
| a domain record gains `@Table` + `@Id`, becoming a Spring Data entity | passes | **fails** |
| a domain record gains `@JsonProperty` or `jakarta.validation` constraints | passes | **fails** |
| an application service injects `RestClient` or `RedisTemplate`, bypassing its port | passes | **fails** |
| an application service uses `@Service` instead of `@ApplicationService` | passes | **fails** |
| domain or application imports `asapp-commons-url` / `asapp-http-clients` | passes | **fails** |
| a class escapes into a new top-level package (`tasks.shared`) | **fails** | passes |
| the bootstrap class imports a domain type | **fails** | passes |
| domain or application depends on infrastructure | **fails** | **fails** |

**This revises a claim in `2026-08-11-dependency-direction-guardrail-design.md`**, which stated the two
rules "genuinely do not overlap". They overlap on the last row: because the allowlists name only
`..domain..`, `..application..` and `java..`, they also forbid domain → infrastructure and application →
infrastructure. Neither rule subsumes the other, and the direction rule keeps sole ownership of class
placement and the bootstrap constraints. The shipped spec stays as the record of what was decided then.

## Naming

`DependencyIsolationTests`, pairing with `DependencyDirectionTests` as the two halves of dependency
governance — which way dependencies point, and which dependencies are permitted at all.

Rejected alternatives:

- **`DomainIsolationTests`** — reserved by the prior spec, before this subtask's scope widened to cover
  application. It would now name half its content.
- **`FrameworkIsolationTests`** / **`FrameworkIndependenceTests`** — understate the rule, which also
  rejects Nimbus, Apache Commons and JSpecify, none of which read as frameworks.
- **`FrameworkAndInfrastructureIsolationTests`** — tracks the TODO line word for word, but 40 characters
  and, worse, `Infrastructure` collides with the package name: a reader hitting the failure would look
  for a rule about the infrastructure layer and find one about the domain. In the TODO line,
  *infrastructure* means infrastructure **concerns** (JDBC, Redis, HTTP, JSON), not the package.

`DependencyIsolationTests` names no category, so it cannot understate, and leaves `PortBoundaryTests`
free for the parent task's remaining subtask.

## The `architecture.md` line

One line under **Package structure**, after the existing direction line:

```markdown
The domain depends only on the JDK; the application layer adds only the logging facade and `@Transactional`.
```

Same justification as the direction line: `rule-authoring.md` says to omit what a tool enforces, but a
rule file is read at authoring time while an ArchUnit failure surfaces at build time. Stating the
constraint prevents generating code that only fails later. It names no class, so nothing drifts when the
tests change — and when the follow-up task tightens the allowlist, this line is the other half of that
edit.

## Testing

The fitness function is the test. There is no test-for-the-test: proving it catches violations would mean
committing a deliberately illegal class, and what an ArchUnit rule guards against is a human writing a
bad import, not an untested code path.

Correctness was established by making each rule fail on purpose, locally and uncommitted, before the
final version was written:

| Probe | Result |
|---|---|
| domain allowlist with `java..` removed | 293 violations reported |
| application allowlist with Spring removed | 12 violations, all `org.springframework.transaction.annotation.*` |
| both rules as designed, all three services | pass |

The pass and the failures matter together. A rule observed only passing is indistinguishable from a rule
whose package pattern is typo'd and matches nothing.

## Conventions applied

- **Javadoc** — `Tests <what>` form with no `{@link}` and no `@since`, per `testing-core.md`'s exception
  for classes with no single class under test, followed by a `<p>Coverage:` list of bare `<li>` items.
  No `@author`: 148 of 151 test classes omit it, including `DependencyDirectionTests`.
- **Placement** — `<service>.architecture` in test scope, grouped by concern, per `architecture.md`.
- **Tier** — `*Tests.java`, so it runs in surefire alongside the other unit tests.
- **Formatting** — `mvn spotless:apply` from the repo root with `-pl <module>`, never from inside the
  submodule directory, so the formatter config path resolves.

## What is not done

**No production code changes.** Nothing in `src/main` moves. There is no violation to fix.

**Infrastructure gets no isolation rule.** It uses 85 distinct third-party package roots — Spring MVC,
Spring Data, Security, Cloud, Nimbus, MapStruct, Jackson, Resilience4j, jakarta, JSpecify, Apache
Commons, PostgreSQL — every one legitimate. Infrastructure is where frameworks belong; an allowlist
there would enumerate the entire stack, forbid nothing, and need re-editing on every new library.

**Technology segregation *inside* infrastructure is left for later** — controllers in `in/` not
importing Spring Data, adapters in `out/` not importing Spring MVC. Real value, but a different and
sharper concern that belongs with `PortBoundaryTests` (the parent task's remaining subtask) or the 0.10
Modulith suite.

**No shared base class or test-fixtures module.** The rules are duplicated across three files. Same
trade as the direction guardrail: deduplicating them needs a module publishing a test-jar plus wiring in
three poms, and again in the Gradle migration that follows immediately.

**`JsonNamingConventionTests` is left alone.** Different concern; 0.10 already tracks folding it into a
consolidated suite.

## Verification

- Each new test class passes: `mvn test -Dtest=DependencyIsolationTests -pl services/<service>`.
- Each rule fails when it should — confirmed by the tightened probes above, then reverted.
- `mvn spotless:apply` run from the repo root with `-pl <module>`.
- `architecture.md` states the allowance and names no class.
- `TODO.md` ticks the reworded subtask and carries the follow-up task with its sequencing warning.

## Post-implementation notes

The canonical implementation is the three services' `LayerDependencyRulesTests` classes and the
`architecture.md` allowance line, not this document.

Notable deltas:

- **The isolation rules merged into `LayerDependencyRulesTests` (revises "Naming").** A later subtask
  dropped `DependencyIsolationTests`; each allowlist is now one statement in that class.

- **Architecture-test conventions moved to their own rule (revises "The `architecture.md` line").**
  `.claude/rules/testing-architecture.md` holds fitness-function placement, ordering, and declaration.

# Enforce the Infrastructure → Application → Domain Dependency Direction

**Status**: Implemented

## Context

The dependency direction is the load-bearing constraint of this codebase's hexagonal structure, and
nothing checks it. `architecture.md` draws the package tree and `ports-adapters.md` governs what lives
in each layer, but neither states that dependencies point inward only — so the direction holds today
purely because it was written that way.

It does hold. Verified across all three hexagonal services:

- **domain** imports only JDK types (`java.time.Instant`, `java.util.UUID`, …) and same-service domain
  types.
- **application** imports only domain types, its own application types,
  `org.springframework.transaction.annotation.Transactional`, and slf4j.
- Nothing in domain or application imports infrastructure.

So this task fixes no violation. It installs a ratchet, which is what the TODO note asks for — *"a
lightweight safety net for the Gradle, OAuth, and Modulith refactors; the full JMolecules suite lands
in 0.10."* Three large refactors are queued behind it, each of which moves code between modules and
rewires configuration. A guardrail is worth most immediately *before* that, while it is still green
and cheap to add.

## The decision

**One ArchUnit fitness function per hexagonal service, checking the direction between four declared
layers, asserting that every class belongs to one of them.**

Four files change. Three are new tests; one is a rule file gaining a single line.

| File | Change |
|---|---|
| `services/asapp-tasks-service/src/test/…/tasks/architecture/DependencyDirectionTests.java` | new |
| `services/asapp-users-service/src/test/…/users/architecture/DependencyDirectionTests.java` | new |
| `services/asapp-authentication-service/src/test/…/authentication/architecture/DependencyDirectionTests.java` | new |
| `.claude/rules/architecture.md` | one line stating the direction |

**No build changes.** `archunit-junit5` is already managed at 1.4.1 in the root `pom.xml`
(`pom.xml:55`, `pom.xml:84-89`) and already declared test-scope in all three service poms — added for
`JsonNamingConventionTests`.

**`config-service` and `discovery-service` are out of scope.** Neither has a layered structure:
`config-service` holds a bootstrap class and one `config/` package, `discovery-service` a bootstrap
class plus `config/` and `security/web/`. There is no direction to enforce.

## The rule

```java
@AnalyzeClasses(packages = "com.attrigo.asapp.tasks", importOptions = ImportOption.DoNotIncludeTests.class)
class DependencyDirectionTests {

    @ArchTest
    static final ArchRule layersRespectDependencyDirection =
            layeredArchitecture().consideringOnlyDependenciesInLayers()
                                 .layer("Bootstrap").definedBy("com.attrigo.asapp.tasks")
                                 .layer("Infrastructure").definedBy("..infrastructure..")
                                 .layer("Application").definedBy("..application..")
                                 .layer("Domain").definedBy("..domain..")
                                 .whereLayer("Bootstrap").mayNotBeAccessedByAnyLayer()
                                 .whereLayer("Infrastructure").mayOnlyBeAccessedByLayers("Bootstrap")
                                 .whereLayer("Application").mayOnlyBeAccessedByLayers("Infrastructure")
                                 .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Infrastructure")
                                 .ensureAllClassesAreContainedInArchitecture();

}
```

The three files differ in exactly three places: the `package` declaration, the `@AnalyzeClasses`
package, and the `Bootstrap` layer pattern. The three layer patterns are relative (`..infrastructure..`),
matching the style already used in `JsonNamingConventionTests` (`..in.request..`).

What each clause catches:

| Violation | Caught by |
|---|---|
| domain imports application or infrastructure | `Domain.mayOnlyBeAccessedByLayers(Application, Infrastructure)` |
| application imports infrastructure | `Application.mayOnlyBeAccessedByLayers(Infrastructure)` |
| the bootstrap class imports domain or application | absence of `Bootstrap` from either allow-list |
| anything imports the bootstrap class | `Bootstrap.mayNotBeAccessedByAnyLayer()` |
| a class lands outside all four layers | `ensureAllClassesAreContainedInArchitecture()` |

The API is confirmed against ArchUnit 1.4.1: `Architectures.layeredArchitecture()` returns a
`DependencySettings` step exposing `consideringOnlyDependenciesInLayers()`, and `LayeredArchitecture`
exposes `layer`, `whereLayer`, and `ensureAllClassesAreContainedInArchitecture`. This matters because
the `DependencySettings` step is mandatory — the fluent chain does not compile without it.

## Why `layeredArchitecture()`, when the architecture is hexagonal

ArchUnit ships an `onionArchitecture()` builder that sounds like the closer fit. It is not usable
here: its layers are fixed slots — `domainModels`, `domainServices`, `applicationServices`,
`adapters`, `configuration` — and this codebase does not split domain into models and services, keeps
adapters inside `infrastructure/<aggregate>/out/` rather than in named adapter slots, and treats
`infrastructure/config/` as part of infrastructure rather than a peer layer.

`layeredArchitecture()` is how ArchUnit spells *arbitrary package-to-package direction rules*. That is
the mechanism this needs. It is deliberately not the class name — see **Naming** below.

## The Bootstrap layer

`ensureAllClassesAreContainedInArchitecture()` is what turns this from a check on three packages into
a check on the whole service, and it is the reason a fourth layer exists.

Each service has exactly one class outside the three layer packages: `Asapp<X>ServiceApplication`, in
the service root package. Declaring `Bootstrap` as `definedBy("com.attrigo.asapp.tasks")` — no `..`
suffix, so it matches only that package and not its subpackages — brings it inside the architecture
and lets the strict assertion pass.

The payoff is that a class in a *new* top-level package (`tasks.shared`, `tasks.common`) fails the
build instead of silently escaping every rule. Given that three refactors are queued behind this
guardrail, silent escape is the failure mode most worth closing.

**The coverage claim was verified, not assumed.** Across the compiled main classes of all three
services, the only classes outside `domain`, `application`, and `infrastructure` are the three
bootstrap classes. MapStruct's generated `*Impl` classes land in
`infrastructure/<aggregate>/mapper/`, inside the Infrastructure layer, so annotation processing does
not produce anything the strict assertion would reject.

**`Infrastructure.mayOnlyBeAccessedByLayers("Bootstrap")` is a forward allowance, not a description of
today.** All three bootstrap classes import nothing but `SpringApplication` and
`@SpringBootApplication`, and nothing in any service references them — component scanning is a runtime
mechanism ArchUnit cannot see, and the one explicit `@ComponentScan` lives in
`infrastructure/config/ApplicationConfiguration.java` pointing *at* application. So
`mayNotBeAccessedByAnyLayer()` would also pass. The allowance states the intended rule instead: the
composition root may wire the outermost layer, so a future `@Import(SecurityConfiguration.class)` on
the bootstrap class is legal rather than a false alarm.

This costs nothing. Domain and Application stay protected from Bootstrap without an extra clause,
because Bootstrap appears in neither of their allow-lists.

## Why `consideringOnlyDependenciesInLayers()`

It is the mandatory `DependencySettings` step, not a filter chosen to exclude anything —
`layeredArchitecture()` does not compile without one of its three methods.

For this rule specifically, it is a no-op. Every clause here is inbound
(`mayOnlyBeAccessedByLayers`, `mayNotBeAccessedByAnyLayer`), and an inbound clause's target is by
construction a class already inside the layer under test, so a third-party or JDK class is never a
candidate. Verified directly: the identical rule with `consideringAllDependencies()` passes against the
real classes, and still passes after adding a synthetic domain class with a real bytecode dependency on
slf4j (`LoggerFactory.getLogger`).

It is chosen anyway because it scopes **outbound** constraints (`mayOnlyAccessLayers`), which this rule
declares none of today but the *next* subtask does. `DomainIsolationTests` (line 19, "Keep the domain
free of framework and infrastructure dependencies") is exactly that — an outbound constraint on domain,
built from `noClasses().that().resideInAPackage("..domain..")` against `resideOutsideOfPackages(…)`, a
different construct with a different failure message. The two rules genuinely do not overlap.
`consideringOnlyDependenciesInLayers()` is what keeps it that way if this rule ever grows an
`mayOnlyAccessLayers` clause of its own: it stays scoped to the four declared layers instead of silently
starting to flag the JDK and framework imports `DomainIsolationTests` owns.

## Naming

`DependencyDirectionTests`, named after the concern from the TODO line, not after the ArchUnit builder
that implements it.

The first draft was `LayeredArchitectureTests`, which leaked the mechanism into the name and misstated
the architecture — the project is hexagonal, not layered. It also broke the precedent
`architecture.md` sets: `JsonNamingConventionTests` is named after its concern, not after the
`fields()` API underneath it.

This leaves clean sibling names for the parent task's remaining subtasks, each its own class grouped
by concern:

| TODO line | Class |
|---|---|
| Enforce the dependency direction | `DependencyDirectionTests` |
| Keep the domain free of framework and infrastructure dependencies | `DomainIsolationTests` |
| Confine cross-layer access to the declared input and output ports | `PortBoundaryTests` |

## The `architecture.md` line

`architecture.md`'s **Architecture tests** section says where fitness functions live but never states
the direction they now enforce. It gains one line:

```markdown
Dependencies point inward only: `infrastructure` → `application` → `domain`.
```

**Why this is not a linter's job.** `rule-authoring.md` says to omit what a tool already enforces, and
an ArchUnit test is a tool. The distinction is *when* each surface is read: a rule file is read at
authoring time, an ArchUnit failure surfaces at build time. Stating the constraint prevents generating
code that only fails later. It is one line, states the constraint rather than restating the
implementation, and names no class — so nothing here drifts when the tests change.

## Testing

The fitness function is the test. There is no test-for-the-test: proving it catches violations would
mean committing a deliberately illegal class, and the mutation an ArchUnit rule guards against is a
human writing a bad import, not a code path that can go untested.

Correctness is established by making it fail on purpose, locally and uncommitted — adding an
infrastructure import to a domain class, confirming the rule reports it, then reverting. A rule that
has only ever been observed passing is indistinguishable from a rule with a typo'd package pattern
that matches nothing.

## Conventions applied

- **Javadoc** — `Tests <what>` form with no `{@link}` and no `@since`, per `testing-core.md`'s
  exception for classes with no single class under test (ArchUnit named explicitly), followed by a
  `<p>Coverage:` list of bare `<li>` items.
- **Placement** — `<service>.architecture` in test scope, grouped by concern, per `architecture.md`.
- **Tier** — `*Tests.java`, so it runs in surefire alongside the other unit tests.

## What is not done

**No production code changes.** The change is three test classes and one rule line. Nothing in
`src/main` moves, and no violation is fixed — there is none to fix.

**Lines 19 and 20 of the parent task are untouched.** Domain framework-freedom and port-boundary
confinement are separate constructs, separate classes, separate commits.

**`JsonNamingConventionTests` is left alone.** It is a different concern and correctly separate; the
0.10 Modulith task already tracks folding it into a consolidated suite.

**No shared base class or test-fixtures module.** The rule is duplicated across three files instead.
Deduplicating it needs a new module publishing a test-jar plus wiring in three poms — and again in the
Gradle migration that follows immediately. That is a poor trade for ten lines that the 0.10 Modulith
suite is expected to supersede, and it would couple the three services' test builds.

## Verification

- Each new test class passes: `mvn test -Dtest=DependencyDirectionTests -pl services/<service>`.
- Each rule fails when it should — a temporary illegal import in a domain class is reported, then
  reverted.
- `mvn spotless:apply` run from the repo root with `-pl <module>`, never from inside the submodule
  directory, so the formatter config path resolves.
- `architecture.md` states the direction and names no class.

## Post-implementation notes

The canonical implementation is the three services' `LayeredArchitectureTests` and
`LayerDependencyRulesTests` classes and the `architecture.md` direction line, not this document.

Notable deltas:

- **Direction line placed under Package structure (revises "The `architecture.md` line").** The
  direction binds every Java file, not only fitness functions; `.claude/rules/architecture.md` holds it.

- **`consideringOnlyDependenciesInLayers()` rationale disproven (revises "Why
  `consideringOnlyDependenciesInLayers()`").** It filters outgoing constraints only, so it is a no-op
  for this rule's inbound clauses.

- **The three classes differ in three places, not two (revises "The rule").** The `package`
  declaration was missing from the count; the section text now includes it.

- **Spotless rewrapped the fluent chain (revises "The rule").** One call per line splits each
  layer/allow-list pair; the committed test files hold the real formatting.

- **Rules split across two classes per service (revises "The rule").** A later subtask moved the
  composite rule to `LayeredArchitectureTests` and the layer clauses to `LayerDependencyRulesTests`.

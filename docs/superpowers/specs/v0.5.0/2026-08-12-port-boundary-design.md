# Confine Cross-Layer Access to the Declared Input and Output Ports

**Status**: Implemented

## Context

The parent task's two shipped subtasks guard dependency *direction* (`DependencyDirectionTests`) and
dependency *isolation* (`DependencyIsolationTests`). Both are blind to how a layer is entered. A
controller may inject `TaskRepository` and query the database without ever touching a use case: the
dependency still points inward, and every type involved is already on the isolation allowlist. The
hexagonal structure holds only where a port is used, and nothing checks that.

This installs the third and last piece — every crossing between infrastructure and application goes
through a declared port.

Unlike its two siblings, this one is **not green on arrival**. Tracing every
infrastructure → application dependency across the three hexagonal services found the input side clean
and two breaches on the output side, both in the authentication service:

| Class | Depends on | Shape |
|---|---|---|
| `ExpiredJwtCleanupScheduler` (`infrastructure/security/scheduler/`) | `JwtAuthenticationRepository` (out port) | Computes the cutoff and calls `deleteAllByRefreshTokenExpiredBefore`, a port method no application service ever calls |
| `JwtVerifier` (`infrastructure/security/`) | `TokenStore` (out port) | The tasks and users copies of the same class take `RedisJwtStore` directly; only authentication borrows the port |

Both are infrastructure classes *consuming* an output port instead of implementing one — the application's
own outbound dependency, driven from outside the application. The scheduler case is the more telling of
the two: because its only caller is infrastructure, the port carries a method shaped entirely by an
infrastructure consumer.

Everything else already holds, and the rules below turn each of these facts into a ratchet:

- Only controllers import use case interfaces; nothing outside the application layer references a class
  in `application/**/in/service/`.
- Every output port implementation lives in infrastructure.
- Every use case is implemented only in `application/**/in/service/`.

## The decision

**Fix both breaches, then ship `PortBoundaryTests` with no exclusions** — one file per hexagonal
service, holding four rules.

| File | Change |
|---|---|
| `…/authentication/infrastructure/security/JwtVerifier.java` | takes `RedisJwtStore` instead of the `TokenStore` port |
| `…/authentication/infrastructure/security/JwtVerifierTests.java` | mock type follows |
| `…/authentication/application/authentication/out/JwtAuthenticationRepository.java` | `deleteAllByRefreshTokenExpiredBefore` removed |
| `…/authentication/infrastructure/authentication/out/JwtAuthenticationRepositoryAdapter.java` | override removed |
| `…/authentication/infrastructure/security/scheduler/ExpiredJwtCleanupScheduler.java` | takes `JdbcJwtAuthenticationRepository` |
| `services/asapp-{tasks,users,authentication}-service/src/test/…/architecture/PortBoundaryTests.java` | new ×3 |
| `.claude/rules/architecture.md` | one line stating the boundary |
| `TODO.md` | subtask ticked |

**No build changes.** `archunit-junit5` is already managed at 1.4.1 in the root `pom.xml` and test-scoped
in all three service poms.

**`config-service`, `discovery-service`, and the two `libs/` modules stay out of scope** — none has a
layered structure, so there are no ports to confine. Same reasoning as both sibling subtasks.

## Fixing the two breaches

### `JwtVerifier` takes the concrete store

The constructor parameter and field move from `TokenStore` to `RedisJwtStore`; the import swaps
`application.authentication.out.TokenStore` for `infrastructure.authentication.out.RedisJwtStore`; the two
`accessTokenExists` / `refreshTokenExists` calls change only their receiver name. `JwtVerifier` is a plain
`@Component` with constructor injection and no `@Bean` method anywhere, and `RedisJwtStore` is a
`@Component` too, so wiring needs no change.

This is alignment, not invention: the tasks and users copies of `JwtVerifier` already hold
`private final RedisJwtStore redisJwtStore`, and their test method names (`_AccessTokenNotInStore`) are
already identical to authentication's. Only the mock field's type and name change in `JwtVerifierTests`.
`WebMvcTestContext` mocks `JwtVerifier` itself rather than the store, so the controller slices are
untouched.

Infrastructure then talks to infrastructure. The port keeps its single implementation and its single
consumer — the application layer.

### Expired-authentication cleanup drops off the port

`deleteAllByRefreshTokenExpiredBefore` and its Javadoc leave `JwtAuthenticationRepository`; the override
leaves `JwtAuthenticationRepositoryAdapter`; the now-unused `java.time.Instant` import leaves both. The
scheduler injects `JdbcJwtAuthenticationRepository` and its body is unchanged.
`JdbcJwtAuthenticationRepository` keeps the query.

The alternative was a `DeleteExpiredAuthenticationsUseCase` plus service, letting the scheduler enter
through an input port like any other driving adapter, with the retention policy and its transaction
boundary in the application layer. It was rejected as the larger change for this subtask: it introduces a
new input port, a new application service, and its tests, which is its own commit-sized outcome rather
than part of installing a guardrail. Dropping the port method satisfies the boundary just as completely
and removes an operation the application never used.

What that trade costs: the cutoff rule (`Instant.now()`, "expired before") stays in infrastructure, and
persistence gains a second entry point — the adapter and the scheduler both reach
`JdbcJwtAuthenticationRepository`. Both are acceptable for a scheduled maintenance job with no domain
decision in it, and 0.8's OAuth work will revisit token lifecycle anyway.

Test fallout is nil. `ExpiredJwtCleanupSchedulerIT` already autowires `JdbcJwtAuthenticationRepository`
and asserts through it, so it keeps passing unchanged; `JwtAuthenticationRepositoryAdapterTests` never
covered the removed method, so the deletion drops untested code rather than tested behavior.
`JdbcJwtAuthenticationRepositoryIT` continues to cover the query itself.

## The rules

```java
@AnalyzeClasses(packages = "com.attrigo.asapp.tasks", importOptions = ImportOption.DoNotIncludeTests.class)
class PortBoundaryTests {

    private static final DescribedPredicate<JavaClass> APPLICATION_LAYER = resideInAPackage("..application..");

    @ArchTest
    static final ArchRule outputPortsAreUsedOnlyByTheApplicationOrTheirImplementations =
            classes().that().resideInAPackage("..application..out..")
                     .should(beUsedOnlyByTheApplicationOrTheirImplementations());

    @ArchTest
    static final ArchRule outputPortsAreImplementedOnlyInInfrastructure =
            classes().that().implement(resideInAPackage("..application..out.."))
                     .should().resideInAPackage("..infrastructure..");

    @ArchTest
    static final ArchRule inputPortsAreImplementedOnlyByApplicationServices =
            classes().that().implement(resideInAPackage("..application..in"))
                     .should().resideInAPackage("..application..in.service..");

    @ArchTest
    static final ArchRule useCaseImplementationsAreUsedOnlyByTheApplication =
            classes().that().resideInAPackage("..application..in.service..")
                     .should().onlyHaveDependentClassesThat().resideInAPackage("..application..");

    private static ArchCondition<JavaClass> beUsedOnlyByTheApplicationOrTheirImplementations() {
        return new ArchCondition<>("be used only by the application layer or their implementations") {

            @Override
            public void check(JavaClass port, ConditionEvents events) {
                port.getDirectDependenciesToSelf()
                    .stream()
                    .filter(dependency -> !APPLICATION_LAYER.test(dependency.getOriginClass())
                            && !dependency.getOriginClass().isAssignableTo(port.getName()))
                    .forEach(dependency -> events.add(
                            SimpleConditionEvent.violated(dependency, dependency.getDescription())));
            }
        };
    }

}
```

The three files differ only in the `package` declaration and the `@AnalyzeClasses` package. Layer patterns
are relative, matching both sibling fitness functions.

Note the two forms of `resideInAPackage` in play: inside `implement(…)` and `APPLICATION_LAYER` it is the
predicate statically imported from `JavaClass.Predicates`; after `should()` it is the fluent API's own
method. Both are needed.

Two rules govern who may **use** each port kind, two govern who may **implement** it:

| Rule | Catches |
|---|---|
| 1 | a controller injecting `TaskRepository` and skipping its use case; any non-implementing infrastructure class taking an output port |
| 2 | an application class implementing its own output port |
| 3 | an infrastructure class implementing a use case interface, or an application class doing it outside `in/service/` |
| 4 | a controller injecting `CreateTaskService` instead of `CreateTaskUseCase` |

Rule 1 is the load-bearing one — it is what makes the two breaches above fail — and rule 4 is its
input-side mirror. Rules 2 and 3 pin each port kind's implementation to the intended side of the boundary.

## Why rule 1 needs a custom condition

`JwtIssuer implements TokenIssuer` from `infrastructure/security/` rather than the aggregate's `out/`,
which `ports-adapters.md` explicitly sanctions for a cross-cutting concern that implements a port
directly. So the package-only formulation of rule 1 must allow all of `..infrastructure.security..`:

```java
.should().onlyHaveDependentClassesThat()
         .resideInAnyPackage("..application..", "..infrastructure..out..", "..infrastructure.security..")
```

That allowance is a hole in exactly the package `JwtVerifier` is being fixed in — the next class added to
`security/` could take an output port and the build would stay green. Rejected for that reason.

The custom condition asks the question the package pattern cannot: for each output port, is this dependent
either inside the application layer or an implementation of *this* port? An `ArchCondition` gets the port
and its dependents together, which the `onlyHaveDependentClassesThat` predicate does not — the predicate
sees only the dependent, so it cannot correlate. The condition admits an implementation wherever it lives,
and rejects every other outside consumer regardless of package.

Moving `JwtIssuer` into `authentication/out/` would make the simple formulation exact, but that decision
belongs to the open 0.5.0 task *"Reconcile driven-adapter conventions and align the code"*, whose stated
questions include whether an adapter may live outside the aggregate's `out/`. This subtask does not
pre-empt it.

## Empty selections versus zero dependents

ArchUnit fails a rule whose `that()` clause selects no classes, so each rule must match something in every
service. All four do: output ports 1 / 2 / 6, their implementations 1 / 2 / 6, application services
4 / 4 / 7 (tasks / users / authentication).

Rule 4 depends on the distinction. Nothing outside the application layer references an `in/service/` class
today, so it has zero *dependents* and passes — which is the green state, not a misconfiguration. Had its
*selection* been empty, ArchUnit would fail. The probe table below tests the difference deliberately,
because a rule with a typo'd package pattern would otherwise look exactly like a rule that passes.

## Naming

`PortBoundaryTests`, reserved for this subtask by the isolation spec. It completes the trio: direction
(which way dependencies point), isolation (which dependencies are permitted), boundary (where a crossing
may happen). Rule names read as sentences like their siblings' (`domainDependsOnlyOnTheJdk`), paired by
port kind so the two usage rules and the two implementation rules sit together.

## The `architecture.md` line

One line under **Package structure**, after the direction and isolation lines:

```markdown
Infrastructure reaches the application only through the use case interfaces; an output port is used only by the application and implemented only in infrastructure.
```

Same justification as its two predecessors: `rule-authoring.md` says to omit what a tool enforces, but a
rule file is read at authoring time while an ArchUnit failure surfaces at build time, so stating the
constraint prevents generating code that only fails later. It names no class, so nothing drifts when the
tests change.

## `repository.md` keeps its raw-argument clause

`repository.md:9` allows a port method a raw technical argument, *"e.g. a cutoff `Instant`"* — written for
the method this change deletes. Afterwards no port has one.

The clause stays. It is forward guidance for port design rather than a description of existing code, the
cutoff query still exists on `JdbcJwtAuthenticationRepository` (also matched by that rule's glob), and a
future port method taking a cutoff is plausible.

## Testing

The fitness function is the test; there is no test-for-the-test, for the same reason as both siblings —
proving it catches violations would mean committing a deliberately illegal class.

Correctness is established by probing each rule to failure, locally and uncommitted, before the final
version is written. A rule observed only passing is indistinguishable from one whose package pattern
matches nothing.

| Probe | Expected |
|---|---|
| restore `JwtVerifier`'s `TokenStore` dependency | rule 1 reports it |
| narrow rule 2's target to `..infrastructure..out..` | `JwtIssuer` flagged, proving the condition admits implementations outside `out/` |
| point rule 3 at `..application..in` | every application service flagged |
| import a `*Service` into a controller | rule 4 reports it — required, since zero dependents passes trivially |
| point any `that()` at a nonexistent package | ArchUnit fails rather than silently passing |

## Commits

Three, with the guardrail last so it lands green:

1. `refactor(security)` — `JwtVerifier` takes `RedisJwtStore`; its test follows
2. `refactor(architecture)` — the cutoff method leaves the port and adapter; the scheduler takes the JDBC repository
3. `test(architecture)` — `PortBoundaryTests` ×3, the `architecture.md` line, the TODO tick

## Conventions applied

- **Javadoc** — `Tests <what>` with no `{@link}` and no `@since`, per `testing-core.md`'s exception for
  classes with no single class under test, followed by a `<p>Coverage:` list of bare `<li>` items
  describing behaviors. No `@author`, matching both siblings.
- **Placement** — `<service>.architecture` in test scope, grouped by concern, per `architecture.md`.
- **Tier** — `*Tests.java`, so it runs in surefire with the other unit tests.
- **Formatting** — `mvn spotless:apply` from the repo root with `-pl <module>`, never from inside the
  submodule directory, so the formatter config path resolves.

## What is not done

**Infrastructure → domain stays unconstrained.** Mappers exist to translate domain types, so any rule
there would fight the design. The direction and isolation rules already bound what that access can be.

**No rule against an output adapter depending on an input port.** It would catch a genuine re-entrancy
smell — an adapter calling back into a use case — but nothing in the tree is near it, so it would guard a
hypothetical.

**`PasswordService` gets no rule.** It is a domain-declared port, implemented by
`infrastructure/user/out/PasswordServiceAdapter` and used only by application services — clean today, and
the only interface in any domain package. A rule for it would exist in one of the three files and select
nothing in the other two, which ArchUnit fails. The driven-adapter reconciliation task owns this shape.

**No blanket "infrastructure may only touch ports" rule.** Infrastructure legitimately depends on
application *exceptions* — `GlobalExceptionHandler` maps them to responses, and `TasksGatewayAdapter`
throws `TasksUnavailableException` per `ports-adapters.md`. The four rules are therefore per-target
(`out/`, `in`, `in/service/`) rather than one allowlist over the whole application package.

**No shared base class or test-fixtures module.** The rules are duplicated across three files, as with
both siblings: deduplicating needs a module publishing a test-jar plus wiring in three poms, and again in
the Gradle migration that follows immediately.

**`JsonNamingConventionTests` is left alone.** Different concern; 0.10 already tracks folding it into a
consolidated suite.

## Verification

- Each new test class passes: `mvn test -Dtest=PortBoundaryTests -pl services/<service>`.
- Each rule fails when it should — confirmed by the probes above, then reverted.
- The authentication service's unit tests pass after both fixes.
- `ExpiredJwtCleanupSchedulerIT` and `JdbcJwtAuthenticationRepositoryIT` — the two Testcontainers ITs
  covering the cleanup path — are run by the developer, not claimed here.
- `mvn spotless:apply` run from the repo root with `-pl <module>`.
- `architecture.md` states the boundary and names no class.
- `TODO.md` ticks this subtask and, with it, the parent task — every subtask of the guardrail is then done.

## Post-implementation notes

The canonical implementation is the three services' `PortBoundaryRulesTests` classes and the
`architecture.md` enforced-boundaries section, not this document.

Notable deltas:

- **A fifth rule confines input-port references (revises "What is not done").** Output adapters and
  cross-cutting components could re-enter through an input port;
  `inputPortsAreReferencedOnlyByTheApplicationLayerOrDrivingAdapters` now closes that path.

- **Boundary line rewritten under a new section (revises "The `architecture.md` line").** The specified
  wording contradicted `ports-adapters.md` and live mapper code; `.claude/rules/architecture.md`'s
  `## Enforced boundaries` holds the narrowed sentence.

- **Output-port glob narrowed to the exact package (revises "The rules").** `..application..out..`
  recursed where the input-port sibling did not; both output rules now match `..application..out`.

- **Rules renamed from "used" to "referenced" (revises "Naming").** ArchUnit checks static references,
  not runtime use; the shipped `@ArchTest` names and condition factory say so.

- **Rules grouped by port kind (revises "Naming").** The fifth rule broke the planned
  usage/implementation pairing; declaration order and the class Javadoc list input rules first.

- **Condition filter extracted to a named method (revises "Why rule 1 needs a custom condition").** The
  inline double negation read poorly; `isNotInApplicationLayerNorImplementationOf` now carries it, with
  identical semantics.

- **The fifth rule and narrowed glob went unprobed (revises "Testing").** Both landed after the recorded
  probe run; the surefire reports cover the four-rule, recursive-glob shape only.

- **Nine commits, not three (revises "Commits").** The design commit went uncounted and five review and
  polish commits followed; the branch history holds them.

- **The parent task's note left `TODO.md` (revises "The decision").** It scoped an in-flight task; with
  the parent ticked complete it had nothing left to scope.

- **Class renamed to `PortBoundaryRulesTests` (revises "Naming").** A later subtask suffixed every
  single-statement rule class with `Rules`; the composite rule alone keeps the plain name.

- **The declarative-boundary follow-up was dropped, not deferred (revises "What is not done").** The
  custom condition stays; the concern folded into the driven-adapter reconciliation task.

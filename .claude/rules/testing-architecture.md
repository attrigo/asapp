---
paths:
  - "**/architecture/*.java"
---

ArchUnit fitness functions: how a rule is placed, ordered, named, and declared.

- A new rule joins the class for its concern; a new concern gets its own class. The composite whole-architecture rule stays alone.
- In `LayerDependencyRulesTests`, declare what a layer may depend on before who may depend on it, each block ordered innermost layer first.
- Name each `@ArchTest` field in camelCase as the sentence the rule asserts, subject first (`domainDependsOnlyOnTheJdk`).
- Prefer the fluent API; reach for a custom `ArchCondition` only when the assertion's two sides are invisible to a predicate — a dependent and the specific port it implements, a field and its annotation value.
- A rule whose `that()` clause selects nothing fails; every rule must match a class in every service.

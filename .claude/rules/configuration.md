---
paths:
  - "**/src/**/application*.properties"
  - "central-config/*.properties"
---

## Record decisions, not defaults

- Set a property only when it overrides a default, or when the default is worth pinning anyway — safety-relevant, drift-prone, or central enough to show in place; trim anything else.

## Secrets

- Base and test files commit plaintext secrets; only the docker profile externalizes them to `${ENV_VAR}` placeholders.
- Values are never encrypted — `{cipher}` is not used.

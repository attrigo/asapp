---
paths:
  - "**/src/**/application*.properties"
  - "central-config/application*.properties"
---

## Record decisions, not defaults

- Set a property only when it overrides a default, or when a default value is worth pinning anyway — it's safety-relevant, drift-prone, or central enough to show in place.
- Trim anything that merely restates a stable library default.

## Secrets

- Local and test files commit plaintext secrets; only the docker profile externalizes them to `${ENV_VAR}` placeholders.
- Values are never encrypted — `{cipher}` is not used.

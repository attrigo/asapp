# Commit Message Examples (continued)

Additional worked examples for the `asapp-draft-commit-msg` skill. See
[SKILL.md](SKILL.md) for the generation process and the two representative examples inline there; this file holds the rest.

**Example 1: Cross-module (refactoring)**

Detected changes:
- Modified error handling in all three services
- Updated GlobalExceptionHandler in each

Generated commit message:
```
refactor(api): standardize error handling across services
```

Reasoning: Structural improvement affecting multiple services, API layer scope used for cross-cutting concern.

---

**Example 2: Bullets-only body (multiple sub-actions, why is obvious from subject)**

Detected changes:
- Renamed credential env vars across all services
- Simplified credential values

Generated commit message:
```
refactor(docker): clean up service credential env vars and values

- Rename MANAGEMENT_USERNAME/PASSWORD → SERVICE_USERNAME/PASSWORD to better reflect credential scope
- Rename CONFIG_URI/USERNAME/PASSWORD → CONFIG_SERVER_URI/USERNAME/PASSWORD for consistency
- Simplify all credential values to user/secret for easier local testing
```

Reasoning: Multiple distinct sub-actions warrant a body; the subject alone conveys the why ("clean up"), so no lead paragraph is needed.

---

**Example 3: Lead paragraph + bullets (non-obvious why)**

Detected changes:
- Removed AuthenticationManager bean from tasks and users services
- Kept the bean in authentication service

Generated commit message:
```
fix(security): drop AuthenticationManager bean from tasks and users

Exposing @Bean AuthenticationManager in a proxyBeanMethods=false class caused a StackOverflowError in the actuator filter chain.

- Drop the AuthenticationManager bean from tasks and users services
- Keep the bean in authentication service where CredentialsAuthenticatorAdapter requires it
```

Reasoning: The why (StackOverflowError root cause) is non-obvious from the subject and warrants a lead paragraph; bullets list the surgical changes.

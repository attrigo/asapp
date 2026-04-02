---
paths:
  - "**/infrastructure/**/*.java"
---

# Development Patterns

## Extracting the Current User

- Always cast to `JwtAuthenticationToken` when reading the current user — never use `UserDetails` or `UsernamePasswordAuthenticationToken`
- `JwtAuthenticationFilter` has already validated the JWT via Redis and stored it as a `JwtAuthenticationToken` in the security context

## Service-to-Service Calls

- Use the injected `RestClient` bean directly — never construct a new `RestClient` or add an `Authorization` header manually
- `JwtInterceptor` automatically propagates the current request's JWT to all outgoing service calls

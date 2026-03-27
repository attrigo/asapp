---
paths:
  - "**/application/**/*.java"
  - "**/infrastructure/**/*.java"
---

# Development Patterns

## Common Snippets

**Extract current user from JWT** (token already Redis-validated by `JwtAuthenticationFilter`):
```java
JwtAuthenticationToken auth = (JwtAuthenticationToken) SecurityContextHolder
    .getContext().getAuthentication();
String username = auth.getName();
String role     = auth.getRole();
```

**Service-to-service calls** — JWT is auto-propagated via `JwtInterceptor`, no manual header needed:
```java
var response = restClient.get()
                         .uri("/api/users/{id}", userId)
                         .retrieve()
                         .body(UserResponse.class);
```

For full use-case, endpoint, and migration workflows, use the corresponding skills.

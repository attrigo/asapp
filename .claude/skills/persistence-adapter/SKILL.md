---
name: persistence-adapter
description: >-
  Implement output port adapters in the infrastructure layer: repository adapters (JDBC),
  gateway adapters (cross-service REST), store adapters (Redis), direct port implementations,
  JDBC entities, Spring Data repositories, and MapStruct mappers (domain/entity).
  Use when creating adapter, repository adapter, gateway adapter, store adapter,
  JDBC entity, Spring Data repository, persistence mapper, output port implementation.
  Not for controllers, use cases, domain modeling, or input ports.
---

# Persistence Adapter

## Quick Reference

| Artifact              | Location                                           | Naming                                   |
|-----------------------|----------------------------------------------------|------------------------------------------|
| Output port           | `application/<agg>/out/`                           | `<Domain>Repository`, `<Domain>Gateway`, `<Domain>Store` |
| Repository adapter    | `infrastructure/<agg>/out/`                        | `<Domain>RepositoryAdapter`              |
| Gateway adapter       | `infrastructure/<agg>/out/`                        | `<Domain>GatewayAdapter`                 |
| Store adapter         | `infrastructure/<agg>/out/`                        | `Redis<Domain>Store` (descriptive, no `Adapter` suffix) |
| Direct implementation | `infrastructure/security/` or `infrastructure/<agg>/out/` | Descriptive name (e.g., `JwtIssuer`)  |
| JDBC entity           | `infrastructure/<agg>/persistence/`                | `Jdbc<Domain>Entity`                     |
| Spring Data repo      | `infrastructure/<agg>/persistence/`                | `Jdbc<Domain>Repository`                 |
| MapStruct mapper      | `infrastructure/<agg>/mapper/`                     | `<Domain>Mapper`                         |
| ObjectFactory         | `infrastructure/<agg>/mapper/`                     | `<Domain>ObjectFactory`                  |
| Value object mapper   | `infrastructure/<agg>/mapper/`                     | `<Field>Mapper` (e.g., `TaskIdMapper`)   |

## Core Workflow

### 1. Define the Output Port

Plain Java interface in `application/<agg>/out/`. No Spring annotations. Domain value objects only, never raw `UUID`.

```java
public interface TaskRepository {
    Optional<Task> findById(TaskId taskId);
    Collection<Task> findByUserId(UserId userId);
    Collection<Task> findAll();
    Task save(Task task);
    Boolean deleteById(TaskId taskId);
}
```

Return types: `Optional<Domain>` single finds, `Collection<Domain>`/`List<Domain>` multi-finds, `Boolean` existence check, `void` unconditional deletes, `Integer` bulk deletes.
### 2. Create the JDBC Entity

Record in `infrastructure/<agg>/persistence/`. Use `@Table`, `@Id`, `@Column`, validation annotations. Raw types only.

```java
@Table("tasks")
public record JdbcTaskEntity(
        @Id UUID id,
        @Column("user_id") @NotNull UUID userId,
        @NotBlank String title,
        String description,
        Instant startDate,
        Instant endDate
) {}
```

For embedded components use `@Embedded.Nullable(prefix = "column_prefix_")`:

```java
@Table("jwt_authentications")
public record JdbcJwtAuthenticationEntity(
        @Id UUID id,
        @NotNull UUID userId,
        @Embedded.Nullable(prefix = "access_token_") JdbcJwtEntity accessToken,
        @Embedded.Nullable(prefix = "refresh_token_") JdbcJwtEntity refreshToken
) {}
```

### 3. Create the Spring Data Repository

Extend `ListCrudRepository<JdbcEntity, UUID>`. Raw `UUID` parameters. Custom deletes use `@Modifying` + `@Query` returning `Long`.

```java
@Repository
public interface JdbcTaskRepository extends ListCrudRepository<JdbcTaskEntity, UUID> {
    Collection<JdbcTaskEntity> findByUserId(UUID userId);

    @Modifying
    @Query("DELETE FROM tasks u WHERE u.id = :id")
    Long deleteTaskById(UUID id);
}
```

### 4. Create Value Object Mappers and ObjectFactory

Add `@Mapper(componentModel = "spring")` on the interface. Use `@Mapping` to map primitive↔VO fields. Add an `@ObjectFactory` method returning `new Entity(...)` when the constructor doesn't match field-by-field. See variant-adapters reference for complex aggregate examples.

### 5. Create the Repository Adapter

`@Component` in `infrastructure/<agg>/out/`. Inject `JdbcRepository` + `Mapper`. Unwrap VOs via `value()`, map results back to domain.

```java
@Component
public class TaskRepositoryAdapter implements TaskRepository {
    private final JdbcTaskRepository taskRepository;
    private final TaskMapper taskMapper;

    public TaskRepositoryAdapter(JdbcTaskRepository taskRepository, TaskMapper taskMapper) {
        this.taskRepository = taskRepository;
        this.taskMapper = taskMapper;
    }

    @Override
    public Optional<Task> findById(TaskId taskId) {
        return taskRepository.findById(taskId.value())
                             .map(taskMapper::toTask);
    }

    @Override
    public Task save(Task task) {
        var taskToSave = taskMapper.toJdbcTaskEntity(task);
        var taskSaved = taskRepository.save(taskToSave);
        return taskMapper.toTask(taskSaved);
    }

    @Override
    public Boolean deleteById(TaskId taskId) {
        return taskRepository.deleteTaskById(taskId.value()) > 0;
    }
}
```

### 6. Variant: Gateway Adapter

Port as `<Service>Gateway` in `application/<agg>/out/`. Adapter delegates to a `*Client` from `libs/asapp-rest-clients`. REST client config gated by `@ConditionalOnProperty(name = "asapp.client.<service>.base-url")`. Clients catch `RestClientException` and return empty collections on failure.

```java
@Component
public class TasksGatewayAdapter implements TasksGateway {
    private final TasksClient tasksClient;
    public TasksGatewayAdapter(TasksClient tasksClient) { this.tasksClient = tasksClient; }

    @Override
    public List<UUID> getTaskIdsByUserId(UserId userId) {
        return tasksClient.getTaskIdsByUserId(userId.value());
    }
}
```

### 7. Variant: Store Adapter (Redis)

Port uses framework-agnostic name (`JwtStore`). Adapter uses descriptive name without `Adapter` suffix (`RedisJwtStore`). Use `executePipelined` for atomic multi-key ops. Wrap exceptions in `*StoreException`.

```java
@Component
public class RedisJwtStore implements JwtStore {
    public static final String ACCESS_TOKEN_PREFIX = "jwt:access_token:";
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void save(JwtPair jwtPair) {
        try {
            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                // SETEX with TTL per token
                return null;
            });
        } catch (Exception e) {
            throw new TokenStoreException("Could not store tokens in fast-access store", e);
        }
    }
}
```

### 8. Variant: Direct Port Implementation

When no translation is needed, implement the port directly. No `Adapter` suffix. Cross-cutting `infrastructure/security/` can implement ports from any aggregate. Use a delegation adapter when the adaptee returns a richer type than the port requires.

```java
// Direct implementation -- no adapter needed
@Component
public class JwtIssuer implements TokenIssuer { /* ... */ }

// Delegation adapter -- discards richer return type
@Component
public class TokenVerifierAdapter implements TokenVerifier {
    private final JwtVerifier jwtVerifier;
    @Override
    public void verifyAccessToken(EncodedToken encodedToken) {
        jwtVerifier.verifyAccessToken(encodedToken); // discards DecodedJwt return
    }
}
```

See [references/variant-adapters.md](references/variant-adapters.md) for full gateway, Redis store, and direct port implementation examples.

## Common Pitfalls

- **Leaking domain types into JDBC layer**: JDBC entities use raw `UUID`, `String`, `Instant` -- never domain value objects
- **Using `CrudRepository` or `JpaRepository`**: Always extend `ListCrudRepository`
- **Spring annotations on output ports**: Port interfaces are plain Java -- no `@Repository`, `@Component`
- **Missing `@Modifying` on custom deletes**: All `@Query` delete/update methods need `@Modifying`
- **Naming store adapters with `Adapter` suffix**: Adapters with embedded logic use descriptive names like `RedisJwtStore`
- **Forgetting `@Mapping(target, ignore = true)` on entity-to-domain**: MapStruct requires explicit ignore for every domain field when using `@ObjectFactory`
- **Constructor injection style**: Use constructor injection with final fields, no `@Autowired`
- **Exception wrapping in adapters**: Only wrap infrastructure exceptions (`DataAccessException`, Redis failures) into application-layer exceptions -- never wrap Spring Security exceptions

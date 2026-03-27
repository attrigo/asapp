# Output Port Reference

## Port Categories

### Repository Port (persistence)

Persists and retrieves domain aggregates. Uses domain value objects for all parameters.

```java
package com.bcn.asapp.tasks.application.task.out;

public interface TaskRepository {

    Optional<Task> findById(TaskId taskId);

    Collection<Task> findByUserId(UserId userId);

    Collection<Task> findAll();

    Task save(Task task);

    Boolean deleteById(TaskId taskId);

}
```

Return type conventions:
- `Optional<Domain>` for single finds
- `Collection<Domain>` or `List<Domain>` for multi-finds
- `Boolean` for deletes when caller needs existence confirmation
- `void` for unconditional deletes
- `Integer` for bulk deletes

### Repository Port (multi-method example)

```java
package com.bcn.asapp.authentication.application.authentication.out;

public interface JwtAuthenticationRepository {

    JwtAuthentication findByAccessToken(EncodedToken accessToken);

    JwtAuthentication findByRefreshToken(EncodedToken refreshToken);

    List<JwtAuthentication> findAllByUserId(UserId userId);

    JwtAuthentication save(JwtAuthentication jwtAuthentication);

    void deleteById(JwtAuthenticationId jwtAuthenticationId);

    void deleteAllByUserId(UserId userId);

    Integer deleteAllByRefreshTokenExpiredBefore(Instant expiredBefore);

}
```

### Gateway Port (cross-service communication)

Retrieves data from another bounded context. Returns raw types (UUIDs) to maintain loose coupling.

```java
package com.bcn.asapp.users.application.user.out;

public interface TasksGateway {

    List<UUID> getTaskIdsByUserId(UserId userId);

}
```

### Store Port (fast-access cache)

Handles temporary storage for fast lookup and validation (e.g., Redis).

```java
package com.bcn.asapp.authentication.application.authentication.out;

public interface JwtStore {

    Boolean accessTokenExists(EncodedToken accessToken);

    Boolean refreshTokenExists(EncodedToken refreshToken);

    void save(JwtPair jwtPair);

    void delete(JwtPair jwtPair);

}
```

### Capability Ports (infrastructure services)

Expose infrastructure capabilities using domain-centric naming.

```java
// Token generation capability
package com.bcn.asapp.authentication.application.authentication.out;

public interface TokenIssuer {

    JwtPair issueTokenPair(UserAuthentication userAuthentication);

    JwtPair issueTokenPair(Subject subject, Role role);

}
```

## Compensating Transactions in Services

When an application service coordinates two non-transactional stores (e.g., PostgreSQL + Redis),
use a compensating transaction pattern. Always implement in the service, never in adapters.

```java
@ApplicationService
public class AuthenticateService implements AuthenticateUseCase {

    private final JwtAuthenticationRepository jwtAuthenticationRepository;
    private final JwtStore jwtStore;

    @Override
    @Transactional
    public JwtAuthentication authenticate(AuthenticateCommand authenticateCommand) {
        // ... credential validation and token generation ...

        var savedAuthentication = persistAuthentication(jwtAuthentication);
        try {
            activateTokens(savedAuthentication.getJwtPair());
            return savedAuthentication;
        } catch (TokenStoreException e) {
            compensateRepositoryPersistence(savedAuthentication);
            throw e;
        }
    }

    private void compensateRepositoryPersistence(JwtAuthentication jwtAuthentication) {
        try {
            jwtAuthenticationRepository.deleteById(jwtAuthentication.getId());
        } catch (Exception e) {
            throw new CompensatingTransactionException(
                    "Failed to compensate repository persistence after token activation failure", e);
        }
    }

}
```

Pattern: save to primary store first, then secondary. If secondary fails, roll back primary via
compensation. Wrap compensation failures in `CompensatingTransactionException`.
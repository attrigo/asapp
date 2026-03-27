# Variant Adapter Patterns

## Gateway Adapter (Cross-Bounded-Context REST)

Port in `application/<agg>/out/` as `<Service>Gateway`. Adapter in `infrastructure/<agg>/out/` delegates to a `*Client` from `asapp-rest-clients`.

```java
// application/user/out/TasksGateway.java
public interface TasksGateway {
    List<UUID> getTaskIdsByUserId(UserId userId);
}

// infrastructure/user/out/TasksGatewayAdapter.java
@Component
public class TasksGatewayAdapter implements TasksGateway {
    private final TasksClient tasksClient;

    public TasksGatewayAdapter(TasksClient tasksClient) {
        this.tasksClient = tasksClient;
    }

    @Override
    public List<UUID> getTaskIdsByUserId(UserId userId) {
        return tasksClient.getTaskIdsByUserId(userId.value());
    }
}
```

### REST Client in `libs/asapp-rest-clients`

Each external service gets a client interface, an implementation, and a configuration class.

**Client interface** -- plain Java, raw types (no domain VOs):

```java
public interface TasksClient {
    List<UUID> getTaskIdsByUserId(UUID userId);
}
```

**REST implementation** -- uses `RestClient`, catches `RestClientException`, returns empty collection on failure (graceful degradation):

```java
public class TasksRestClient implements TasksClient {
    private final UriHandler tasksServiceUriHandler;
    private final RestClient taskClient;

    public TasksRestClient(UriHandler tasksServiceUriHandler, RestClient taskClient) {
        this.tasksServiceUriHandler = tasksServiceUriHandler;
        this.taskClient = taskClient;
    }

    @Override
    public List<UUID> getTaskIdsByUserId(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
        try {
            var uri = this.tasksServiceUriHandler.newInstance()
                                                 .path(TASKS_GET_BY_USER_ID_FULL_PATH)
                                                 .build(userId);
            List<TasksByUserIdResponse> tasks = this.taskClient.get()
                                                               .uri(uri)
                                                               .retrieve()
                                                               .body(new ParameterizedTypeReference<>() {});
            if (tasks == null) {
                return Collections.emptyList();
            }
            return tasks.stream().map(TasksByUserIdResponse::taskId).toList();
        } catch (RestClientException e) {
            logger.warn("Failed to retrieve tasks for user {}: {}. Returning empty list.", userId, e.getMessage());
            return Collections.emptyList();
        }
    }
}
```

**Configuration** -- gated by `@ConditionalOnProperty` so consuming services opt in via `application.yml`:

```java
@Configuration
@ConditionalOnProperty(name = "asapp.client.tasks.base-url")
public class TasksClientConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "tasksServiceUriHandler")
    UriHandler tasksServiceUriHandler(@Value("${asapp.client.tasks.base-url}") String baseUrl) {
        return new DefaultUriHandler(baseUrl);
    }

    @Bean
    TasksClient taskClient(UriHandler tasksServiceUriHandler, RestClient.Builder restClientBuilder) {
        return new TasksRestClient(tasksServiceUriHandler, restClientBuilder.build());
    }
}
```

Key points:
- `RestClient.Builder` is pre-configured by the consuming service (e.g., with `JwtInterceptor`)
- URL constants come from `asapp-commons-url` (e.g., `TASKS_GET_BY_USER_ID_FULL_PATH`)
- Property: `asapp.client.<service>.base-url`

## Store Adapter (Redis)

Port uses framework-agnostic name (e.g., `JwtStore`). Adapter uses descriptive name without `Adapter` suffix when it contains logic (e.g., `RedisJwtStore`).

```java
@Component
public class RedisJwtStore implements JwtStore {
    public static final String ACCESS_TOKEN_PREFIX = "jwt:access_token:";
    public static final String REFRESH_TOKEN_PREFIX = "jwt:refresh_token:";

    private final RedisTemplate<String, String> redisTemplate;

    public RedisJwtStore(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Boolean accessTokenExists(EncodedToken accessToken) {
        var key = ACCESS_TOKEN_PREFIX + accessToken.value();
        return redisTemplate.hasKey(key);
    }

    @Override
    public void save(JwtPair jwtPair) {
        try {
            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                RedisStringCommands cmds = connection.stringCommands();

                var accessKey = ACCESS_TOKEN_PREFIX + jwtPair.accessToken().encodedTokenValue();
                var accessTtl = calculateTtl(jwtPair.accessToken().expiration());
                cmds.setEx(accessKey.getBytes(), accessTtl, "".getBytes());

                var refreshKey = REFRESH_TOKEN_PREFIX + jwtPair.refreshToken().encodedTokenValue();
                var refreshTtl = calculateTtl(jwtPair.refreshToken().expiration());
                cmds.setEx(refreshKey.getBytes(), refreshTtl, "".getBytes());

                return null;
            });
        } catch (Exception e) {
            throw new TokenStoreException("Could not store tokens in fast-access store", e);
        }
    }

    @Override
    public void delete(JwtPair jwtPair) {
        try {
            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                RedisKeyCommands cmds = connection.keyCommands();
                cmds.del((ACCESS_TOKEN_PREFIX + jwtPair.accessToken().encodedTokenValue()).getBytes());
                cmds.del((REFRESH_TOKEN_PREFIX + jwtPair.refreshToken().encodedTokenValue()).getBytes());
                return null;
            });
        } catch (Exception e) {
            throw new TokenStoreException("Could not delete tokens from fast-access store", e);
        }
    }

    private long calculateTtl(Expiration expiration) {
        var ttl = Duration.between(Instant.now(), expiration.value()).getSeconds();
        return Math.max(ttl, 1);
    }
}
```

Key patterns:
- Use `executePipelined` for atomic multi-key operations
- Store tokens as Redis keys with empty values (presence-only validation)
- Calculate TTL from token expiration: `Duration.between(now, expiration).getSeconds()`, minimum 1
- Wrap all Redis exceptions in application-layer `TokenStoreException`
- Use key prefixes to namespace tokens (e.g., `jwt:access_token:`, `jwt:refresh_token:`)

## Direct Port Implementation

When no translation is needed, the infrastructure class implements the port directly. No `Adapter` suffix. Cross-cutting components (e.g., `infrastructure/security/`) can implement ports from any aggregate.

```java
// Port: application/authentication/out/TokenIssuer.java
public interface TokenIssuer {
    JwtPair issueTokenPair(UserAuthentication userAuthentication);
    JwtPair issueTokenPair(Subject subject, Role role);
}

// Direct implementation: infrastructure/security/JwtIssuer.java
@Component
public class JwtIssuer implements TokenIssuer {
    private final SecretKey secretKey;
    private final Long accessTokenExpirationTime;
    private final Long refreshTokenExpirationTime;

    public JwtIssuer(
            @Value("${asapp.security.jwt-secret}") String jwtSecret,
            @Value("${asapp.security.access-token.expiration-time}") Long accessTokenExpirationTime,
            @Value("${asapp.security.refresh-token.expiration-time}") Long refreshTokenExpirationTime) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
        this.accessTokenExpirationTime = accessTokenExpirationTime;
        this.refreshTokenExpirationTime = refreshTokenExpirationTime;
    }

    @Override
    public JwtPair issueTokenPair(UserAuthentication userAuthentication) {
        var subject = Subject.of(userAuthentication.username().value());
        var role = userAuthentication.role();
        return issueTokenPair(subject, role);
    }
    // ...
}
```

Use a **delegation adapter** when the adaptee returns a richer type than the port requires:

```java
@Component
public class TokenVerifierAdapter implements TokenVerifier {
    private final JwtVerifier jwtVerifier;

    public TokenVerifierAdapter(JwtVerifier jwtVerifier) {
        this.jwtVerifier = jwtVerifier;
    }

    @Override
    public void verifyAccessToken(EncodedToken encodedToken) {
        jwtVerifier.verifyAccessToken(encodedToken); // discards DecodedJwt return
    }

    @Override
    public void verifyRefreshToken(EncodedToken encodedToken) {
        jwtVerifier.verifyRefreshToken(encodedToken);
    }
}
```

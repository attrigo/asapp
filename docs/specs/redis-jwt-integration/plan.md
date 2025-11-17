# Redis JWT Integration - Implementation Plan

**Feature**: Redis JWT Integration
**Version**: 0.3.0
**Status**: Draft
**Last Updated**: 2025-01-13

---

## Pre-Implementation Gates

### Gate 1: Simplicity Check ✅
- **Maximum 3 new components per service**:
  - JwtCacheRepository (interface)
  - RedisJwtCacheRepository (adapter)
  - RedisJwtVerifier (infrastructure)
  - ExpiredJwtCleanupScheduler (infrastructure)
- **Total**: 4 components ⚠️ (acceptable - fundamental feature)
- **Rationale**: Each component has single responsibility, cannot be simplified further

### Gate 2: Anti-Abstraction Check ✅
- **Use Spring Data Redis directly**: No custom abstraction over Redis client
- **Use Lettuce client**: Default Spring choice, no wrapper
- **Use Spring @Scheduled**: No custom job scheduler
- **Rationale**: Leverage framework capabilities, avoid premature abstraction

### Gate 3: Integration-First Check ✅
- **Contracts defined before implementation**:
  - JwtCacheRepository interface (application layer)
  - Redis data model (key/value format)
  - Component interfaces (RedisJwtVerifier)
- **Integration tests with Testcontainers**: Real Redis validation
- **E2E tests**: Full token lifecycle across services

---

## Technology Decisions

### TD-1: Spring Data Redis with Lettuce Client

**Decision**: Use `spring-boot-starter-data-redis` with Lettuce client

**Rationale**:
- Spring Boot default: Well-integrated with actuator, metrics, health checks
- Lettuce: Reactive-capable (future-proof), connection pooling, auto-reconnection
- Alternative considered: Jedis (older, blocking-only, less maintained)
- RedisTemplate: Provides high-level operations (get, set, delete, pipeline)

**Dependencies**:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

---

### TD-2: SHA-256 for Token Hashing

**Decision**: Use Java built-in `MessageDigest` with SHA-256 algorithm

**Rationale**:
- No external dependencies required
- SHA-256 provides 256-bit output (64 hex chars)
- Collision resistance sufficient for ~5K tokens
- Standard library implementation (well-tested, performant)

**Implementation**:
```java
MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))
```

---

### TD-3: Jackson for JSON Serialization

**Decision**: Use existing Jackson ObjectMapper (already in Spring Boot)

**Rationale**:
- Already used throughout project
- Spring Boot auto-configuration
- No additional dependencies
- Type-safe with record support

---

### TD-4: Redis Testcontainers for Integration Tests

**Decision**: Use `testcontainers-redis` module for integration testing

**Rationale**:
- Real Redis instance (not mocks)
- Same image as production (`redis:7-alpine`)
- Static container pattern for performance
- Already used for PostgreSQL tests

**Dependencies**:
```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <scope>test</scope>
</dependency>
```

---

## Architecture Changes

### Domain Layer

**JwtAuthentication Aggregate** (modified):
```java
public final class JwtAuthentication {
    private final JwtAuthenticationId id;
    private final UserId userId;
    private JwtPair jwtPair;
    private Instant expiredAt;  // NEW FIELD

    // NEW: Mark as expired
    public void markAsExpired(Instant expiredAt) {
        if (this.expiredAt != null) {
            return; // Already marked, immutable
        }
        validateRefreshTokenIsExpired();
        this.expiredAt = expiredAt;
    }

    // NEW: Query methods
    public boolean isExpired() {
        return this.expiredAt != null;
    }

    public boolean isActive() {
        return this.expiredAt == null;
    }

    public Instant getExpiredAt() {
        return this.expiredAt;
    }
}
```

**Value Objects** (no changes):
- Existing: Jwt, Expiration, Issued (already have timestamps)

---

### Application Layer

**New Output Port** (application/authentication/out/):
```java
public interface JwtCacheRepository {
    void storeTokenPair(Jwt accessToken, Jwt refreshToken, UserId userId);
    Optional<TokenMetadata> findToken(String token);
    void deleteTokenPair(Jwt accessToken, Jwt refreshToken);
}

public record TokenMetadata(String username, String userId, String role) {}
```

**Updated Repository** (application/authentication/out/):
```java
public interface JwtAuthenticationRepository {
    // Existing methods...

    // NEW: Expiration tracking
    int markExpiredAuthentications();
    int deleteAuthenticationsExpiredBefore(Instant cutoffDate);
}
```

**No Changes to Use Cases**:
- AuthenticateUseCase, RefreshAuthenticationUseCase, RevokeAuthenticationUseCase
- Use cases remain pure business logic
- Infrastructure adapters handle Redis operations

---

### Infrastructure Layer

**New Components**:

1. **RedisJwtCacheRepository** (infrastructure/authentication/out/):
   - Implements JwtCacheRepository interface
   - Uses RedisTemplate<String, String>
   - Handles token hashing (SHA-256)
   - Handles JSON serialization/deserialization
   - Implements pipeline for atomic operations

2. **RedisJwtVerifier** (infrastructure/security/):
   - Replaces existing JwtVerifier
   - Validates signature → type → Redis existence
   - Returns DecodedJwt with claims from Redis
   - Handles Redis connection failures (fail closed)

3. **ExpiredJwtCleanupScheduler** (infrastructure/config/):
   - @Scheduled component
   - Marks expired authentications
   - Deletes old expired authentications
   - Logs metrics via SLF4J

4. **TokenHasher** (infrastructure/security/util/):
   - Utility for SHA-256 hashing
   - Stateless, pure function
   - Used by RedisJwtCacheRepository and RedisJwtVerifier

**Updated Components**:

1. **JwtAuthenticationFilter** (infrastructure/security/web/):
   - Replace JwtVerifier with RedisJwtVerifier
   - Same filter flow, different verifier implementation

2. **DefaultJwtAuthenticationGranter** (infrastructure/authentication/out/):
   - After PostgreSQL save succeeds
   - Call jwtCacheRepository.storeTokenPair()
   - Log but don't fail on Redis errors

3. **DefaultJwtAuthenticationRefresher** (infrastructure/authentication/out/):
   - After PostgreSQL update succeeds
   - Call jwtCacheRepository.deleteTokenPair(oldTokens)
   - Call jwtCacheRepository.storeTokenPair(newTokens)
   - Use Redis pipeline for atomicity

4. **DefaultJwtAuthenticationRevoker** (infrastructure/authentication/out/):
   - Before PostgreSQL delete
   - Call jwtCacheRepository.deleteTokenPair()
   - Continue with PostgreSQL delete even if Redis fails

5. **JwtAuthenticationEntity** (infrastructure/authentication/out/entity/):
   - Add `Instant expiredAt` field
   - Mapper updated to handle new field

6. **JwtAuthenticationMapper** (infrastructure/authentication/mapper/):
   - Map expiredAt domain field ↔ entity field

---

## Data Model

### Domain Model

**JwtAuthentication Aggregate**:
```java
public final class JwtAuthentication {
    private final JwtAuthenticationId id;           // UUID
    private final UserId userId;                    // UUID
    private JwtPair jwtPair;                        // Composite: (Jwt accessToken, Jwt refreshToken)
    private Instant expiredAt;                      // NEW: null = active, non-null = expired
}
```

**New Methods**:
- `void markAsExpired(Instant expiredAt)` - Set expiration timestamp (immutable once set)
- `boolean isExpired()` - Check if authentication is expired
- `boolean isActive()` - Check if authentication is active
- `Instant getExpiredAt()` - Get expiration timestamp (null if active)

---

### Redis Data Model

**Access Token Entry**:
```
Type: String key-value
Key: jwt:access:{SHA256(token)}
Value: {"username":"user@asapp.com","userId":"uuid","role":"USER"}
TTL: 300 seconds (from JWT expiration claim)
```

**Refresh Token Entry**:
```
Type: String key-value
Key: jwt:refresh:{SHA256(token)}
Value: {"username":"user@asapp.com","userId":"uuid","role":"USER"}
TTL: 3600 seconds (from JWT expiration claim)
```

**Key Generation Algorithm**:
```java
String hash = SHA256(token);  // 64 lowercase hex chars
String key = "jwt:" + tokenType + ":" + hash;  // e.g., "jwt:access:abc123..."
```

---

### PostgreSQL Data Model

**jwt_authentications Table** (modified):
```sql
CREATE TABLE jwt_authentications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- Access Token (embedded)
    access_token_token TEXT NOT NULL,
    access_token_type VARCHAR(50) NOT NULL,
    access_token_subject TEXT NOT NULL,
    access_token_claims_token_use VARCHAR(50) NOT NULL,
    access_token_claims_role VARCHAR(50),
    access_token_issued TIMESTAMPTZ NOT NULL,
    access_token_expiration TIMESTAMPTZ NOT NULL,

    -- Refresh Token (embedded)
    refresh_token_token TEXT NOT NULL,
    refresh_token_type VARCHAR(50) NOT NULL,
    refresh_token_subject TEXT NOT NULL,
    refresh_token_claims_token_use VARCHAR(50) NOT NULL,
    refresh_token_claims_role VARCHAR(50),
    refresh_token_issued TIMESTAMPTZ NOT NULL,
    refresh_token_expiration TIMESTAMPTZ NOT NULL,

    -- NEW COLUMN
    expired_at TIMESTAMPTZ NULL,  -- When authentication became inactive

    CONSTRAINT uk_access_token UNIQUE(access_token_token),
    CONSTRAINT uk_refresh_token UNIQUE(refresh_token_token)
);

-- NEW INDEX (partial)
CREATE INDEX idx_jwt_auth_expired_at ON jwt_authentications(expired_at)
WHERE expired_at IS NOT NULL;
```

---

## Component Contracts

### JwtCacheRepository (Output Port)

**Location**: `application/authentication/out/JwtCacheRepository.java`

```java
public interface JwtCacheRepository {

    /**
     * Stores access and refresh tokens in cache atomically.
     *
     * @param accessToken the access token with expiration
     * @param refreshToken the refresh token with expiration
     * @param userId the user ID
     */
    void storeTokenPair(Jwt accessToken, Jwt refreshToken, UserId userId);

    /**
     * Finds token metadata by token string.
     *
     * @param token the raw token string
     * @return token metadata if found, empty otherwise
     */
    Optional<TokenMetadata> findToken(String token);

    /**
     * Deletes access and refresh tokens from cache atomically.
     *
     * @param accessToken the access token
     * @param refreshToken the refresh token
     */
    void deleteTokenPair(Jwt accessToken, Jwt refreshToken);
}

public record TokenMetadata(String username, String userId, String role) {}
```

---

### RedisJwtVerifier (Infrastructure Component)

**Location**: `infrastructure/security/RedisJwtVerifier.java`

```java
@Component
public class RedisJwtVerifier {

    private final JwtDecoder jwtDecoder;
    private final JwtCacheRepository jwtCacheRepository;
    private final TokenHasher tokenHasher;

    /**
     * Verifies access token signature and Redis existence.
     *
     * @param accessToken the bearer token from request
     * @return decoded JWT with claims from Redis
     * @throws InvalidJwtException if validation fails
     */
    public DecodedJwt verifyAccessToken(String accessToken) {
        // 1. Validate signature + expiration (fail fast)
        var decodedJwt = jwtDecoder.decode(accessToken);

        // 2. Validate type
        if (!decodedJwt.isAccessToken()) {
            throw new UnexpectedJwtTypeException(...);
        }

        // 3. Check Redis existence
        var tokenMetadata = jwtCacheRepository.findToken(accessToken)
            .orElseThrow(() -> new InvalidJwtException("Token not found in cache"));

        // 4. Build DecodedJwt with Redis claims (overwrite JWT claims with Redis data)
        return buildDecodedJwtFromRedis(accessToken, tokenMetadata);
    }
}
```

---

### ExpiredJwtCleanupScheduler (Infrastructure Component)

**Location**: `infrastructure/config/ExpiredJwtCleanupScheduler.java`

```java
@Component
public class ExpiredJwtCleanupScheduler {

    private final JwtAuthenticationRepository repository;

    @Scheduled(cron = "${asapp.cleanup.jwt.cron:0 0 2 * * ?}")
    public void cleanupExpiredAuthentications() {
        var retentionDays = configurationProperties.getRetentionDays();
        var cutoffDate = Instant.now().minus(retentionDays, ChronoUnit.DAYS);

        // Step 1: Mark expired
        var markedCount = repository.markExpiredAuthentications();
        logger.info("Marked {} authentications as expired", markedCount);

        // Step 2: Delete old
        var deletedCount = repository.deleteAuthenticationsExpiredBefore(cutoffDate);
        logger.info("Deleted {} authentications older than {} days", deletedCount, retentionDays);
    }
}
```

---

## Implementation Phases

### Phase 0: Infrastructure Setup

**Goal**: Set up Redis infrastructure and configuration

**Tasks**:
1. Add Spring Data Redis dependency to all service pom.xml files
2. Add Redis to docker-compose.yaml with password authentication
3. Add Redis configuration properties to application.yml (all services)
4. Add Redis health check to Spring Boot Actuator
5. Create TestContainerConfiguration for Redis (shared test infrastructure)
6. Verify Redis connectivity from all services

**Acceptance**:
- Redis starts via Docker Compose
- All services connect to Redis successfully
- Health check shows Redis UP

---

### Phase 1: Domain Model Changes

**Goal**: Add expiredAt field to JwtAuthentication domain

**Tasks**:
1. Add `expiredAt` field to JwtAuthentication class
2. Add `markAsExpired(Instant)` method with validation
3. Add `isExpired()` and `isActive()` query methods
4. Add `getExpiredAt()` getter
5. Update equals/hashCode if needed (likely not - based on id)
6. Write unit tests for new methods

**Acceptance**:
- Domain tests pass
- expiredAt field validated (immutable once set)
- State query methods work correctly

---

### Phase 2: Database Schema Migration

**Goal**: Add expired_at column to jwt_authentications table

**Tasks**:
1. Create Liquibase changeset: `20250113_1_add_expired_at_column.xml`
2. Add column with TIMESTAMPTZ type, nullable
3. Create partial index on expired_at (WHERE expired_at IS NOT NULL)
4. Include rollback instructions
5. Update JwtAuthenticationEntity record with expiredAt field
6. Update JwtAuthenticationMapper to map expiredAt
7. Run migration locally and verify

**Acceptance**:
- Migration executes successfully
- Column added, index created
- Rollback works correctly
- Entity/mapper handle new field

---

### Phase 3: Repository Cleanup Methods

**Goal**: Add repository methods for expiration marking and cleanup

**Tasks**:
1. Add `markExpiredAuthentications()` to JwtAuthenticationRepository interface
2. Add `deleteAuthenticationsExpiredBefore(Instant)` to JwtAuthenticationRepository interface
3. Implement methods in JwtAuthenticationJdbcRepository using @Query
4. Write integration tests with Testcontainers PostgreSQL
5. Test batch operations and performance

**Acceptance**:
- Repository tests pass
- Mark operation updates correct records
- Delete operation removes only old expired records
- Performance meets NFR (<5s for 100K tokens)

---

### Phase 4: Redis Cache Repository (Authentication Service)

**Goal**: Implement Redis storage for JWT tokens in authentication service

**Tasks**:
1. Create TokenMetadata record in domain layer
2. Create JwtCacheRepository interface in application/authentication/out/
3. Create TokenHasher utility in infrastructure/security/util/
4. Create RedisJwtCacheRepository in infrastructure/authentication/out/
5. Implement storeTokenPair() with Redis pipeline
6. Implement findToken() with SHA-256 hashing
7. Implement deleteTokenPair() with Redis pipeline
8. Write unit tests (mock RedisTemplate)
9. Write integration tests (Testcontainers Redis)

**Acceptance**:
- All JwtCacheRepository tests pass (unit + integration)
- Token storage verified in real Redis
- TTL set correctly (matches JWT expiration)
- Atomic operations verified

---

### Phase 5: Integrate Redis into Authentication Operations (Authentication Service)

**Goal**: Update granter/refresher/revoker to use Redis

**Tasks**:
1. Update DefaultJwtAuthenticationGranter:
   - Inject JwtCacheRepository
   - After PostgreSQL save, call storeTokenPair()
   - Handle Redis failures gracefully (log, don't fail login)
2. Update DefaultJwtAuthenticationRefresher:
   - After PostgreSQL update, delete old tokens from Redis
   - Store new tokens in Redis
   - Use try-catch for Redis errors
3. Update DefaultJwtAuthenticationRevoker:
   - Before PostgreSQL delete, delete from Redis
   - Continue even if Redis fails
4. Write unit tests (mock repository)
5. Write integration tests (real Redis)

**Acceptance**:
- Login stores tokens in Redis
- Refresh invalidates old tokens, stores new ones
- Revoke removes tokens from Redis
- Tests verify Redis operations

---

### Phase 6: Redis JWT Verifier (All Services)

**Goal**: Create RedisJwtVerifier for filter validation

**Tasks**:
1. Create RedisJwtVerifier in infrastructure/security/ (all services)
2. Inject JwtDecoder and JwtCacheRepository
3. Implement verifyAccessToken(String):
   - Decode and validate signature/type
   - Check Redis for token
   - Build DecodedJwt from Redis metadata
   - Handle Redis failures (throw InvalidJwtException)
4. Write unit tests (mock dependencies)
5. Write integration tests (Testcontainers Redis)

**Acceptance**:
- RedisJwtVerifier tests pass
- Signature validation before Redis lookup
- Redis lookup uses SHA-256 hash
- Fail closed on Redis errors

---

### Phase 7: Update Authentication Filters (All Services)

**Goal**: Replace JwtVerifier with RedisJwtVerifier in filters

**Tasks**:
1. Update JwtAuthenticationFilter in authentication service
2. Update JwtAuthenticationFilter in users service
3. Update JwtAuthenticationFilter in tasks service
4. Update filter to inject RedisJwtVerifier
5. Remove old JwtVerifier component
6. Update integration tests (SecurityConfigurationIT)

**Acceptance**:
- All filters use RedisJwtVerifier
- Integration tests pass
- Tokens in Redis accepted
- Tokens not in Redis rejected
- Redis failure causes 401

---

### Phase 8: Cleanup Scheduler (Authentication Service)

**Goal**: Implement scheduled cleanup job

**Tasks**:
1. Create CleanupConfiguration properties class
2. Create ExpiredJwtCleanupScheduler component
3. Inject JwtAuthenticationRepository
4. Implement scheduled method with mark + delete logic
5. Add logging for marked/deleted counts
6. Enable @EnableScheduling in application
7. Write unit tests (mock repository)
8. Write integration tests (verify scheduling)

**Acceptance**:
- Scheduler runs at configured time
- Marks expired authentications
- Deletes old expired authentications
- Logs execution metrics

---

### Phase 9: Observability Integration

**Goal**: Add Redis metrics to Prometheus/Grafana

**Tasks**:
1. Configure Micrometer metrics for RedisTemplate
2. Add custom metrics:
   - Counter: `jwt_redis_lookups_total{result}`
   - Timer: `jwt_redis_lookup_duration_seconds`
   - Counter: `jwt_cleanup_marked_total`
   - Counter: `jwt_cleanup_deleted_total`
3. Expose via Spring Boot Actuator
4. Create/update Grafana dashboard
5. Configure Prometheus scraping

**Acceptance**:
- Metrics visible in Prometheus
- Grafana dashboard displays Redis stats
- Cleanup job metrics tracked

---

### Phase 10: End-to-End Testing

**Goal**: Validate complete token lifecycle across services

**Tasks**:
1. Write E2E test: Login → use in users service → success
2. Write E2E test: Login → revoke → use in tasks service → 401
3. Write E2E test: Login → wait for expiration → 401
4. Write E2E test: Login → refresh → old token 401, new token 200
5. Write E2E test: Redis down → all requests 401
6. Performance test: 1000 req/s with Redis validation

**Acceptance**:
- All E2E tests pass
- Performance meets NFR (<20ms p99 filter overhead)
- Redis failure properly handled

---

## Testing Strategy

### Unit Tests (>80% coverage)

**Components**:
- TokenHasher: SHA-256 correctness
- RedisJwtCacheRepository: mocked RedisTemplate
- RedisJwtVerifier: mocked dependencies
- ExpiredJwtCleanupScheduler: mocked repository
- JwtAuthentication: expiredAt logic

**Tools**: JUnit 5, Mockito, AssertJ

---

### Integration Tests

**Scenarios**:
- RedisJwtCacheRepository with real Redis (Testcontainers)
- JwtAuthenticationJdbcRepository mark/delete queries
- RedisJwtVerifier with real Redis
- Filter validation with Redis
- Cleanup scheduler execution

**Tools**: @SpringBootTest, @DataJdbcTest, Testcontainers

---

### E2E Tests

**Scenarios**:
- Full authentication flow with Redis
- Cross-service token validation
- Token lifecycle (login → use → revoke → rejected)
- Redis failure scenarios

**Tools**: @SpringBootTest(RANDOM_PORT), WebTestClient, MockServer, Testcontainers

---

## Configuration Management

### Redis Configuration

**application.yml** (all services):
```yaml
spring:
  data:
    redis:
      host: redis
      port: 6379
      password: ${REDIS_PASSWORD}
      timeout: 2000ms
      lettuce:
        pool:
          max-active: 20
          max-idle: 10
          min-idle: 5
```

### Cleanup Configuration

**application.yml** (authentication service only):
```yaml
asapp:
  cleanup:
    jwt:
      enabled: true
      cron: "0 0 2 * * ?"  # Daily at 2 AM
      retention-days: 30
      batch-size: 1000
```

### Redis Docker Compose

**docker-compose.yaml**:
```yaml
redis:
  image: redis:7-alpine
  container_name: asapp-redis
  ports:
    - "6379:6379"
  environment:
    - REDIS_PASSWORD=${REDIS_PASSWORD}
  command: >
    redis-server
    --requirepass ${REDIS_PASSWORD}
    --appendonly yes
    --maxmemory 100mb
    --maxmemory-policy volatile-lru
  volumes:
    - redis-data:/data
  networks:
    - asapp-network
  healthcheck:
    test: ["CMD", "redis-cli", "--raw", "incr", "ping"]
    interval: 10s
    timeout: 3s
    retries: 5

volumes:
  redis-data:
```

---

## Deployment Steps

### Step 1: Deploy Redis Infrastructure
1. Update docker-compose.yaml
2. Set REDIS_PASSWORD environment variable
3. Start Redis: `docker-compose up -d redis`
4. Verify health: `docker-compose ps`

### Step 2: Deploy Database Migration
1. Run Liquibase migration
2. Verify column added: `\d jwt_authentications`
3. Verify index created: `\di idx_jwt_auth_expired_at`

### Step 3: Deploy Application Changes (Feature Flag OFF)
1. Deploy authentication service with Redis code (feature flag: disabled)
2. Deploy users service with Redis code (feature flag: disabled)
3. Deploy tasks service with Redis code (feature flag: disabled)
4. Verify services start successfully
5. Verify existing tests pass (signature-only mode)

### Step 4: Enable Feature Gradually
1. Enable Redis in authentication service (update config)
2. Monitor metrics, logs for 1 hour
3. Enable Redis in users service
4. Monitor metrics, logs for 1 hour
5. Enable Redis in tasks service
6. Monitor metrics, logs for 24 hours

### Step 5: Verify Production
1. Test login → token in Redis
2. Test revoke → token removed from Redis
3. Test protected endpoints → Redis validation
4. Monitor cleanup job execution (next 2 AM)
5. Verify Prometheus metrics collecting

---

## Rollback Plan

### If Issues Detected

1. **Disable feature flag**: Revert to signature-only validation
2. **Services continue**: No downtime required
3. **Redis optional**: Can stop Redis container
4. **Database unchanged**: expired_at column remains (backward compatible)

### If Migration Fails

1. **Rollback Liquibase**: `mvn liquibase:rollback -Dliquibase.rollbackCount=1`
2. **Column removed**: Services work with old schema
3. **No data loss**: Rollback is clean

---

## Risk Mitigation

| Risk | Mitigation Strategy |
|------|-------------------|
| Redis unavailable at startup | Services fail to start with clear error message; operator starts Redis first |
| Redis fills memory | volatile-lru eviction removes oldest TTL keys; alerts on memory >80% |
| Cleanup job too slow | Batch size configurable; runs during low-traffic hours; async execution doesn't block |
| Redis-PostgreSQL desync | Redis is cache-only; PostgreSQL is source of truth; can rebuild Redis from PostgreSQL if needed |
| Performance degradation | Monitor p99 latency; alert if >50ms; can temporarily disable feature flag |

---

## Success Criteria Validation

### Performance Targets
- [ ] Filter latency p99 < 20ms (load test 1000 req/s)
- [ ] Redis lookup p99 < 10ms (verified via metrics)
- [ ] Cleanup job < 5s for 100K tokens (integration test)

### Security Targets
- [ ] Revoked tokens rejected within 100ms (E2E test)
- [ ] No revoked token false positives (zero tolerance)
- [ ] 30-day audit trail maintained (verify retention)

### Reliability Targets
- [ ] Redis connection pool never exhausted (monitor active connections)
- [ ] Fail closed behavior verified (kill Redis during test)
- [ ] Automatic reconnection works (restart Redis during test)

---

## Open Issues

**None** - All configuration decisions finalized

---

## Related Documents

- `spec.md` - Feature specification
- `requirements.md` - Detailed requirements
- `docs/claude/architecture.md` - Current architecture
- `docs/claude/testing.md` - Testing patterns

---

## Approval Checklist

- [ ] Architecture design reviewed and approved
- [ ] Component contracts reviewed
- [ ] Phase breakdown validated
- [ ] Testing strategy approved
- [ ] Deployment plan approved
- [ ] Rollback plan documented

---

**Ready for Implementation**: ✅ All gates passed, all decisions documented

# Redis JWT Integration - Feature Specification

**Version**: 0.3.0
**Status**: Draft
**Author**: Architecture Team
**Date**: 2025-01-13

---

## Overview

Integrate Redis as a distributed cache for JWT token validation across all microservices. This feature enables fast, centralized token validation and instant token revocation while maintaining PostgreSQL as the authoritative persistence layer for audit trails.

**Problem Statement**: Currently, JWT validation in authentication filters is stateless (signature-only), preventing instant token revocation. The authentication service persists tokens in PostgreSQL, but users/tasks services cannot verify if tokens have been revoked without cross-service database queries.

**Solution**: Introduce Redis as a distributed validation cache that all services consult during authentication filter processing, enabling instant revocation and centralized session management.

---

## User Stories

### US-1: Token Validation with Revocation Check
**As a** microservice authentication filter
**I want** to validate JWT tokens against both signature and Redis existence
**So that** revoked tokens are immediately rejected across all services

**Acceptance Criteria**:
- Filter validates token signature first (fail fast)
- Filter checks Redis for token existence
- Revoked tokens (not in Redis) are rejected with 401 Unauthorized
- Valid tokens (in Redis with valid signature) are accepted

---

### US-2: Centralized Token Storage on Authentication
**As an** authentication service
**I want** to store JWT tokens in Redis when users authenticate
**So that** all services can validate tokens against a single source of truth

**Acceptance Criteria**:
- On successful login, access token is stored in Redis with 5-minute TTL
- On successful login, refresh token is stored in Redis with 1-hour TTL
- Redis key format is deterministic and consistent
- TTL matches token expiration from JWT claims
- PostgreSQL record is also created (dual storage)

---

### US-3: Instant Token Revocation
**As an** authentication service
**I want** to delete tokens from Redis when revoking authentication
**So that** revoked tokens are immediately invalid across all services

**Acceptance Criteria**:
- Revoke operation deletes tokens from both Redis and PostgreSQL
- Deletion from Redis happens before PostgreSQL (fail fast)
- All services immediately reject revoked tokens (within Redis propagation time)
- Revoke operation is idempotent

---

### US-4: Token Refresh with Cache Update
**As an** authentication service
**I want** to update Redis when refreshing authentication tokens
**So that** old tokens are invalidated and new tokens are immediately usable

**Acceptance Criteria**:
- Old tokens are deleted from Redis
- New tokens are stored in Redis with updated TTL
- PostgreSQL record is updated atomically
- Operation is transactional (all or nothing)

---

### US-5: Automatic Cleanup of Expired Tokens
**As a** system administrator
**I want** expired tokens automatically removed from PostgreSQL
**So that** the database doesn't accumulate zombie authentication records

**Acceptance Criteria**:
- Scheduled job runs daily at 2 AM
- Deletes tokens expired more than 30 days ago (retention policy)
- Job logs number of records deleted
- Job does not impact system performance during execution
- Retains tokens for audit trail (30-day window)

---

## Functional Requirements

### FR-1: Redis Token Storage Format

**Access Token Key**: `jwt:access:{SHA256(token)}`
**Refresh Token Key**: `jwt:refresh:{SHA256(token)}`

**Value Structure** (JSON):
```json
{
  "username": "user@asapp.com",
  "userId": "uuid",
  "role": "USER"
}
```

**TTL**: Matches token expiration from JWT claims
- Access token: 5 minutes (300 seconds)
- Refresh token: 1 hour (3600 seconds)

**Rationale**:
- SHA256 hash prevents raw token storage in Redis
- JSON value enables future extensions (rate limiting, session metadata)
- TTL ensures automatic cleanup

---

### FR-2: Filter Validation Flow

**Sequence**:
1. Extract Bearer token from Authorization header
2. Validate JWT signature and expiration (existing `JwtDecoder`)
3. **NEW**: Check token exists in Redis (hash token, lookup key)
4. If not in Redis → reject (401 Unauthorized)
5. If in Redis → extract claims, build `JwtAuthenticationToken`, proceed

**Failure Modes**:
- Invalid signature → 401 Unauthorized (skip Redis check)
- Expired token → 401 Unauthorized (skip Redis check)
- Valid signature but not in Redis → 401 Unauthorized (revoked)
- Redis connection failure → 401 Unauthorized (fail closed - security over availability)

---

### FR-3: Authentication Service Operations

**Login (Authenticate)**:
1. Validate credentials (existing logic)
2. Generate JWT tokens (existing logic)
3. Save to PostgreSQL (existing logic)
4. **NEW**: Push tokens to Redis with TTL

**Refresh**:
1. Verify refresh token in PostgreSQL (existing logic)
2. Generate new token pair (existing logic)
3. **NEW**: Delete old tokens from Redis
4. Update PostgreSQL (existing logic)
5. **NEW**: Push new tokens to Redis with TTL

**Revoke**:
1. **NEW**: Delete tokens from Redis (first, for immediate effect)
2. Delete from PostgreSQL (existing logic)

---

### FR-4: Authentication Expiration Tracking

**New Domain Field**: `expiredAt` (nullable timestamp)

**Purpose**: Explicitly track when an authentication became inactive (refresh token expired)

**Setting Logic**:
- Set by ANY process that detects `refresh_token_expiration < now()`
- Processes include: cleanup job, filter validation, endpoint validation
- Field is immutable once set (first detection wins)
- Value: timestamp when expiration was detected

**Benefits**:
- Explicit state tracking (active vs expired)
- Single source of truth for cleanup queries
- Metrics on authentication lifetime

---

### FR-5: Cleanup Scheduler

**Schedule**: Daily at 2:00 AM (configurable via cron expression)

**Logic**:
```
retention_period = 30 days
cutoff_date = now() - retention_period

# Step 1: Mark expired authentications
UPDATE jwt_authentications
SET expired_at = now()
WHERE expired_at IS NULL
  AND refresh_token_expiration < now()

# Step 2: Delete old expired authentications
DELETE FROM jwt_authentications
WHERE expired_at IS NOT NULL
  AND expired_at < cutoff_date
```

**Monitoring**: Log marked count, deleted count, and execution time

---

## Non-Functional Requirements

### NFR-1: Performance
- Redis lookup latency: < 10ms (p99)
- Filter overhead: < 20ms additional latency
- Cleanup job execution: < 5 seconds for 100K tokens

### NFR-2: Availability
- Redis downtime handling: Fail closed - reject all requests (prioritize security)
- Redis connection pool: Minimum 10 connections per service
- Fallback strategy if Redis unavailable: None - return 401 Unauthorized

### NFR-3: Scalability
- Support up to 1,000 concurrent active users (~5,000 tokens in Redis)
- Cleanup job handles 1M+ expired tokens without blocking
- Redis memory limit: 100MB with volatile-lru eviction policy

### NFR-4: Security
- Token hashing: SHA256 before Redis storage (prevent token leakage)
- Redis authentication: Password-protected using requirepass
- Network encryption: Not required (Docker network isolation sufficient for development/small production)

### NFR-5: Observability
- Metrics: Redis hit/miss rate, lookup latency, connection pool stats
- Logs: Token revocation events, cleanup job results, Redis failures
- Alerts: Redis connection failures, high miss rate (>10%)

---

## Acceptance Criteria

### AC-1: Token Lifecycle
- [ ] User login stores tokens in both PostgreSQL and Redis
- [ ] Token refresh removes old tokens from Redis and adds new ones
- [ ] Token revocation removes tokens from Redis immediately
- [ ] Expired tokens auto-delete from Redis (via TTL)
- [ ] Refresh token expiration triggers expiredAt field update in PostgreSQL
- [ ] Authentications with expiredAt older than 30 days are deleted from PostgreSQL

### AC-2: Filter Validation
- [ ] Authentication filter checks Redis for access tokens
- [ ] Users service filter checks Redis for access tokens
- [ ] Tasks service filter checks Redis for access tokens
- [ ] Tokens not in Redis are rejected (even if signature valid)
- [ ] Signature validation happens before Redis lookup (optimization)

### AC-3: Edge Cases
- [ ] Multiple concurrent logins by same user create separate Redis entries
- [ ] Token refresh invalidates old tokens in Redis atomically
- [ ] Revoked tokens cannot be used even if signature still valid
- [ ] Expired tokens in Redis are auto-removed (no manual cleanup)
- [ ] Redis connection failure causes 401 responses (fail closed behavior)

### AC-4: Backward Compatibility
- [ ] PostgreSQL schema change is backward-compatible (adds nullable column)
- [ ] Existing endpoints behavior unchanged (authenticate, refresh, revoke)
- [ ] No breaking changes to API responses
- [ ] Domain model enhanced with expiredAt field (non-breaking addition)

---

## Out of Scope

The following are explicitly **NOT** part of this feature:

- ❌ Redis cluster setup (single instance deployment)
- ❌ Token rotation policy changes (keep existing 5min/1hour)
- ❌ Session management UI (admin panel to view active tokens)
- ❌ Rate limiting based on tokens
- ❌ Geolocation tracking of token usage
- ❌ Multi-factor authentication integration

---

## Assumptions

1. **Redis Deployment**: Single Redis instance in Docker Compose for both development and production
2. **Network Latency**: Redis is deployed in same datacenter/VPC as services (<5ms latency)
3. **Redis Persistence**: Redis configured with AOF persistence for durability
4. **Token Hashing**: SHA256 is sufficient for token key generation
5. **Retention Policy**: 30-day audit trail is acceptable business requirement
6. **Cleanup Frequency**: Daily cleanup is sufficient (not hourly/real-time)
7. **Failure Mode**: Fail closed - services reject requests when Redis is unavailable (prioritize security)

---

## Dependencies

### External
- Redis 7.x or higher (key expiration, JSON support)
- Spring Data Redis 3.x
- Lettuce client (reactive support)

### Internal
- Existing JWT domain model (Jwt, Expiration, Issued)
- Existing repositories (JwtAuthenticationRepository)
- Existing JwtVerifier infrastructure components

---

## Success Metrics

### Performance
- Filter validation latency: Target <20ms (p99), acceptable <50ms
- Redis cache hit rate: Target >98%, critical <95%
- Cleanup job duration: Target <5s, acceptable <30s

### Security
- Token revocation propagation: <100ms across all services
- Zero revoked token false positives (revoked token passes filter)
- Audit trail retention: 100% for 30 days

### Reliability
- Redis availability: >99.9%
- Service degradation on Redis failure: Services become unavailable (fail closed mode)

---

## Configuration Decisions

All open questions have been resolved with the following decisions:

1. **Redis Failure Strategy**: ✅ **Fail Closed (Option A)**
   - All services reject requests when Redis is unavailable
   - Return 401 Unauthorized for all protected endpoints
   - Prioritizes security over availability

2. **Redis Security Configuration**: ✅ **Password Authentication**
   - Password-protected using Redis `requirepass` configuration
   - No TLS encryption (Docker network isolation sufficient)
   - No ACL (password-based auth is adequate for <1000 users)

3. **Redis Memory Limits**: ✅ **100MB with volatile-lru**
   - Expected users: <1,000 concurrent users
   - Expected tokens: ~5,000 active tokens in Redis
   - Eviction policy: `volatile-lru` (evict least-recently-used keys with TTL)
   - Memory limit: 100MB

4. **Deployment Configuration**: ✅ **Docker Compose for both environments**
   - Development: Redis in docker-compose.yaml
   - Production: Same Docker Compose setup (suitable for <1000 users)
   - Connection pool: 10-20 connections per service

5. **Monitoring Integration**: ✅ **Integrate with Prometheus/Grafana**
   - Add Redis metrics to existing Prometheus scraping
   - Create Grafana dashboard for Redis monitoring
   - Alert on connection failures and high miss rates

---

## Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| Redis single point of failure | High - all auth fails | Monitor Redis health, implement automated restart, document manual recovery procedure |
| Redis-PostgreSQL sync issues | Medium - data inconsistency | Treat Redis as cache-only, PostgreSQL as source of truth |
| Memory exhaustion in Redis | Medium - eviction or OOM | Configure maxmemory-policy, monitor token volume |
| Cleanup job performance | Low - DB contention | Run during low-traffic hours, batch delete with limits |
| Token hash collisions | Very Low - security risk | SHA256 provides sufficient entropy |

---

## Related Documentation

- `docs/claude/architecture.md` - Current JWT architecture
- `docs/claude/testing.md` - Testing strategy
- Liquibase changelog for jwt_authentications table
- JwtAuthentication domain aggregate

---

## Approval Checklist

- [x] All configuration decisions resolved
- [ ] User stories reviewed and approved
- [ ] Non-functional requirements validated
- [ ] Security review completed
- [ ] Performance targets confirmed
- [ ] Operational runbook prepared (Redis maintenance)

---

**Next Steps**:
1. Create `requirements.md` with detailed technical requirements
2. Create implementation plan (`plan.md`)
3. Define data model details (`data-model.md`)
4. Write acceptance tests (`quickstart.md`)

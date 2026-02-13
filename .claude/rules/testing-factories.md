---
paths:
  - "**/testutil/**/*Factory.java"
  - "**/testutil/**/Factory.java"
---

# Test Factory Maintenance

Guidelines for creating and maintaining test data factories (Object Mother + Builder patterns).

## 1. When to Create a New Factory

### 1.1 Creation Criteria

**Rules**:
- Domain aggregate or important entities
- Used in 5+ test files
- Complex construction (3+ parameters or multi-step creation)
- Multiple test representations needed (domain + JDBC entity)

**DON'T**: Create factories for simple value objects (1-2 fields), test-specific DTOs (1-2 tests), or framework objects

### 1.2 Naming Convention

Pattern: `<DomainEntity>Factory.java`

Examples: `UserFactory`, `JwtFactory`, `JwtAuthenticationFactory`, `JwtPairFactory`

## 2. Factory Structure

### 2.1 Standard Template

**Pattern**: Follow existing factory structure (see `UserFactory`, `JwtFactory`)

### 2.2 Javadoc Requirements

**Class-level** (required):
- Brief description + optional `<p>` paragraph
- **MUST** include `@since` tag with module version

**Method-level** (optional):
- Not required for simple methods with self-explanatory names
- Required for complex behavior or framework references (`@see`)

## 3. Naming Conventions

### 3.1 Semantic Method Names

**Domain entities (with business meaning):**
- Pattern: `a/an<Adjective><Entity>()`
- Examples: `anActiveUser()`, `anInactiveUser()`, `anAdminUser()`

**Value objects (with business meaning):**
- Pattern: `<adjective><Entity>()`
- Examples: `accessToken()`, `refreshToken()`, `expiredAccessToken()`

**Default instances (no specific business meaning):**
- Pattern: `a/an<Entity>()`
- Examples: `aUser()`, `aJwtPair()`, `aJwtAuthentication()`

**Builder entry points:**
- Pattern: `a/an<Entity>Builder()`
- Rules: The `Builder` suffix distinguishes builders from semantic factory methods
- Examples: `aUserBuilder()`, `aJwtBuilder()`, `aJwtPairBuilder()`

**Technical representations:**
- Pattern: `a/an<Technology><Entity>()`
- Rules: For infrastructure-specific entities (JDBC, JPA, etc.)
- Examples: `aJdbcUser()`, `aJdbcJwtAuth()`

**Rules**:
- Use business language, not technical jargon
- No verb prefixes (`create`, `make`, `build`)
- Avoid artificial adjectives ("default", "valid", "standard") - use `a<Entity>()` instead
- Use adjective prefixes when multiple representations of the same concept exist to prevent import collisions and maintain call-site clarity (`encodedToken`, `decodedToken`)

### 3.2 Wither Method Names

**Standard pattern:** `with<PropertyName>(<primitiveType>)`

**Rules**:
- Name must indicate primitive type, not value object
- Use full property names (no abbreviations)
- Match domain attribute names exactly

Examples:
```java
// Simple property
withUserId(UUID userId)

// Nested property
withAccessTokenValue(String value)

// State modifier
withAccessTokenExpired()
expired()

// Convenience (multiple properties)
withTokenValues(String accessToken, String refreshToken)
```

**Type ambiguity example:**
```java
// Domain has: EncodedToken (value object)
// Factory field: String encodedToken

// GOOD - Clear it takes primitive
withTokenValue(String value)

// BAD - Confusing (EncodedToken is a type)
withEncodedToken(String value)
```

## 4. Wither Parameter Types

| Type | When to Use | Example |
|------|-------------|---------|
| **Primitives** (default) | All standard withers | `withUsername(String)`, `withUserId(UUID)` |
| **Enums** | Domain uses enum | `withRole(Role)`, `withType(JwtType)` |
| **Collections** | Property is collection | `withClaims(Map<String, Object>)` |
| **Value Objects** | Only for composition | `withAccessToken(Jwt)` |
| **Entities** | Never | Use ID primitives instead |

**Rules**: Prefer primitives - factory constructs value objects in `build()`

## 5. Method Addition Criteria

### 5.1 When to Add Semantic Defaults

**Rules** (ALL must be met):
- Pattern appears **10+ times** across test files
- Reduces **4+ lines** to 1 line
- Represents **fixed configuration** (not variable)
- Has **clear business meaning**

**Evidence required:**
```bash
# Search for pattern BEFORE adding
grep -r "aJwt().accessToken().expired().build()" src/test --include="*.java"
```

### 5.2 When to Add Wither Methods

**Rules**:
- Add for all domain properties (completeness)
- Remove if redundant (two ways to do same thing)
- Map 1:1 with domain attributes

### 5.3 Decision Checklist

Before adding any method:

1. Search for pattern: `grep -r "pattern" src/test`
2. Count occurrences (10+ for semantic defaults?)
3. Measure savings (4+ lines reduced?)
4. Check alternatives (builder already sufficient?)
5. Validate uniqueness (no similar method exists?)

If any answer is "no", don't add (YAGNI)

### 5.4 Anti-Patterns

**DON'T**:
- Add based on "might be useful" (wait for actual need)
- Add for rare scenarios (1-3 uses - use builder)
- Add for variable data (semantic defaults are for fixed configs)
- Add redundant methods (one way to do things)

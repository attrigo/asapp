---
paths:
  - "**/testutil/**/*Mother.java"
  - "**/testutil/**/Mother.java"
---

# Test Factory Maintenance

Guidelines for creating and maintaining test data factories (Object Mother + Builder patterns)

## 1. When to Create a New Factory

Create a factory when all of these apply:
- Domain aggregate or important entity
- Used in 3+ test files
- Complex construction (3+ parameters or multi-step creation)
- Multiple test representations needed (domain + JDBC entity)

## 2. Factory Structure

### 2.1 Default Values

- All factory fields must have fixed default values for reproducibility; use dynamic values only when a fixed value produces incorrect behaviour in any test type using the factory (e.g., a fixed past timestamp produces negative Redis TTL in integration tests)

### 2.2 Dual Build Outputs

- Factories for persisted aggregates provide `buildJdbc()` for infrastructure entity output alongside `build()` for domain output

### 2.3 Factory Composition

- Complex factories delegate to simpler factories (e.g., `JwtMother.build()` delegates to `EncodedTokenMother`) rather than duplicating construction logic

## 3. Naming Conventions

**Semantic methods**:
- No verb prefixes (`create`, `make`, `build`)
- Avoid artificial adjectives ("default", "valid", "standard") — use `a<Entity>()` instead
- Use adjective prefixes when multiple representations of the same concept exist to prevent import collisions and maintain call-site clarity (`encodedToken`, `decodedToken`)

**Wither methods**: Match domain attribute names exactly, no abbreviations

## 4. Wither Parameter Types

- Never accept entities as parameters — use ID primitives instead; factory constructs value objects in `build()`

## 5. Method Addition Criteria

### 5.1 When to Add Semantic Defaults

**Pattern**: The builder's fluent API provides all the flexibility, don't create a new static method for every variation

Add a semantic default method when all of these are met:
- Pattern appears 10+ times across test files
- Reduces 4+ builder calls to 1 semantic method
- Represents fixed configuration (not variable data)
- Has clear business meaning

### 5.2 When to Add Wither Methods

- Withers map 1:1 with domain attributes

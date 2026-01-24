---
paths:
  - "**/*Tests.java"
---

# Unit Test Patterns

Mocking-specific patterns for unit tests with mocked dependencies.

---

## 1. Test Setup

### 1.1 Mock Usage by Layer

**Rules**:
- **Domain tests** (`domain/**/*Tests.java`): NEVER use mocks - domain must be infrastructure-agnostic
- **Application/Infrastructure tests**: Use `@ExtendWith(MockitoExtension.class)` when mocking dependencies

**DON'T**: Mock domain objects or value objects in domain layer tests

---

## 2. Test Patterns

### 2.1 Parameterized Tests

**Rules**:
- Use `@ParameterizedTest` when testing same logic with multiple inputs (boundary values, invalid formats, equivalent partitions)

**Examples**: Validation rules, error conditions, format variations

---

## 3. Mocking Patterns

### 3.1 BDDMockito (Not Regular Mockito)

**Rules**:
- Use BDDMockito syntax exclusively, NEVER mix with Mockito syntax

### 3.2 Mock Precision with Specific Variables

**Rules**:
- Use specific variables instead of `any()` matchers for better test precision
- Only use `any()` when exact value is irrelevant or testing generic error handling

**Examples**:
```java
// CORRECT: Specific variables
var username = Username.of("user@asapp.com");
given(authenticator.authenticate(username, password)).willReturn(userAuth);
then(authenticator).should().authenticate(username, password);

// AVOID: Generic matchers
given(authenticator.authenticate(any(), any())).willReturn(userAuth);
then(authenticator).should().authenticate(any(), any());
```

### 3.3 ArgumentCaptor

**Rules**:
- Use ArgumentCaptor to verify arguments passed to methods that modify state without returning data

**Examples**: Repository save operations, event publishers, logging calls

### 3.4 Mock Verification

**Rules**:
- Verify mock interactions in Then block (unless interaction is truly irrelevant to test scenario)
- Avoid over-verification of non-critical interactions (logging, metrics)

### 3.5 InOrder Verification

**Rules**:
- Verify interaction order when sequence matters for correctness (validation before persistence, compensating actions)
- Use `inOrder(mock1, mock2, ...)` in Then block

**Examples**: Authentication before token generation, deletion before recreation, rollback sequences

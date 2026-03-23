---
paths:
  - "**/*Tests.java"
---

# Unit Test Patterns

Mocking-specific patterns for unit tests with mocked dependencies

## 1. Test Setup

### 1.1 Mock Usage by Layer

**Rules**:
- **Domain tests** (`domain/**/*Tests.java`): NEVER use mocks - domain must be infrastructure-agnostic
- **Application/Infrastructure tests**: Use `@ExtendWith(MockitoExtension.class)` when mocking dependencies

## 2. Test Patterns

### 2.1 Parameterized Tests

**Rules**:
- Use `@ParameterizedTest` when testing same logic with multiple inputs (boundary values, invalid formats, equivalent partitions)
- Method name describes the shared behavior: `ThrowsException_InvalidUsername`, not individual cases
- Ordering: treated as a single test — place by its behavior category (failure/success)
- Source selection: `@MethodSource` for multi-parameter, `@ValueSource` for single primitives, `@EnumSource` for enum exhaustiveness. Do NOT use `@CsvSource`

## 3. Mocking Patterns

### 3.1 BDDMockito (Not Regular Mockito)

**Rules**:
- Use BDDMockito syntax exclusively, NEVER mix with Mockito syntax

### 3.2 Mock Precision with Specific Variables

**Rules**:
- Use specific variables instead of `any()` matchers for better test precision
- Only use `any()` when exact value is irrelevant or testing generic error handling
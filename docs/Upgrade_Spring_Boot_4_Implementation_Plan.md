# Spring Boot 4 Upgrade — Implementation Plan

## Header

- **Branch:** upgrade-spring-boot-4
- **Date:** 2026-04-05
- **Author:** attrigo

---

## Audience

This document is addressed to Claude Code. You are the implementer — you will execute every step in this plan.
Read the "How to read this document" section in full before executing any step.

---

## How to read this document

### Plan Body Schema

#### General rules
- A step using explicit scope should touch no more than ~10 files. If more, use derived scope with a transform rule instead.

#### Precondition types
| Type | Meaning | Claude Code action |
|------|---------|-------------------|
| `[phase]` | Entire phase must be complete | Verify against plan execution state |
| `[step]` | Specific step must be complete | Verify against plan execution state |
| `[branch]` | External branch must be merged | Run `git log` / `git branch` to confirm |
| `[file]` | File must exist | Check file exists at path |
| `[class]` | Class must exist | Grep for class definition |
| `[env]` | Local tool/runtime prerequisite verifiable by a shell command (e.g. `java -version`) | Run the specified command and assert the expected output |
| `[user]` | Prerequisite requiring manual user action (not verifiable by Claude) | Stop, list every `[user]` requirement, and wait for explicit user confirmation before proceeding |

#### Acceptance criteria types
| Type | Meaning | Claude Code action |
|------|---------|-------------------|
| `[grep]` | Pattern must have at least one match | Run grep, assert non-empty |
| `[no-match]` | Pattern must return 0 results | Run grep, assert empty |
| `[compile]` | Module must compile without errors | Run mvn compile |
| `[test]` | Specific test(s) must pass | Run mvn test |
| `[file-exists]` | File must exist at path | Check file exists |
| `[manual]` | Human must run a command/tool and verify output by reading it | Stop and instruct the user exactly what to run and what to look for |

#### Scope modes
- `[explicit]` — when the set is small and known (≤ ~10 files): list exact file paths
- `[derived]` — when the set is large or dynamic: provide a grep/glob command that resolves the file list at execution time

> Use `[explicit]` or `[derived]`, never both in the same step.
> For bulk steps, scope MUST be `[derived]`, not a file list.

#### Change types
All change types are optional and combinable within a single step.

| Type | When to use | Format |
|------|-------------|--------|
| `[new-file]` | A new file must be created, or an existing file must be completely replaced | Path + full content |
| `[edit]` | Small known set of files (explicit scope) | Before/after snippet per site |
| `[transform]` | Large/dynamic set of files (derived scope) | Generalized find/replace rule |
| `[delete]` | Code or files must be removed | What to remove and where |
| `[command]` | A shell or Maven command must be executed | Full command + expected outcome |

> `[edit]` pairs with explicit scope. `[transform]` pairs with derived scope.
> `[new-file]` replaces the existing file when the file already exists.

### Execution rules
- If anything is unclear or ambiguous at any point, stop and ask the user for clarification before proceeding — never assume or infer intent.
- Execute steps strictly in order. Do not skip or reorder.
- Before starting a phase or step, verify all preconditions are met. Stop and report if any fail.
- If a `[edit]` before-snippet does not match exactly, stop and report — do not attempt a best-guess edit.
- If a `[transform]` rule matches unexpected sites, stop and report before applying.
- When scope is `[derived]`, re-run the derivation command after completing all changes — it must return 0 results.
- After completing a step, verify all acceptance criteria before moving to the next step.
- If an acceptance criteria check fails, stop and report — do not proceed.
- Once all acceptance criteria for a step pass, suggest a commit message for the changes made, then stop and wait for the user to review, approve, and commit manually before proceeding to the next step.

---

## Overview

- **Goal:** Upgrade ASAPP from Spring Boot 3.4.3 to Spring Boot 4.0.x. The upgrade is split into two logical blocks: (1) preparatory JWT library migration from `jjwt` to `nimbus-jose-jwt` on the stable SB3 baseline, followed by (2) automated OpenRewrite migration to SB4 with targeted manual fixes.
- **Phases:**
  - Phase 1 — Migrate jjwt to nimbus-jose-jwt
  - Phase 2 — OpenRewrite: Spring Boot 3 best practices
  - Phase 3 — OpenRewrite: Spring Boot 3.5.x upgrade
  - Phase 4 — OpenRewrite: Spring Boot 4.0.x upgrade
  - Phase 5 — Fix application properties
  - Phase 6 — Verify JdbcConversionsConfiguration
  - Phase 7 — Remove spring-boot-properties-migrator
  - Phase 8 — SB4 Verification
  - Phase 9 — Spring Boot 4 idioms and dependency upgrades
  - Phase 10 — Java 25 LTS
  - Phase 11 — Maven wrapper upgrade
  - Phase 12 — Final verification
  - Phase 13 — Documentation update
- **Total scope:** ~25 files modified
- **Key constraints:**
  - nimbus-jose-jwt is not yet on the classpath (project does not use `spring-security-oauth2-jose`). Must be added explicitly at version `9.37.3`.
  - Nimbus requires an explicit `JWSAlgorithm` — JJWT auto-selected from key bit-length. Replicate with: key ≥512 bits → HS512, ≥384 bits → HS384, else → HS256. The 48-byte production key → HS384; the 32-byte test key → HS256.
  - Nimbus `MACVerifier.verify()` only checks the signature — expiration must be checked explicitly.
  - `JwtVerifier`, `JwtAuthenticationFilter`, and all infrastructure files that do not import `io.jsonwebtoken` are not touched.
  - Root `pom.xml` has no `<build><plugins>` section — only `<build><pluginManagement>`. OpenRewrite requires a temporary `<plugins>` section, added and removed each time.

---

## Plan body

---

# Phase 1 — Migrate jjwt to nimbus-jose-jwt
- **Context:** Replaces the `io.jsonwebtoken:jjwt-*` library with `com.nimbusds:nimbus-jose-jwt` across the authentication service. All production classes, test utilities, and unit tests that import jjwt are rewritten to use the Nimbus API. jjwt is then removed from both POMs. The migration is performed entirely on the stable Spring Boot 3.4.3 baseline — the public API of every modified class is unchanged so all other services and callers are unaffected. Steps are ordered to keep the build green at every commit: add Nimbus first, rewrite production code, rewrite test infrastructure, update tests, then remove jjwt.

## Step 1.1 — Add nimbus-jose-jwt to services/pom.xml
- **Scope:**
  - `[explicit]` `services/pom.xml`
- **Changes:**
  - `[edit]`
    ```xml
    <!-- Before -->
            <!-- * Other Dependencies -->
            <jjwt.version>0.12.6</jjwt.version>
    ```
    ```xml
    <!-- After -->
            <!-- * Other Dependencies -->
            <jjwt.version>0.12.6</jjwt.version>
            <nimbus-jose-jwt.version>9.37.3</nimbus-jose-jwt.version>
    ```
  - `[edit]`
    ```xml
    <!-- Before -->
            <!-- * Other Dependencies -->
            <dependency>
                <groupId>io.jsonwebtoken</groupId>
                <artifactId>jjwt-api</artifactId>
                <version>${jjwt.version}</version>
            </dependency>
    ```
    ```xml
    <!-- After -->
            <!-- * Other Dependencies -->
            <dependency>
                <groupId>com.nimbusds</groupId>
                <artifactId>nimbus-jose-jwt</artifactId>
                <version>${nimbus-jose-jwt.version}</version>
            </dependency>
            <dependency>
                <groupId>io.jsonwebtoken</groupId>
                <artifactId>jjwt-api</artifactId>
                <version>${jjwt.version}</version>
            </dependency>
    ```
- **Acceptance criteria:**
  - `[grep]` `grep -n "nimbus-jose-jwt" services/pom.xml`
  - `[compile]` `mvn compile -pl services`

## Step 1.2 — Add nimbus-jose-jwt to asapp-authentication-service/pom.xml
- **Scope:**
  - `[explicit]` `services/asapp-authentication-service/pom.xml`
- **Changes:**
  - `[edit]`
    ```xml
    <!-- Before -->
            <!-- * Other Dependencies -->
            <dependency>
                <groupId>io.micrometer</groupId>
                <artifactId>micrometer-registry-prometheus</artifactId>
            </dependency>
            <dependency>
                <groupId>io.jsonwebtoken</groupId>
                <artifactId>jjwt-api</artifactId>
            </dependency>
    ```
    ```xml
    <!-- After -->
            <!-- * Other Dependencies -->
            <dependency>
                <groupId>com.nimbusds</groupId>
                <artifactId>nimbus-jose-jwt</artifactId>
            </dependency>
            <dependency>
                <groupId>io.micrometer</groupId>
                <artifactId>micrometer-registry-prometheus</artifactId>
            </dependency>
            <dependency>
                <groupId>io.jsonwebtoken</groupId>
                <artifactId>jjwt-api</artifactId>
            </dependency>
    ```
- **Acceptance criteria:**
  - `[grep]` `grep -n "nimbus-jose-jwt" services/asapp-authentication-service/pom.xml`
  - `[compile]` `mvn compile -pl services/asapp-authentication-service`

## Step 1.3 — Rewrite JwtDecoder
- **Scope:**
  - `[explicit]` `services/asapp-authentication-service/src/main/java/com/bcn/asapp/authentication/infrastructure/security/JwtDecoder.java`
- **Changes:**
  - `[new-file]` `services/asapp-authentication-service/src/main/java/com/bcn/asapp/authentication/infrastructure/security/JwtDecoder.java`
    Content: see `docs/snippets/phase1-step1.3-JwtDecoder.java`
- **Notes:**
  - `MACVerifier` handles any HMAC algorithm declared in the JWT header — no algorithm selection needed in the decoder.
  - `MACVerifier.verify()` returns `false` on signature mismatch (does not throw). A `JOSEException` is thrown explicitly to trigger the `RuntimeException` wrapper.
  - `getExpirationTime().before(new Date())` replicates JJWT's automatic expiry check.
  - `JwtVerifier.verifyAccessToken` / `verifyRefreshToken` catch `Exception` generically — any `RuntimeException` from this class continues to be wrapped in `InvalidJwtException` as before.
- **Acceptance criteria:**
  - `[no-match]` `grep -r "io.jsonwebtoken" services/asapp-authentication-service/src/main/java/com/bcn/asapp/authentication/infrastructure/security/JwtDecoder.java`
  - `[grep]` `grep -n "SignedJWT" services/asapp-authentication-service/src/main/java/com/bcn/asapp/authentication/infrastructure/security/JwtDecoder.java`
  - `[compile]` `mvn compile -pl services/asapp-authentication-service`

## Step 1.4 — Rewrite JwtIssuer
- **Scope:**
  - `[explicit]` `services/asapp-authentication-service/src/main/java/com/bcn/asapp/authentication/infrastructure/security/JwtIssuer.java`
- **Changes:**
  - `[new-file]` `services/asapp-authentication-service/src/main/java/com/bcn/asapp/authentication/infrastructure/security/JwtIssuer.java`
    Content: see `docs/snippets/phase1-step1.4-JwtIssuer.java`
- **Notes:**
  - `JWTClaimsSet.Builder.claim(String, Object)` adds individual claims — there is no bulk `Map` overload in Nimbus. Use `Map.forEach` to add all entries from `JwtClaims.value()`.
  - `signedJwt.serialize()` returns the compact JWT string (`header.payload.signature`), equivalent to JJWT's `.compact()`.
- **Acceptance criteria:**
  - `[no-match]` `grep -r "io.jsonwebtoken" services/asapp-authentication-service/src/main/java/com/bcn/asapp/authentication/infrastructure/security/JwtIssuer.java`
  - `[grep]` `grep -n "MACSigner" services/asapp-authentication-service/src/main/java/com/bcn/asapp/authentication/infrastructure/security/JwtIssuer.java`
  - `[compile]` `mvn compile -pl services/asapp-authentication-service`

## Step 1.5 — Rewrite EncodedTokenFactory
- **Scope:**
  - `[explicit]` `services/asapp-authentication-service/src/test/java/com/bcn/asapp/authentication/testutil/fixture/EncodedTokenFactory.java`
- **Changes:**
  - `[new-file]` `services/asapp-authentication-service/src/test/java/com/bcn/asapp/authentication/testutil/fixture/EncodedTokenFactory.java`
    Content: see `docs/snippets/phase1-step1.5-EncodedTokenFactory.java`
- **Notes:**
  - `withSecretKey(SecretKey)` retains its parameter type (`javax.crypto.SecretKey` is standard Java, not jjwt). The implementation changes to extract the raw bytes via `secretKey.getEncoded()`.
  - The `selectAlgorithm` helper mirrors the one in `JwtIssuer` so tokens built by this factory use the same algorithm the real issuer would select for the same key length.
  - The `signed` field and `notSigned()` builder method are removed — Nimbus does not support unsigned JWT construction in this builder, and the method has no test usages.
- **Acceptance criteria:**
  - `[no-match]` `grep -n "io.jsonwebtoken" services/asapp-authentication-service/src/test/java/com/bcn/asapp/authentication/testutil/fixture/EncodedTokenFactory.java`
  - `[grep]` `grep -n "MACSigner" services/asapp-authentication-service/src/test/java/com/bcn/asapp/authentication/testutil/fixture/EncodedTokenFactory.java`
  - `[compile]` `mvn test-compile -pl services/asapp-authentication-service`

## Step 1.6 — Rewrite JwtAssertions
- **Scope:**
  - `[explicit]` `services/asapp-authentication-service/src/test/java/com/bcn/asapp/authentication/testutil/JwtAssertions.java`
- **Changes:**
  - `[new-file]` `services/asapp-authentication-service/src/test/java/com/bcn/asapp/authentication/testutil/JwtAssertions.java`
    Content: see `docs/snippets/phase1-step1.6-JwtAssertions.java`
- **Notes:**
  - The generic type changes from `AbstractAssert<JwtAssertions, Jws<Claims>>` to `AbstractAssert<JwtAssertions, SignedJWT>`.
  - `getJWTClaimsSet()` throws checked `ParseException` — each method catches it and rethrows as `AssertionError` to avoid polluting the fluent assertion API with checked exceptions.
  - The private `hasHeader()` and `hasPayload()` helpers are removed — they were intermediate null checks not relevant to the fluent API surface.
- **Acceptance criteria:**
  - `[no-match]` `grep -n "io.jsonwebtoken" services/asapp-authentication-service/src/test/java/com/bcn/asapp/authentication/testutil/JwtAssertions.java`
  - `[grep]` `grep -n "SignedJWT" services/asapp-authentication-service/src/test/java/com/bcn/asapp/authentication/testutil/JwtAssertions.java`
  - `[compile]` `mvn test-compile -pl services/asapp-authentication-service`

## Step 1.7 — Update JwtDecoderTests
- **Scope:**
  - `[explicit]` `services/asapp-authentication-service/src/test/java/com/bcn/asapp/authentication/infrastructure/security/JwtDecoderTests.java`
- **Changes:**
  - `[edit]` — Replace the import block (remove jjwt, add SecretKeySpec)
    ```java
    // Before
    import java.util.Base64;
    import javax.crypto.SecretKey;

    import org.junit.jupiter.api.BeforeEach;
    import org.junit.jupiter.api.Nested;
    import org.junit.jupiter.api.Test;

    import io.jsonwebtoken.ExpiredJwtException;
    import io.jsonwebtoken.MalformedJwtException;
    import io.jsonwebtoken.security.Keys;
    import io.jsonwebtoken.security.SignatureException;
    ```
    ```java
    // After
    import java.util.Base64;
    import javax.crypto.SecretKey;
    import javax.crypto.spec.SecretKeySpec;

    import org.junit.jupiter.api.BeforeEach;
    import org.junit.jupiter.api.Nested;
    import org.junit.jupiter.api.Test;
    ```
  - `[edit]` — Replace the secretKey field declaration
    ```java
    // Before
        private final SecretKey secretKey = Keys.hmacShaKeyFor(new byte[32]);
    ```
    ```java
    // After
        private final SecretKey secretKey = new SecretKeySpec(new byte[32], "HmacSHA256");
    ```
  - `[edit]` — Update malformed token test assertion
    ```java
    // Before
            assertThat(actual).isInstanceOf(MalformedJwtException.class)
                              .hasMessageContaining("JWT");
    ```
    ```java
    // After
            assertThat(actual).isInstanceOf(RuntimeException.class)
                              .cause()
                              .isNotNull();
    ```
  - `[edit]` — Update invalid signature test — key creation
    ```java
    // Before
            var secretKey = Keys.hmacShaKeyFor("invalid-secret-key-with-at-least-32-bytes".getBytes());
    ```
    ```java
    // After
            var secretKey = new SecretKeySpec("invalid-secret-key-with-at-least-32-bytes".getBytes(), "HmacSHA256");
    ```
  - `[edit]` — Update invalid signature test assertion
    ```java
    // Before
            assertThat(actual).isInstanceOf(SignatureException.class)
                              .hasMessageContaining("signature");
    ```
    ```java
    // After
            assertThat(actual).isInstanceOf(RuntimeException.class)
                              .hasMessageContaining("signature");
    ```
  - `[edit]` — Update expired token test assertion
    ```java
    // Before
            assertThat(actual).isInstanceOf(ExpiredJwtException.class)
                              .hasMessageContaining("expired");
    ```
    ```java
    // After
            assertThat(actual).isInstanceOf(RuntimeException.class)
                              .hasMessageContaining("expired");
    ```
- **Acceptance criteria:**
  - `[no-match]` `grep -n "io.jsonwebtoken" services/asapp-authentication-service/src/test/java/com/bcn/asapp/authentication/infrastructure/security/JwtDecoderTests.java`
  - `[test]` `mvn test -pl services/asapp-authentication-service -Dtest=JwtDecoderTests`

## Step 1.8 — Update JwtIssuerTests
- **Scope:**
  - `[explicit]` `services/asapp-authentication-service/src/test/java/com/bcn/asapp/authentication/infrastructure/security/JwtIssuerTests.java`
- **Changes:**
  - `[edit]` — Remove jjwt import
    ```java
    // Before
    import io.jsonwebtoken.security.Keys;

    import com.bcn.asapp.authentication.domain.authentication.JwtClaims;
    ```
    ```java
    // After
    import com.bcn.asapp.authentication.domain.authentication.JwtClaims;
    ```
  - `[edit]` — Simplify key creation in @BeforeEach
    ```java
    // Before
            var secretKey = Keys.hmacShaKeyFor(new byte[32]);
            var jwtSecret = Base64.getEncoder()
                                  .encodeToString(secretKey.getEncoded());
    ```
    ```java
    // After
            var jwtSecret = Base64.getEncoder()
                                  .encodeToString(new byte[32]);
    ```
- **Acceptance criteria:**
  - `[no-match]` `grep -n "io.jsonwebtoken" services/asapp-authentication-service/src/test/java/com/bcn/asapp/authentication/infrastructure/security/JwtIssuerTests.java`
  - `[test]` `mvn test -pl services/asapp-authentication-service -Dtest=JwtIssuerTests`

## Step 1.9 — Remove jjwt from asapp-authentication-service/pom.xml
- **Scope:**
  - `[explicit]` `services/asapp-authentication-service/pom.xml`
- **Changes:**
  - `[delete]` — Remove the `jjwt-api` compile dependency entry:
    ```xml
            <dependency>
                <groupId>io.jsonwebtoken</groupId>
                <artifactId>jjwt-api</artifactId>
            </dependency>
    ```
  - `[delete]` — Remove the `jjwt-impl` runtime dependency entry:
    ```xml
            <dependency>
                <groupId>io.jsonwebtoken</groupId>
                <artifactId>jjwt-impl</artifactId>
                <scope>runtime</scope>
            </dependency>
    ```
  - `[delete]` — Remove the `jjwt-jackson` runtime dependency entry:
    ```xml
            <dependency>
                <groupId>io.jsonwebtoken</groupId>
                <artifactId>jjwt-jackson</artifactId>
                <scope>runtime</scope>
            </dependency>
    ```
- **Notes:**
  - Step 1.9 removes the usage declarations in the service POM; step 1.10 removes the version management and BOM entries in the parent services POM.
- **Acceptance criteria:**
  - `[no-match]` `grep -n "jjwt" services/asapp-authentication-service/pom.xml`
  - `[compile]` `mvn compile -pl services/asapp-authentication-service`

## Step 1.10 — Remove jjwt from services/pom.xml
- **Scope:**
  - `[explicit]` `services/pom.xml`
- **Changes:**
  - `[delete]` — Remove the `jjwt.version` property:
    ```xml
            <jjwt.version>0.12.6</jjwt.version>
    ```
  - `[delete]` — Remove the `jjwt-api` dependencyManagement entry:
    ```xml
            <dependency>
                <groupId>io.jsonwebtoken</groupId>
                <artifactId>jjwt-api</artifactId>
                <version>${jjwt.version}</version>
            </dependency>
    ```
  - `[delete]` — Remove the `jjwt-impl` dependencyManagement entry:
    ```xml
            <dependency>
                <groupId>io.jsonwebtoken</groupId>
                <artifactId>jjwt-impl</artifactId>
                <version>${jjwt.version}</version>
                <scope>runtime</scope>
            </dependency>
    ```
  - `[delete]` — Remove the `jjwt-jackson` dependencyManagement entry:
    ```xml
            <dependency>
                <groupId>io.jsonwebtoken</groupId>
                <artifactId>jjwt-jackson</artifactId>
                <version>${jjwt.version}</version>
                <scope>runtime</scope>
            </dependency>
    ```
- **Acceptance criteria:**
  - `[no-match]` `grep -n "jjwt" services/pom.xml`
  - `[compile]` `mvn compile -pl services`

## Step 1.11 — Run authentication-service tests
- **Changes:**
  - `[command]`
    ```bash
    mvn clean verify -pl services/asapp-authentication-service
    ```
    Expected outcome: BUILD SUCCESS, all unit and integration tests green.
- **Acceptance criteria:**
  - `[test]` `mvn clean verify -pl services/asapp-authentication-service`

## Step 1.12 — Run full test suite
- **Changes:**
  - `[command]`
    ```bash
    mvn clean verify
    ```
    Expected outcome: BUILD SUCCESS across all modules.
- **Acceptance criteria:**
  - `[test]` `mvn clean verify`

---

# Phase 2 — OpenRewrite: Spring Boot 3 best practices
- **Context:** Applies the `SpringBoot3BestPracticesOnly` OpenRewrite recipe to clean up existing patterns before the upgrade. Also adds `spring-boot-properties-migrator` which will emit `WARN` logs at runtime whenever a property key is renamed — useful throughout Phases 3 and 4. The OpenRewrite plugin is added temporarily to the root `pom.xml` `<build>` section and removed after.
- **Preconditions:**
  - `[phase]` Phase 1 complete

## Step 2.1 — Add spring-boot-properties-migrator to services/pom.xml
- **Scope:**
  - `[explicit]` `services/pom.xml`
- **Changes:**
  - `[edit]`
    ```xml
    <!-- Before -->
            <!-- Runtime Dependencies -->
            <!-- * Other Dependencies -->
    ```
    ```xml
    <!-- After -->
            <!-- Runtime Dependencies -->
            <!-- * Spring Dependencies -->
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-properties-migrator</artifactId>
                <scope>runtime</scope>
            </dependency>
            <!-- * Other Dependencies -->
    ```
- **Notes:**
  - If a `<!-- Runtime Dependencies -->` section does not already exist in `services/pom.xml`, add it immediately after the `<!-- * Other Dependencies -->` closing comment in the compile section.
- **Acceptance criteria:**
  - `[grep]` `grep -n "spring-boot-properties-migrator" services/pom.xml`

## Step 2.2 — Add OpenRewrite plugin to root pom.xml
- **Scope:**
  - `[explicit]` `pom.xml`
- **Changes:**
  - `[edit]`
    ```xml
    <!-- Before -->
        <build>
            <pluginManagement>
    ```
    ```xml
    <!-- After -->
        <build>
            <plugins>
                <!-- Other Plugins (temporary — remove after OpenRewrite run) -->
                <plugin>
                    <groupId>org.openrewrite.maven</groupId>
                    <artifactId>rewrite-maven-plugin</artifactId>
                    <version>6.35.0</version>
                    <configuration>
                        <activeRecipes>
                            <recipe>org.openrewrite.java.spring.boot3.SpringBoot3BestPracticesOnly</recipe>
                        </activeRecipes>
                    </configuration>
                    <dependencies>
                        <dependency>
                            <groupId>org.openrewrite.recipe</groupId>
                            <artifactId>rewrite-spring</artifactId>
                            <version>6.28.2</version>
                        </dependency>
                    </dependencies>
                </plugin>
            </plugins>
            <pluginManagement>
    ```
- **Acceptance criteria:**
  - `[grep]` `grep -n "rewrite-maven-plugin" pom.xml`

## Step 2.3 — Run OpenRewrite dry-run
- **Changes:**
  - `[command]`
    ```bash
    mvn rewrite:dryRun
    ```
    Expected outcome: Generates a diff report in `target/rewrite/rewrite.patch`. Review the report — if unexpected files are modified, stop and report to the user before proceeding.
- **Acceptance criteria:**
  - `[manual]` Review `target/rewrite/rewrite.patch`. Verify all of the following:
    1. `io.jsonwebtoken` does **not** appear anywhere in the patch — the jjwt migration is already done and must not be touched
    2. `JwtDecoder.java`, `JwtIssuer.java`, `EncodedTokenFactory.java`, `JwtAssertions.java` are **not** in the patch
    3. Modified files are limited to formatting, annotation style, or property cleanups (e.g. removal of unnecessary `@Autowired` on constructors) — no application logic changes
    4. Report any modification outside these categories before proceeding

## Step 2.4 — Apply OpenRewrite
- **Changes:**
  - `[command]`
    ```bash
    mvn rewrite:run
    ```
    Expected outcome: Source files modified in-place.
- **Acceptance criteria:**
  - `[compile]` `mvn compile`

## Step 2.5 — Remove OpenRewrite plugin from root pom.xml
- **Scope:**
  - `[explicit]` `pom.xml`
- **Changes:**
  - `[edit]`
    ```xml
    <!-- Before -->
        <build>
            <plugins>
                <!-- Other Plugins (temporary — remove after OpenRewrite run) -->
                <plugin>
                    <groupId>org.openrewrite.maven</groupId>
                    <artifactId>rewrite-maven-plugin</artifactId>
                    <version>6.35.0</version>
                    <configuration>
                        <activeRecipes>
                            <recipe>org.openrewrite.java.spring.boot3.SpringBoot3BestPracticesOnly</recipe>
                        </activeRecipes>
                    </configuration>
                    <dependencies>
                        <dependency>
                            <groupId>org.openrewrite.recipe</groupId>
                            <artifactId>rewrite-spring</artifactId>
                            <version>6.28.2</version>
                        </dependency>
                    </dependencies>
                </plugin>
            </plugins>
            <pluginManagement>
    ```
    ```xml
    <!-- After -->
        <build>
            <pluginManagement>
    ```
- **Acceptance criteria:**
  - `[no-match]` `grep -n "rewrite-maven-plugin" pom.xml`

## Step 2.6 — Build verification
- **Changes:**
  - `[command]`
    ```bash
    mvn clean verify
    ```
    Expected outcome: BUILD SUCCESS.
- **Acceptance criteria:**
  - `[test]` `mvn clean verify`

---

# Phase 3 — OpenRewrite: Spring Boot 3.5.x upgrade
- **Context:** Upgrades the project to Spring Boot 3.5.x. This surfaces all deprecations that will become removals in SB4. After OpenRewrite runs, start each service to capture `WARN` lines from `spring-boot-properties-migrator` — these identify property keys that will be renamed in SB4.
- **Preconditions:**
  - `[phase]` Phase 2 complete

## Step 3.1 — Add OpenRewrite plugin to root pom.xml

- **Scope:**
  - `[explicit]` `pom.xml`
- **Changes:**
  - `[edit]`
    ```xml
    <!-- Before -->
        <build>
            <pluginManagement>
    ```
    ```xml
    <!-- After -->
        <build>
            <plugins>
                <!-- Other Plugins (temporary — remove after OpenRewrite run) -->
                <plugin>
                    <groupId>org.openrewrite.maven</groupId>
                    <artifactId>rewrite-maven-plugin</artifactId>
                    <version>6.35.0</version>
                    <configuration>
                        <activeRecipes>
                            <recipe>org.openrewrite.java.spring.boot3.UpgradeSpringBoot_3_5</recipe>
                        </activeRecipes>
                    </configuration>
                    <dependencies>
                        <dependency>
                            <groupId>org.openrewrite.recipe</groupId>
                            <artifactId>rewrite-spring</artifactId>
                            <version>6.28.2</version>
                        </dependency>
                    </dependencies>
                </plugin>
            </plugins>
            <pluginManagement>
    ```
- **Acceptance criteria:**
  - `[grep]` `grep -n "UpgradeSpringBoot_3_5" pom.xml`

## Step 3.2 — Run OpenRewrite dry-run
- **Changes:**
  - `[command]`
    ```bash
    mvn rewrite:dryRun
    ```
    Expected outcome: Diff report in `target/rewrite/rewrite.patch`. Review before applying.
- **Acceptance criteria:**
  - `[manual]` Review `target/rewrite/rewrite.patch`. Verify all of the following:
    1. `pom.xml` shows `spring-boot-starter-parent` version changing from `3.4.3` to `3.5.x`
    2. No application logic files (`.java`) are modified — this recipe only bumps versions and applies 3.5 preparation changes
    3. Report any modifications outside POM version bumps before proceeding

## Step 3.3 — Apply OpenRewrite
- **Changes:**
  - `[command]`
    ```bash
    mvn rewrite:run
    ```
    Expected outcome: Source files modified in-place. Spring Boot parent version bumped to 3.5.x.
- **Acceptance criteria:**
  - `[compile]` `mvn compile`
  - `[no-match]` `grep -n "3.4.3" pom.xml`
  - `[grep]` `grep -n "3\.5\." pom.xml`

## Step 3.4 — Remove OpenRewrite plugin from root pom.xml
- **Scope:**
  - `[explicit]` `pom.xml`
- **Changes:**
  - `[edit]`
    ```xml
    <!-- Before -->
        <build>
            <plugins>
                <!-- Other Plugins (temporary — remove after OpenRewrite run) -->
                <plugin>
                    <groupId>org.openrewrite.maven</groupId>
                    <artifactId>rewrite-maven-plugin</artifactId>
                    <version>6.35.0</version>
                    <configuration>
                        <activeRecipes>
                            <recipe>org.openrewrite.java.spring.boot3.UpgradeSpringBoot_3_5</recipe>
                        </activeRecipes>
                    </configuration>
                    <dependencies>
                        <dependency>
                            <groupId>org.openrewrite.recipe</groupId>
                            <artifactId>rewrite-spring</artifactId>
                            <version>6.28.2</version>
                        </dependency>
                    </dependencies>
                </plugin>
            </plugins>
            <pluginManagement>
    ```
    ```xml
    <!-- After -->
        <build>
            <pluginManagement>
    ```
- **Acceptance criteria:**
  - `[no-match]` `grep -n "rewrite-maven-plugin" pom.xml`

## Step 3.5 — Build verification
- **Changes:**
  - `[command]`
    ```bash
    mvn clean verify
    ```
    Expected outcome: BUILD SUCCESS. Resolve any deprecation warnings emitted by SB 3.5.x.
- **Acceptance criteria:**
  - `[test]` `mvn clean verify`

## Step 3.6 — Capture property migration warnings
- **Changes:** (none — read-only observation step)
- **Notes:**
  - Two warnings are expected at this point (SB 3.5.x baseline, pre-SB4 migration):
    1. `spring.jackson.default-property-inclusion` — will be renamed in SB4; fixed manually in Step 5.1
    2. `management.endpoint.health.probes.enabled` — becomes the default in SB4; removed in Step 5.2
  - Any warning **beyond these two** is unexpected. Stop and report it to the user before proceeding to Phase 4 — it will need an additional fix step in Phase 5.
- **Acceptance criteria:**
  - `[manual]` For each service, run:
    ```bash
    cd services/asapp-authentication-service && mvn spring-boot:run
    cd services/asapp-tasks-service && mvn spring-boot:run
    cd services/asapp-users-service && mvn spring-boot:run
    ```
    Look for log lines containing `WARN` from `spring-boot-properties-migrator`. Compare against the two expected warnings above. If any additional warnings appear, stop and report them to the user before proceeding. If only the expected warnings appear (or none), confirm and continue.

---

# Phase 4 — OpenRewrite: Spring Boot 4.0.x upgrade
- **Context:** The main automated upgrade step. Runs three recipes in one pass: `UpgradeSpringBoot_4_0` (composite — handles starters, properties, Jackson 3, Spring Security 7, Spring Data, test annotations), `UpgradeSpringDoc_3_0`, and `Testcontainers2Migration`. These are not bundled together by default and must be listed explicitly.
- **Preconditions:**
  - `[phase]` Phase 3 complete

## Step 4.1 — Add OpenRewrite plugin to root pom.xml
- **Scope:**
  - `[explicit]` `pom.xml`
- **Changes:**
  - `[edit]`
    ```xml
    <!-- Before -->
        <build>
            <pluginManagement>
    ```
    ```xml
    <!-- After -->
        <build>
            <plugins>
                <!-- Other Plugins (temporary — remove after OpenRewrite run) -->
                <plugin>
                    <groupId>org.openrewrite.maven</groupId>
                    <artifactId>rewrite-maven-plugin</artifactId>
                    <version>6.35.0</version>
                    <configuration>
                        <activeRecipes>
                            <!-- Composite: Spring Boot, Framework, Security, Data, Jackson, starters, properties, test annotations -->
                            <recipe>org.openrewrite.java.spring.boot4.UpgradeSpringBoot_4_0</recipe>
                            <!-- Not bundled in the composite above — add separately -->
                            <recipe>org.openrewrite.java.springdoc.UpgradeSpringDoc_3_0</recipe>
                            <recipe>org.openrewrite.java.testing.testcontainers.Testcontainers2Migration</recipe>
                        </activeRecipes>
                    </configuration>
                    <dependencies>
                        <dependency>
                            <groupId>org.openrewrite.recipe</groupId>
                            <artifactId>rewrite-spring</artifactId>
                            <version>6.28.2</version>
                        </dependency>
                        <dependency>
                            <!-- Required for Testcontainers2Migration -->
                            <groupId>org.openrewrite.recipe</groupId>
                            <artifactId>rewrite-testing-frameworks</artifactId>
                            <version>2.28.2</version>
                        </dependency>
                    </dependencies>
                </plugin>
            </plugins>
            <pluginManagement>
    ```
- **Acceptance criteria:**
  - `[grep]` `grep -n "UpgradeSpringBoot_4_0" pom.xml`

## Step 4.2 — Run OpenRewrite dry-run
- **Changes:**
  - `[command]`
    ```bash
    mvn rewrite:dryRun
    ```
    Expected outcome: Diff report in `target/rewrite/rewrite.patch`.
- **Acceptance criteria:**
  - `[manual]` Review the patch carefully. Verify that: Spring Boot parent is bumped to 4.0.x; `spring-boot-starter-web` is renamed to `spring-boot-starter-webmvc`; `liquibase-core` is replaced by `spring-boot-starter-liquibase`; `@MockBean`/`@SpyBean` annotations are replaced; Jackson imports are updated to `tools.jackson.*`. Report any unexpected changes before proceeding.

## Step 4.3 — Apply OpenRewrite
- **Changes:**
  - `[command]`
    ```bash
    mvn rewrite:run
    ```
    Expected outcome: Source files modified in-place.
- **Acceptance criteria:**
  - `[compile]` `mvn compile`
  - `[no-match]` `grep -rn "spring-boot-starter-web</artifactId>" services/ --include="pom.xml"`
  - `[grep]` `grep -rn "spring-boot-starter-webmvc" services/ --include="pom.xml"`
  - `[no-match]` `grep -rn "liquibase-core</artifactId>" services/ --include="pom.xml"`
  - `[grep]` `grep -rn "spring-boot-starter-liquibase" services/ --include="pom.xml"`
  - `[no-match]` `grep -rn "import org.springframework.boot.test.mock.mockito.MockBean" services/ --include="*.java"`
  - `[grep]` `grep -rn "import org.mockito.junit.jupiter.MockitoExtension\|MockitoBean" services/ --include="*.java"`

## Step 4.4 — Remove OpenRewrite plugin from root pom.xml
- **Scope:**
  - `[explicit]` `pom.xml`
- **Changes:**
  - `[edit]`
    ```xml
    <!-- Before -->
        <build>
            <plugins>
                <!-- Other Plugins (temporary — remove after OpenRewrite run) -->
                <plugin>
                    <groupId>org.openrewrite.maven</groupId>
                    <artifactId>rewrite-maven-plugin</artifactId>
                    <version>6.35.0</version>
                    <configuration>
                        <activeRecipes>
                            <!-- Composite: Spring Boot, Framework, Security, Data, Jackson, starters, properties, test annotations -->
                            <recipe>org.openrewrite.java.spring.boot4.UpgradeSpringBoot_4_0</recipe>
                            <!-- Not bundled in the composite above — add separately -->
                            <recipe>org.openrewrite.java.springdoc.UpgradeSpringDoc_3_0</recipe>
                            <recipe>org.openrewrite.java.testing.testcontainers.Testcontainers2Migration</recipe>
                        </activeRecipes>
                    </configuration>
                    <dependencies>
                        <dependency>
                            <groupId>org.openrewrite.recipe</groupId>
                            <artifactId>rewrite-spring</artifactId>
                            <version>6.28.2</version>
                        </dependency>
                        <dependency>
                            <!-- Required for Testcontainers2Migration -->
                            <groupId>org.openrewrite.recipe</groupId>
                            <artifactId>rewrite-testing-frameworks</artifactId>
                            <version>2.28.2</version>
                        </dependency>
                    </dependencies>
                </plugin>
            </plugins>
            <pluginManagement>
    ```
    ```xml
    <!-- After -->
        <build>
            <pluginManagement>
    ```
- **Acceptance criteria:**
  - `[no-match]` `grep -n "rewrite-maven-plugin" pom.xml`

## Step 4.5 — Build verification
- **Changes:**
  - `[command]`
    ```bash
    mvn clean verify
    ```
    Expected outcome: BUILD SUCCESS. If compilation fails, report the errors — do not attempt fixes without reporting first.
- **Acceptance criteria:**
  - `[test]` `mvn clean verify`

---

# Phase 5 — Fix application properties
- **Context:** Two manual property changes are required in all three services — these are not handled by OpenRewrite. The Jackson property rename (`spring.jackson.default-property-inclusion` → `spring.jackson.json.write.default-property-inclusion`) follows Jackson 3's new naming scheme. The health probes property is now the SB4 default and must be removed to avoid noise.
- **Preconditions:**
  - `[phase]` Phase 4 complete

## Step 5.1 — Rename Jackson property
- **Scope:**
  - `[derived]` `grep -rl "spring.jackson.default-property-inclusion" services/ --include="*.properties"`
- **Changes:**
  - `[transform]`
    ```
    Find:    spring.jackson.default-property-inclusion=NON_NULL
    Replace: spring.jackson.json.write.default-property-inclusion=NON_NULL
    ```
- **Acceptance criteria:**
  - `[no-match]` `grep -r "spring.jackson.default-property-inclusion" services/ --include="*.properties"`
  - `[grep]` `grep -r "spring.jackson.json.write.default-property-inclusion" services/ --include="*.properties"`

## Step 5.2 — Remove health probes property
- **Scope:**
  - `[derived]` `grep -rl "management.endpoint.health.probes.enabled=true" services/ --include="*.properties"`
- **Changes:**
  - `[delete]` — Remove the entire line `management.endpoint.health.probes.enabled=true` from each matching file.
- **Acceptance criteria:**
  - `[no-match]` `grep -r "management.endpoint.health.probes.enabled=true" services/ --include="*.properties"`

## Step 5.3 — Build verification
- **Changes:**
  - `[command]`
    ```bash
    mvn clean verify
    ```
    Expected outcome: BUILD SUCCESS.
- **Acceptance criteria:**
  - `[test]` `mvn clean verify`

## Step 5.4 — Verify no remaining property migration warnings
- **Changes:** (none — read-only verification step)
- **Notes:**
  - This step closes the loop opened by Step 3.6. The migrator must be silent before it is removed in Phase 7.
  - If a warning persists, the corresponding property fix was missed. Stop and resolve it before proceeding.
- **Acceptance criteria:**
  - `[manual]` For each service, run:
    ```bash
    cd services/asapp-authentication-service && mvn spring-boot:run
    cd services/asapp-tasks-service && mvn spring-boot:run
    cd services/asapp-users-service && mvn spring-boot:run
    ```
    Confirm that **no** `WARN` lines from `spring-boot-properties-migrator` appear in any service log. If any warning remains, stop and report it to the user before proceeding.

---

# Phase 6 — Verify JdbcConversionsConfiguration
- **Context:** OpenRewrite's `UpgradeJackson_2_3` recipe (included in `UpgradeSpringBoot_4_0`) should have automatically migrated `JdbcConversionsConfiguration.java` from `com.fasterxml.jackson` to `tools.jackson`. This phase confirms the migration is correct and that the PostgreSQL JSONB ↔ JWT claims conversion still works end-to-end.
- **Preconditions:**
  - `[phase]` Phase 4 complete

## Step 6.1 — Verify JdbcConversionsConfiguration
- **Changes:** (none initially — verification step only)
- **Notes:**
  - Spring Boot 4 auto-configures a `JsonMapper` bean (not `ObjectMapper`). The constructor injection must accept `JsonMapper` (or `ObjectMapper` if it still resolves via inheritance — confirm by running the integration tests). If the class does not compile or the integration tests fail, report the exact error.
- **Acceptance criteria:**
  - `[grep]` `grep -n "tools.jackson" services/asapp-authentication-service/src/main/java/com/bcn/asapp/authentication/infrastructure/config/JdbcConversionsConfiguration.java`
  - `[no-match]` `grep -n "com.fasterxml.jackson" services/asapp-authentication-service/src/main/java/com/bcn/asapp/authentication/infrastructure/config/JdbcConversionsConfiguration.java`
  - `[test]` `mvn clean verify -pl services/asapp-authentication-service`
  - `[manual]` If any of the above fail, read the current content of `JdbcConversionsConfiguration.java`, report the exact issue to the user, and wait for instructions before making any changes.

---

# Phase 7 — Remove spring-boot-properties-migrator
- **Context:** `spring-boot-properties-migrator` was added in Phase 2 to surface property renames at runtime. All identified renames have been fixed (Phase 5 plus any additional ones captured in Phase 3.6). Remove it now to avoid it printing stale warnings in production.
- **Preconditions:**
  - `[phase]` Phase 5 complete
  - `[phase]` Phase 6 complete

## Step 7.1 — Remove spring-boot-properties-migrator from services/pom.xml
- **Scope:**
  - `[explicit]` `services/pom.xml`
- **Changes:**
  - `[delete]` — Remove the runtime dependency block:
    ```xml
            <!-- Runtime Dependencies -->
            <!-- * Spring Dependencies -->
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-properties-migrator</artifactId>
                <scope>runtime</scope>
            </dependency>
    ```
- **Notes:**
  - Also remove the `<!-- * Spring Dependencies -->` comment if no other Spring runtime deps remain in that group. Also remove the `<!-- Runtime Dependencies -->` section header if the section becomes empty.
- **Acceptance criteria:**
  - `[no-match]` `grep -n "spring-boot-properties-migrator" services/pom.xml`

## Step 7.2 — Build verification
- **Changes:**
  - `[command]`
    ```bash
    mvn clean verify
    ```
    Expected outcome: BUILD SUCCESS.
- **Acceptance criteria:**
  - `[test]` `mvn clean verify`

---

# Phase 8 — SB4 Verification
- **Context:** Build and test verification confirming the complete Spring Boot 4 upgrade is stable. Phases 9, 10, and 11 all require this phase to be green before proceeding.
- **Preconditions:**
  - `[phase]` Phase 7 complete

## Step 8.1 — Full build
- **Changes:**
  - `[command]`
    ```bash
    mvn clean install
    ```
    Expected outcome: BUILD SUCCESS across all modules including libs.
- **Acceptance criteria:**
  - `[compile]` `mvn clean install`

## Step 8.2 — Full test suite
- **Changes:**
  - `[command]`
    ```bash
    mvn clean verify
    ```
    Expected outcome: BUILD SUCCESS, all unit and integration tests green.
- **Acceptance criteria:**
  - `[test]` `mvn clean verify`

---

# Phase 9 — Spring Boot 4 idioms and dependency upgrades
- **Context:** Post-upgrade improvements applied on the stable Spring Boot 4.0.x baseline. Structured logging (ECS format) is enabled to improve observability. SecurityConfiguration requires no changes — all three services already use string-based `requestMatchers()` with no direct `AntPathRequestMatcher` instantiation, which continues to work in Spring Security 7. Spring Data JDBC and Springdoc require no code changes either. Dependency and plugin bumps that were intentionally held back during the migration to reduce diff noise are applied here.
- **Preconditions:**
  - `[phase]` Phase 8 complete

## Step 9.1 — Enable structured logging in all 3 services
- **Scope:**
  - `[derived]` `find services -path "*/main/resources/application.properties"`
- **Changes:**
  - `[transform]` — In each matched file, append `logging.structured.format.console=ecs` immediately after the last `logging.level.*` line in the `# Logger properties` block.
    ```properties
    # Before (example from asapp-authentication-service):
    # Logger properties
    logging.level.org.springframework=INFO
    logging.level.org.springframework.jdbc.core=DEBUG
    logging.level.com.bcn.asapp.authentication=INFO

    # After:
    # Logger properties
    logging.level.org.springframework=INFO
    logging.level.org.springframework.jdbc.core=DEBUG
    logging.level.com.bcn.asapp.authentication=INFO
    logging.structured.format.console=ecs
    ```
- **Notes:**
  - Apply the same pattern to `asapp-tasks-service` and `asapp-users-service` (replacing the service-specific logger line).
  - `logging.structured.format.console=ecs` outputs JSON-formatted logs (Elastic Common Schema) to stdout — no extra dependency required, built-in since Spring Boot 3.4.
  - Add only to main `application.properties`, not test properties — JSON format makes test output harder to read.
  - The property is placed after the existing `# Logger properties` block.
- **Acceptance criteria:**
  - `[grep]` `grep -rn "logging.structured.format.console" services/*/src/main/resources/`
  - `[no-match]` `grep -rn "logging.structured.format.console" services/*/src/test/resources/`

## Step 9.2 — Upgrade BouncyCastle to 1.83
- **Scope:**
  - `[explicit]` `services/asapp-authentication-service/pom.xml`
- **Changes:**
  - `[edit]`
    ```xml
    <!-- Before -->
            <bcprov-jdk18on.version>1.80</bcprov-jdk18on.version>
    ```
    ```xml
    <!-- After -->
            <bcprov-jdk18on.version>1.83</bcprov-jdk18on.version>
    ```
- **Acceptance criteria:**
  - `[grep]` `grep -n "bcprov-jdk18on.version" services/asapp-authentication-service/pom.xml`

## Step 9.3 — Upgrade build plugin versions
- **Scope:**
  - `[explicit]` `pom.xml`
- **Changes:**
  - `[edit]`
    ```xml
    <!-- Before -->
            <jacoco-maven-plugin.version>0.8.12</jacoco-maven-plugin.version>
    ```
    ```xml
    <!-- After -->
            <jacoco-maven-plugin.version>0.8.13</jacoco-maven-plugin.version>
    ```
  - `[edit]`
    ```xml
    <!-- Before -->
            <pitest-maven.version>1.20.4</pitest-maven.version>
    ```
    ```xml
    <!-- After -->
            <pitest-maven.version>1.23.0</pitest-maven.version>
    ```
  - `[edit]`
    ```xml
    <!-- Before -->
            <spotless-maven-plugin.version>2.46.1</spotless-maven-plugin.version>
    ```
    ```xml
    <!-- After -->
            <spotless-maven-plugin.version>3.4.0</spotless-maven-plugin.version>
    ```
- **Notes:**
  - `spotless-maven-plugin` 2.46.1 → 3.4.0 is safe: the `<eclipse><file>asapp_formatter.xml</file></eclipse>` config structure is unchanged in 3.x; JRE 17+ requirement is met (project runs Java 21+).
- **Acceptance criteria:**
  - `[grep]` `grep -n "jacoco-maven-plugin.version\|pitest-maven.version\|spotless-maven-plugin.version" pom.xml`

## Step 9.4 — Build verification
- **Changes:**
  - `[command]`
    ```bash
    mvn clean verify
    ```
    Expected outcome: BUILD SUCCESS across all modules.
- **Acceptance criteria:**
  - `[test]` `mvn clean verify`

---

# Phase 10 — Java 25 LTS
- **Context:** Spring Boot 4.0 has first-class support for Java 25 (current LTS, released September 2025). The `BP_JVM_VERSION` environment variable in `services/pom.xml` already references `${java.version}` — Docker images update automatically when the root property changes. Only the root `pom.xml` and the CI workflow require explicit edits.
- **Preconditions:**
  - `[phase]` Phase 8 complete
  - `[env]` JDK 25 installed locally — verify: `java -version` must show `25.x`
  - `[user]` IntelliJ IDEA Project SDK set to JDK 25 — File → Project Structure → Project → SDK

## Step 10.1 — Update java.version in root pom.xml
- **Scope:**
  - `[explicit]` `pom.xml`
- **Changes:**
  - `[edit]`
    ```xml
    <!-- Before -->
            <java.version>21</java.version>
    ```
    ```xml
    <!-- After -->
            <java.version>25</java.version>
    ```
- **Acceptance criteria:**
  - `[grep]` `grep -n "java.version" pom.xml`

## Step 10.2 — Update CI workflow Java version
- **Scope:**
  - `[explicit]` `.github/workflows/ci.yml`
- **Changes:**
  - `[edit]`
    ```yaml
    # Before
          java-version: '21'
    ```
    ```yaml
    # After
          java-version: '25'
    ```
- **Acceptance criteria:**
  - `[grep]` `grep -n "java-version" .github/workflows/ci.yml`

## Step 10.3 — Build verification
- **Changes:**
  - `[command]`
    ```bash
    mvn clean verify
    ```
    Expected outcome: BUILD SUCCESS across all modules. Java 25 is source-compatible with Java 21 — no code changes expected.
- **Acceptance criteria:**
  - `[test]` `mvn clean verify`

---

# Phase 11 — Maven wrapper upgrade
- **Context:** All 5 sub-project Maven wrappers (`libs/asapp-commons-url`, `libs/asapp-rest-clients`, and the 3 service modules) point to Apache Maven 3.9.5 and wrapper-jar 3.2.0. The root project has no wrapper. Upgrading to Maven 3.9.14 (latest 3.9.x) and wrapper-jar 3.3.4 keeps tooling current while staying in the stable 3.9.x line.
- **Preconditions:**
  - `[phase]` Phase 8 complete
  - `[env]` Maven 3.9.14 installed locally — verify: `mvn -version` must show `3.9.14`
  - `[user]` IntelliJ IDEA Maven runner configured to use the local Maven 3.9.14 installation — Settings → Build, Execution, Deployment → Build Tools → Maven → Maven home path

## Step 11.1 — Update all maven-wrapper.properties files
- **Scope:**
  - `[derived]` `find . -name "maven-wrapper.properties"`
    Expected matches (5 files): `./libs/asapp-commons-url/.mvn/wrapper/maven-wrapper.properties`, `./libs/asapp-rest-clients/.mvn/wrapper/maven-wrapper.properties`, `./services/asapp-authentication-service/.mvn/wrapper/maven-wrapper.properties`, `./services/asapp-tasks-service/.mvn/wrapper/maven-wrapper.properties`, `./services/asapp-users-service/.mvn/wrapper/maven-wrapper.properties`
- **Changes:**
  - `[transform]` — In every matched file, apply both replacements:
    ```properties
    # Before
    distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.5/apache-maven-3.9.5-bin.zip
    wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar
    ```
    ```properties
    # After
    distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.14/apache-maven-3.9.14-bin.zip
    wrapperUrl=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.4/maven-wrapper-3.3.4.jar
    ```
- **Acceptance criteria:**
  - `[no-match]` `grep -r "3.9.5\|3.2.0" $(find . -name "maven-wrapper.properties")`
  - `[grep]` `grep -r "3.9.14" $(find . -name "maven-wrapper.properties")`

## Step 11.2 — Build verification
- **Changes:**
  - `[command]`
    ```bash
    mvn clean verify
    ```
    Expected outcome: BUILD SUCCESS across all modules.
- **Acceptance criteria:**
  - `[test]` `mvn clean verify`

---

# Phase 12 — Final verification
- **Context:** End-to-end validation of the complete upgrade on the running stack, after all phases (including Java 25 and Maven wrapper updates) are complete.
- **Preconditions:**
  - `[phase]` Phase 11 complete

## Step 12.1 — Docker smoke test
- **Changes:** (none — runtime verification)
- **Acceptance criteria:**
  - `[manual]` Run the following and verify each item:
    1. `docker-compose up -d` — all containers start healthy
    2. Hit `POST /asapp-authentication-service/api/auth/authenticate` — receive `200` with access and refresh tokens
    3. Use the access token to hit a protected endpoint on `asapp-tasks-service` — receive `200`
    4. Hit `POST /asapp-authentication-service/api/auth/refresh` — receive `200` with new token pair
    5. Hit `POST /asapp-authentication-service/api/auth/revoke` — receive `200`
    6. Retry the protected endpoint with the revoked access token — receive `401`
    7. Verify Actuator health at `http://localhost:8090/asapp-authentication-service/actuator/health` — `status: UP`
    8. Verify Prometheus metrics at `http://localhost:8090/asapp-authentication-service/actuator/prometheus` — returns metrics
    9. Verify Liquibase ran cleanly — check container logs for no migration errors

---

# Phase 13 — Documentation update
- **Context:** Updates all README files and `CLAUDE.md` to reflect the completed upgrade. Covers version badge URLs, inline version strings, library references (JJWT → Nimbus), and framework version references. The exact Spring Boot 4.0.x version must be read from `pom.xml` after Phase 4 and used in place of the `4.0.x` placeholder below.
- **Preconditions:**
  - `[phase]` Phase 12 complete

## Step 13.1 — Update version references across all READMEs and CLAUDE.md
- **Scope:**
  - `[derived]` `find . \( -name "README.md" -o -name "CLAUDE.md" \) ! -path "*/target/*"`
    Expected matches (7 files): `./README.md`, `./CLAUDE.md`, `./libs/asapp-commons-url/README.md`, `./libs/asapp-rest-clients/README.md`, `./services/asapp-authentication-service/README.md`, `./services/asapp-tasks-service/README.md`, `./services/asapp-users-service/README.md`
- **Changes:**
  - `[transform]` — Java version — badge URL and text:
    ```
    Find:    Java-21
    Replace: Java-25

    Find:    java21
    Replace: java25

    Find:    Java 21
    Replace: Java 25

    Find:    JDK 21
    Replace: JDK 25
    ```
  - `[transform]` — Spring Boot version — badge URL and text (replace `4.0.x` with actual version from `pom.xml`):
    ```
    Find:    Spring%20Boot-3.4.3
    Replace: Spring%20Boot-4.0.x

    Find:    Spring Boot 3.4.3
    Replace: Spring Boot 4.0.x

    Find:    Spring Boot**: 3.4.3
    Replace: Spring Boot**: 4.0.x
    ```
  - `[transform]` — Spring Framework and Security version references:
    ```
    Find:    Spring Framework: 6.x
    Replace: Spring Framework: 7.x

    Find:    Spring Security 6.x
    Replace: Spring Security 7.x
    ```
  - `[transform]` — SpringDoc version:
    ```
    Find:    SpringDoc OpenAPI 2.8.5
    Replace: SpringDoc OpenAPI 3.x
    ```
  - `[transform]` — Maven version — badge URL and text:
    ```
    Find:    Maven-3.9+-blue
    Replace: Maven-3.9.14+-blue

    Find:    Maven 3.9.0 or higher
    Replace: Maven 3.9.14 or higher

    Find:    Maven 3.9+
    Replace: Maven 3.9.14+
    ```
  - `[transform]` — CLAUDE.md header line:
    ```
    Find:    Spring Boot 3.4.3 / Java 21
    Replace: Spring Boot 4.0.x / Java 25
    ```
- **Notes:**
  - `java21` appears in Oracle JDK download badge URLs (`#java21` fragment); replace it with `#java25`
  - Replace the `4.0.x` placeholder in all transforms above with the actual Spring Boot version from `<version>` under `spring-boot-starter-parent` in `pom.xml` after Phase 4
- **Acceptance criteria:**
  - `[no-match]` `grep -rn "Java-21\|Java 21\|JDK 21\|java21\|Spring Boot 3\.4\.3\|3\.4\.3" README.md CLAUDE.md libs/*/README.md services/*/README.md`
  - `[grep]` `grep -rn "Java 25" README.md CLAUDE.md libs/*/README.md services/*/README.md`

## Step 13.2 — Update JJWT references in authentication service and root README
- **Scope:**
  - `[explicit]` `services/asapp-authentication-service/README.md`
  - `[explicit]` `README.md`
- **Changes:**
  - `[transform]`
    ```
    Find:    JJWT (JWT library)
    Replace: Nimbus JOSE+JWT

    Find:    Spring Security + JJWT (JWT library)
    Replace: Spring Security + Nimbus JOSE+JWT

    Find:    Spring Security 6.x, JJWT 0.12.x
    Replace: Spring Security 7.x, Nimbus JOSE+JWT 9.37.3

    Find:    [JJWT (Java JWT)](https://github.com/jwtk/jjwt)
    Replace: [Nimbus JOSE+JWT](https://connect2id.com/products/nimbus-jose-jwt)
    ```
- **Acceptance criteria:**
  - `[no-match]` `grep -rn "jjwt\|JJWT" README.md services/asapp-authentication-service/README.md`
  - `[grep]` `grep -rn "Nimbus" README.md services/asapp-authentication-service/README.md`

---

## References

| Title | Description | Link |
|-------|-------------|------|
| Upgrade Spring Boot 4 Analysis | Full analysis document with risk assessment, impact analysis per area, and dependency compatibility matrix | `docs/Upgrade_Spring_Boot_4_Analysis.md` |
| Implementation Plan Template | Template defining the Plan Body Schema and execution rules used in this document | `docs/Implementation_Plan_Template.md` |
| Nimbus JOSE+JWT — JWT with HMAC | Official Nimbus docs: signing and verifying JWTs using HMAC | https://connect2id.com/products/nimbus-jose-jwt/examples/jwt-with-hmac |
| Jackson 3 Migration Guide | Official Jackson migration notes: package renames, `JsonMapper`, exception changes | https://github.com/FasterXML/jackson/blob/main/jackson3/MIGRATING_TO_JACKSON_3.md |
| OpenRewrite recipe index — Spring Boot 4 | Full list of sub-recipes within `UpgradeSpringBoot_4_0` | https://docs.openrewrite.org/recipes/java/spring/boot4 |

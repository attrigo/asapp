/**
* Copyright 2023 the original author or authors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.bcn.asapp.authentication.domain.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link JwtClaims} claim access, immutability, and factory validation.
 * <p>
 * Coverage:
 * <li>Rejects null or empty claims maps</li>
 * <li>Accepts valid inputs through constructor and factory methods (Map, key-value pairs)</li>
 * <li>Validates all keys and values are non-null in key-value factory</li>
 * <li>Returns immutable claims map preventing external modification</li>
 * <li>Provides type-safe claim access with Optional return</li>
 * <li>Returns empty Optional for missing claims or type mismatches</li>
 * <li>Provides access to wrapped claims map</li>
 */
class JwtClaimsTests {

    @Nested
    class CreateJwtClaimsWithConstructor {

        @Test
        void ReturnsJwtClaims_ValidClaims() {
            // Given
            var claims = Map.<String, Object>of("sub", "user", "exp", 123456789L, "active", true);

            // When
            var actual = new JwtClaims(claims);

            // Then
            assertThat(actual.claims()).hasSize(3)
                                       .containsEntry("sub", "user")
                                       .containsEntry("exp", 123456789L)
                                       .containsEntry("active", true);
        }

        @Test
        void ThrowsIllegalArgumentException_NullClaims() {
            // When
            var actual = catchThrowable(() -> new JwtClaims(null));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Claim could not be null or empty");
        }

        @Test
        void ThrowsIllegalArgumentException_EmptyClaims() {
            // When
            var actual = catchThrowable(() -> new JwtClaims(Map.of()));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Claim could not be null or empty");
        }

    }

    @Nested
    class CreateJwtClaimsWithMapFactoryMethod {

        @Test
        void ReturnsJwtClaims_ValidClaims() {
            // Given
            var claims = Map.<String, Object>of("sub", "user", "exp", 123456789L, "active", true);

            // When
            var actual = JwtClaims.of(claims);

            // Then
            assertThat(actual.claims()).hasSize(3)
                                       .containsEntry("sub", "user")
                                       .containsEntry("exp", 123456789L)
                                       .containsEntry("active", true);
        }

        @Test
        void ThrowsIllegalArgumentException_NullClaims() {
            // When
            var actual = catchThrowable(() -> JwtClaims.of(null));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Claim could not be null or empty");
        }

        @Test
        void ThrowsIllegalArgumentException_EmptyClaims() {
            // When
            var actual = catchThrowable(() -> JwtClaims.of(Map.of()));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Claim could not be null or empty");
        }

    }

    @Nested
    class CreateJwtClaimsWithKeyValueFactoryMethod {

        @Test
        void ReturnsJwtClaims_ValidParameters() {
            // When
            var actual = JwtClaims.of("key1", "value1", "key2", "value2");

            // Then
            assertThat(actual.claims()).hasSize(2)
                                       .containsEntry("key1", "value1")
                                       .containsEntry("key2", "value2");
        }

        @Test
        void ThrowsIllegalArgumentException_NullFirstKey() {
            // When
            var actual = catchThrowable(() -> JwtClaims.of(null, "value1", "key2", "value2"));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Claim keys and values must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullFirstValue() {
            // When
            var actual = catchThrowable(() -> JwtClaims.of("key1", null, "key2", "value2"));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Claim keys and values must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullSecondKey() {
            // When
            var actual = catchThrowable(() -> JwtClaims.of("key1", "value1", null, "value2"));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Claim keys and values must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullSecondValue() {
            // When
            var actual = catchThrowable(() -> JwtClaims.of("key1", "value1", "key2", null));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Claim keys and values must not be null");
        }

    }

    @Nested
    class ModifyClaims {

        @Test
        void ReturnsImmutableClaims_ModifiedOriginalClaims() {
            // Given
            var claims = new HashMap<String, Object>();
            claims.put("sub", "user");
            claims.put("exp", 123456789L);

            // When
            var actual = JwtClaims.of(claims);
            claims.put("active", true);

            // Then
            assertThat(actual.claims()).hasSize(2)
                                       .doesNotContainKey("active");
        }

        @Test
        void ThrowsUnsupportedOperationException_ModifiedClaims() {
            // Given
            var claims = Map.<String, Object>of("sub", "user", "exp", 123456789L, "active", true);
            var jwtClaims = JwtClaims.of(claims);

            // When
            var actual = catchThrowable(() -> jwtClaims.claims()
                                                       .put("active", "true"));

            // Then
            assertThat(actual).isInstanceOf(UnsupportedOperationException.class);
        }

    }

    @Nested
    class GetClaim {

        @Test
        void ReturnsClaimValue_ClaimMatchingTypeInClaims() {
            // Given
            var claims = Map.<String, Object>of("sub", "user", "exp", 123456789L, "active", true);
            var jwtClaims = JwtClaims.of(claims);

            // When
            var actual = jwtClaims.claim("sub", String.class);

            // Then
            assertThat(actual).isPresent()
                              .contains("user");
        }

        @Test
        void ReturnsClaimValue_MultipleClaimsDifferentTypes() {
            // Given
            var claims = Map.<String, Object>of("sub", "user", "exp", 123456789L, "active", true);
            var jwtClaims = JwtClaims.of(claims);

            // When
            var subClaim = jwtClaims.claim("sub", String.class);
            var expClaim = jwtClaims.claim("exp", Long.class);
            var activeClaim = jwtClaims.claim("active", Boolean.class);

            // Then
            assertThat(subClaim).isPresent()
                                .contains("user");
            assertThat(expClaim).isPresent()
                                .contains(123456789L);
            assertThat(activeClaim).isPresent()
                                   .contains(true);
        }

        @Test
        void ReturnsEmpty_ClaimNameNotExistsInClaims() {
            // Given
            var claims = Map.<String, Object>of("sub", "user", "exp", 123456789L, "active", true);
            var jwtClaims = JwtClaims.of(claims);

            // When
            var actual = jwtClaims.claim("claim_name_not_exist", String.class);

            // Then
            assertThat(actual).isEmpty();
        }

        @Test
        void ReturnsEmpty_NullClaimName() {
            // Given
            var claims = Map.<String, Object>of("sub", "user", "exp", 123456789L, "active", true);
            var jwtClaims = JwtClaims.of(claims);

            // When
            var actual = jwtClaims.claim(null, String.class);

            // Then
            assertThat(actual).isEmpty();
        }

        @Test
        void ReturnsEmpty_RequiredTypeMismatch() {
            // Given
            var claims = Map.<String, Object>of("sub", "user", "exp", 123456789L, "active", true);
            var jwtClaims = JwtClaims.of(claims);

            // When
            var actual = jwtClaims.claim("exp", String.class);

            // Then
            assertThat(actual).isEmpty();
        }

    }

    @Nested
    class GetValue {

        @Test
        void ReturnsClaimsMap_ValidClaims() {
            // Given
            var claims = Map.<String, Object>of("sub", "user", "exp", 123456789L, "active", true);
            var jwtClaims = JwtClaims.of(claims);

            // When
            var actual = jwtClaims.value();

            // Then
            assertThat(actual).isEqualTo(claims);
        }

    }

}

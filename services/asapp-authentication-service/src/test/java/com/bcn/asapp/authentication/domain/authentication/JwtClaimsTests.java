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

class JwtClaimsTests {

    private final Map<String, Object> claimsValue = Map.of("sub", "user", "exp", 123456789L, "active", true);

    @Nested
    class CreateJwtClaimsWithConstructor {

        @Test
        void ThenThrowsIllegalArgumentException_GivenClaimsIsNull() {
            // When
            var thrown = catchThrowable(() -> new JwtClaims(null));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Claim could not be null or empty");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenClaimsIsEmpty() {
            // When
            var thrown = catchThrowable(() -> new JwtClaims(Map.of()));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Claim could not be null or empty");
        }

        @Test
        void ThenReturnsJwtClaims_GivenClaimsAreValid() {
            // When
            var actual = new JwtClaims(claimsValue);

            // Then
            assertThat(actual.claims()).hasSize(3)
                                       .containsEntry("sub", "user")
                                       .containsEntry("exp", 123456789L)
                                       .containsEntry("active", true);
        }

    }

    @Nested
    class CreateJwtClaimsWithMapFactoryMethod {

        @Test
        void ThenThrowsIllegalArgumentException_GivenClaimsIsNull() {
            // When
            var thrown = catchThrowable(() -> JwtClaims.of(null));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Claim could not be null or empty");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenClaimsIsEmpty() {
            // When
            var thrown = catchThrowable(() -> JwtClaims.of(Map.of()));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Claim could not be null or empty");
        }

        @Test
        void ThenReturnsJwtClaims_GivenClaimsAreValid() {
            // When
            var actual = JwtClaims.of(claimsValue);

            // Then
            assertThat(actual.claims()).hasSize(3)
                                       .containsEntry("sub", "user")
                                       .containsEntry("exp", 123456789L)
                                       .containsEntry("active", true);
        }

    }

    @Nested
    class CreateJwtClaimsWithKeyValueFactoryMethod {

        @Test
        void ThenThrowsIllegalArgumentException_GivenFirstKeyIsNull() {
            // When
            var thrown = catchThrowable(() -> JwtClaims.of(null, "value1", "key2", "value2"));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Claim keys and values must not be null");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenFirstValueIsNull() {
            // When
            var thrown = catchThrowable(() -> JwtClaims.of("key1", null, "key2", "value2"));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Claim keys and values must not be null");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenSecondKeyIsNull() {
            // When
            var thrown = catchThrowable(() -> JwtClaims.of("key1", "value1", null, "value2"));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Claim keys and values must not be null");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenSecondValueIsNull() {
            // When
            var thrown = catchThrowable(() -> JwtClaims.of("key1", "value1", "key2", null));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Claim keys and values must not be null");
        }

        @Test
        void ThenReturnsJwtClaims_GivenParametersAreValid() {
            // When
            var actual = JwtClaims.of("key1", "value1", "key2", "value2");

            // Then
            assertThat(actual.claims()).hasSize(2)
                                       .containsEntry("key1", "value1")
                                       .containsEntry("key2", "value2");
        }

    }

    @Nested
    class ModifyClaims {

        @Test
        void ThenReturnsImmutableClaims_GivenOriginalClaimsAreModified() {
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
        void ThenThrowsException_GivenClaimsAreModified() {
            // Given
            var jwtClaims = JwtClaims.of(claimsValue);

            // When
            var thrown = catchThrowable(() -> jwtClaims.claims()
                                                       .put("active", "true"));

            // Then
            assertThat(thrown).isInstanceOf(UnsupportedOperationException.class);
        }

    }

    @Nested
    class GetClaim {

        @Test
        void ThenReturnsEmpty_GivenClaimNameNotExist() {
            // Given
            var jwtClaims = JwtClaims.of(claimsValue);

            // When
            var actual = jwtClaims.claim("non_existent", String.class);

            // Then
            assertThat(actual).isEmpty();
        }

        @Test
        void ThenReturnsEmpty_GivenClaimNameIsNull() {
            // Given
            var jwtClaims = JwtClaims.of(claimsValue);

            // When
            var actual = jwtClaims.claim(null, String.class);

            // Then
            assertThat(actual).isEmpty();
        }

        @Test
        void ThenReturnsEmpty_GivenRequiredTypeNotMatch() {
            // Given
            var jwtClaims = JwtClaims.of(claimsValue);

            // When
            var actual = jwtClaims.claim("exp", String.class);

            // Then
            assertThat(actual).isEmpty();
        }

        @Test
        void ThenReturnsClaimValue_GivenClaimExistsAndTypeMatches() {
            // Given
            var jwtClaims = JwtClaims.of(claimsValue);

            // When
            var actual = jwtClaims.claim("sub", String.class);

            // Then
            assertThat(actual).isPresent()
                              .contains("user");
        }

        @Test
        void ThenReturnsClaimValue_GivenMultipleClaimsWithDifferentTypes() {
            // Given
            var jwtClaims = JwtClaims.of(claimsValue);

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

    }

    @Nested
    class GetValue {

        @Test
        void ThenReturnsClaimsMap_GivenClaimsAreValid() {
            // Given
            var jwtClaims = JwtClaims.of(claimsValue);

            // When
            var actual = jwtClaims.value();

            // Then
            assertThat(actual).isEqualTo(claimsValue);
        }

    }

}

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

import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link JwtAuthenticationId} validation and value access.
 * <p>
 * Coverage:
 * <li>Rejects null UUID values</li>
 * <li>Accepts valid UUIDs through constructor and factory method</li>
 * <li>Provides access to wrapped UUID value</li>
 */
class JwtAuthenticationIdTests {

    @Nested
    class CreateJwtAuthenticationIdWithConstructor {

        @Test
        void ReturnsJwtAuthenticationId_ValidId() {
            // Given
            var jwtAuthenticationId = UUID.fromString("9b3e7f12-4c8a-4d3e-a9f2-6e5d4c3b2a10");

            // When
            var actual = new JwtAuthenticationId(jwtAuthenticationId);

            // Then
            assertThat(actual.id()).isEqualTo(jwtAuthenticationId);
        }

        @Test
        void ThrowsIllegalArgumentException_NullId() {
            // When
            var actual = catchThrowable(() -> new JwtAuthenticationId(null));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("JWT Authentication ID must not be null");
        }

    }

    @Nested
    class CreateJwtAuthenticationIdWithFactoryMethod {

        @Test
        void ReturnsJwtAuthenticationId_ValidId() {
            // Given
            var jwtAuthenticationId = UUID.fromString("9b3e7f12-4c8a-4d3e-a9f2-6e5d4c3b2a10");

            // When
            var actual = JwtAuthenticationId.of(jwtAuthenticationId);

            // Then
            assertThat(actual.id()).isEqualTo(jwtAuthenticationId);
        }

        @Test
        void ThrowsIllegalArgumentException_NullId() {
            // When
            var actual = catchThrowable(() -> JwtAuthenticationId.of(null));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("JWT Authentication ID must not be null");
        }

    }

    @Nested
    class GetValue {

        @Test
        void ReturnsJwtAuthenticationIdValue_ValidJwtAuthenticationId() {
            // Given
            var jwtAuthenticationIdValue = UUID.fromString("9b3e7f12-4c8a-4d3e-a9f2-6e5d4c3b2a10");
            var jwtAuthenticationId = JwtAuthenticationId.of(jwtAuthenticationIdValue);

            // When
            var actual = jwtAuthenticationId.value();

            // Then
            assertThat(actual).isEqualTo(jwtAuthenticationIdValue);
        }

    }

}

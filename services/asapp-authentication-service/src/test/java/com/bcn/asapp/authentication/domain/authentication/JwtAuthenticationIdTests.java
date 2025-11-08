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

class JwtAuthenticationIdTests {

    private final UUID idValue = UUID.fromString("9b3e7f12-4c8a-4d3e-a9f2-6e5d4c3b2a10");

    @Nested
    class CreateJwtAuthenticationIdWithConstructor {

        @Test
        void ThenThrowsIllegalArgumentException_GivenIdIsNull() {
            // When
            var thrown = catchThrowable(() -> new JwtAuthenticationId(null));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("JWT Authentication ID must not be null");
        }

        @Test
        void ThenReturnsUserId_GivenIdIsValid() {
            // When
            var actual = new JwtAuthenticationId(idValue);

            // Then
            assertThat(actual.id()).isEqualTo(idValue);
        }

    }

    @Nested
    class CreateJwtAuthenticationIdWithFactoryMethod {

        @Test
        void ThenThrowsIllegalArgumentException_GivenIdIsNull() {
            // When
            var thrown = catchThrowable(() -> JwtAuthenticationId.of(null));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("JWT Authentication ID must not be null");
        }

        @Test
        void ThenReturnsUserId_GivenIdIsValid() {
            // When
            var actual = JwtAuthenticationId.of(idValue);

            // Then
            assertThat(actual.id()).isEqualTo(idValue);
        }

    }

    @Nested
    class GetValue {

        @Test
        void ThenReturnsUserIdValue_GivenJwtAuthenticationIsValid() {
            // Given
            var jwtAuthenticationId = JwtAuthenticationId.of(idValue);

            // When
            var actual = jwtAuthenticationId.value();

            // Then
            assertThat(actual).isEqualTo(idValue);
        }

    }

}

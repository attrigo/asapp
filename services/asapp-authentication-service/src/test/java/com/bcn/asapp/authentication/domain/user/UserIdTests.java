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

package com.bcn.asapp.authentication.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UserIdTests {

    private final UUID idValue = UUID.randomUUID();

    @Nested
    class CreateUserIdWithConstructor {

        @Test
        void ThenThrowsIllegalArgumentException_GivenIdIsNull() {
            // When
            var thrown = catchThrowable(() -> new UserId(null));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("User ID must not be null");
        }

        @Test
        void ThenReturnsUserId_GivenIdIsValid() {
            // When
            var actual = new UserId(idValue);

            // Then
            assertThat(actual.id()).isEqualTo(idValue);
        }

    }

    @Nested
    class CreateUserIdWithFactoryMethod {

        @Test
        void ThenThrowsIllegalArgumentException_GivenIdIsNull() {
            // When
            var thrown = catchThrowable(() -> UserId.of(null));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("User ID must not be null");
        }

        @Test
        void ThenReturnsUserId_GivenIdIsValid() {
            // When
            var actual = UserId.of(idValue);

            // Then
            assertThat(actual.id()).isEqualTo(idValue);
        }

    }

    @Nested
    class GetValue {

        @Test
        void ThenReturnsUserIdValue_GivenUserIdIsValid() {
            // Given
            var userId = UserId.of(idValue);

            // When
            var actual = userId.value();

            // Then
            assertThat(actual).isEqualTo(idValue);
        }

    }

}

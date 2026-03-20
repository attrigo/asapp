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

package com.bcn.asapp.users.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link UserId} validation and value access.
 * <p>
 * Coverage:
 * <li>Rejects null identifier</li>
 * <li>Accepts valid UUIDs through constructor and factory method</li>
 * <li>Provides access to wrapped identifier value</li>
 */
class UserIdTests {

    @Nested
    class CreateUserIdWithConstructor {

        @Test
        void ReturnsUserId_ValidId() {
            // Given
            var userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

            // When
            var actual = new UserId(userId);

            // Then
            assertThat(actual.id()).isEqualTo(userId);
        }

        @Test
        void ThrowsIllegalArgumentException_NullId() {
            // When
            var actual = catchThrowable(() -> new UserId(null));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("User ID must not be null");
        }

    }

    @Nested
    class CreateUserIdWithFactoryMethod {

        @Test
        void ReturnsUserId_ValidId() {
            // Given
            var userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

            // When
            var actual = UserId.of(userId);

            // Then
            assertThat(actual.id()).isEqualTo(userId);
        }

        @Test
        void ThrowsIllegalArgumentException_NullId() {
            // When
            var actual = catchThrowable(() -> UserId.of(null));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("User ID must not be null");
        }

    }

    @Nested
    class GetValue {

        @Test
        void ReturnsUserIdValue_ValidUserId() {
            // Given
            var userIdValue = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
            var userId = UserId.of(userIdValue);

            // When
            var actual = userId.value();

            // Then
            assertThat(actual).isEqualTo(userIdValue);
        }

    }

}

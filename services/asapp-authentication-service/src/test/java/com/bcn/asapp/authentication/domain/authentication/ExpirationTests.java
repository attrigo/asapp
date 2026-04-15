/**
* Copyright 2023 the original author or authors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     https://www.apache.org/licenses/LICENSE-2.0
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

import java.time.Instant;
import java.util.Date;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link Expiration} timestamp conversion, factory methods, and defensive copying.
 * <p>
 * Coverage:
 * <li>Rejects null expiration timestamp values</li>
 * <li>Accepts valid inputs through constructor and factory methods (Date, issued timestamp plus milliseconds)</li>
 * <li>Calculates expiration from issued timestamp plus duration correctly</li>
 * <li>Converts Instant to Date representation correctly</li>
 * <li>Returns new Date instance on each call to prevent mutation</li>
 * <li>Provides access to wrapped Instant value</li>
 */
class ExpirationTests {

    @Nested
    class CreateExpirationWithConstructor {

        @Test
        void ReturnsExpiration_ValidExpiration() {
            // Given
            var expiration = Instant.parse("2025-01-01T11:00:00Z");

            // When
            var actual = new Expiration(expiration);

            // Then
            assertThat(actual.expiration()).isEqualTo(expiration);
        }

        @Test
        void ThrowsIllegalArgumentException_NullExpiration() {
            // When
            var actual = catchThrowable(() -> new Expiration(null));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Expiration must not be null");
        }

    }

    @Nested
    class CreateExpirationWithIssuedAndExpirationTimeFactoryMethod {

        @Test
        void ReturnsExpiration_ValidIssuedAndExpirationTime() {
            // Given
            var issuedValue = Instant.parse("2025-01-01T11:00:00Z");
            var issued = Issued.of(issuedValue);
            var expiration = issuedValue.plusMillis(1000L);

            // When
            var actual = Expiration.of(issued, 1000L);

            // Then
            assertThat(actual.expiration()).isEqualTo(expiration);
        }

        @Test
        void ThrowsIllegalArgumentException_NullIssued() {
            // When
            var actual = catchThrowable(() -> Expiration.of(null, 1000L));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Issued must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullExpirationTime() {
            // Given
            var issuedValue = Instant.parse("2025-01-01T11:00:00Z");
            var issued = Issued.of(issuedValue);

            // When
            var actual = catchThrowable(() -> Expiration.of(issued, null));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Expiration time in milliseconds must not be null");
        }

    }

    @Nested
    class CreateExpirationWithDateFactoryMethod {

        @Test
        void ReturnsExpiration_ValidExpirationDate() {
            // Given
            var expiration = Instant.parse("2025-01-01T11:00:00Z");
            var expirationDate = Date.from(expiration);

            // When
            var actual = Expiration.of(expirationDate);

            // Then
            assertThat(actual.expiration()).isEqualTo(expiration);
        }

        @Test
        void ThrowsIllegalArgumentException_NullExpirationDate() {
            // When
            var actual = catchThrowable(() -> Expiration.of(null));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Expiration date must not be null");
        }

    }

    @Nested
    class GetValue {

        @Test
        void ReturnsInstant_ValidExpiration() {
            // Given
            var expirationValue = Instant.parse("2025-01-01T11:00:00Z");
            var expiration = new Expiration(expirationValue);

            // When
            var actual = expiration.value();

            // Then
            assertThat(actual).isEqualTo(expirationValue);
        }

    }

    @Nested
    class GetAsDate {

        @Test
        void ReturnsDateWithSameInstant_ValidExpiration() {
            // Given
            var expirationValue = Instant.parse("2025-01-01T11:00:00Z");
            var expiration = new Expiration(expirationValue);
            var expirationDate = Date.from(expirationValue);
            var expirationTimeMillis = expirationValue.toEpochMilli();

            // When
            var actual = expiration.asDate();

            // Then
            assertThat(actual).isEqualTo(expirationDate);
            assertThat(actual.getTime()).isEqualTo(expirationTimeMillis);
        }

        @Test
        void ReturnsNewDateInstance_ValidExpiration() {
            // Given
            var expirationValue = Instant.parse("2025-01-01T11:00:00Z");
            var expiration = new Expiration(expirationValue);

            // When
            var actual1 = expiration.asDate();
            var actual2 = expiration.asDate();

            // Then
            assertThat(actual1).isNotSameAs(actual2);
            assertThat(actual1).isEqualTo(actual2);
        }

    }

}

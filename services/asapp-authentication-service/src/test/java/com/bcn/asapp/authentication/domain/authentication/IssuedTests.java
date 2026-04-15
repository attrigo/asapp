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

import static java.time.temporal.ChronoUnit.MILLIS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

import java.time.Instant;
import java.util.Date;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link Issued} timestamp conversion, factory methods, and defensive copying.
 * <p>
 * Coverage:
 * <li>Rejects null issued timestamp values</li>
 * <li>Accepts valid inputs through constructor and factory methods (now, Instant, Date)</li>
 * <li>Converts Instant to Date representation correctly</li>
 * <li>Returns new Date instance on each call to prevent mutation</li>
 * <li>Provides access to wrapped Instant value</li>
 */
class IssuedTests {

    @Nested
    class CreateIssuedWithConstructor {

        @Test
        void ReturnsIssued_ValidIssued() {
            // Given
            var issued = Instant.parse("2025-01-01T10:00:00Z");

            // When
            var actual = new Issued(issued);

            // Then
            assertThat(actual.issued()).isEqualTo(issued);
        }

        @Test
        void ThrowsIllegalArgumentException_NullIssued() {
            // When
            var actual = catchThrowable(() -> new Issued(null));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Issued must not be null");
        }

    }

    @Nested
    class CreateIssuedWithNowFactoryMethod {

        @Test
        void ReturnsIssued_CloseToCurrentTime() {
            // When
            var actual = Issued.now();

            // Then
            assertThat(actual.issued()).isCloseTo(Instant.now(), within(100, MILLIS));
        }

    }

    @Nested
    class CreateIssuedWithInstantFactoryMethod {

        @Test
        void ReturnsIssued_ValidIssuedInstant() {
            // Given
            var issued = Instant.parse("2025-01-01T10:00:00Z");

            // When
            var actual = Issued.of(issued);

            // Then
            assertThat(actual.issued()).isEqualTo(issued);
        }

        @Test
        void ThrowsIllegalArgumentException_NullIssuedInstant() {
            // When
            var actual = catchThrowable(() -> Issued.of((Instant) null));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Issued must not be null");
        }

    }

    @Nested
    class CreateIssuedWithDateFactoryMethod {

        @Test
        void ReturnsIssued_ValidIssuedDate() {
            // Given
            var issued = Instant.parse("2025-01-01T10:00:00Z");
            var issuedDate = Date.from(issued);

            // When
            var actual = Issued.of(issuedDate);

            // Then
            assertThat(actual.issued()).isEqualTo(issued);
        }

        @Test
        void ThrowsIllegalArgumentException_NullIssuedDate() {
            // When
            var actual = catchThrowable(() -> Issued.of((Date) null));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Issued date must not be null");
        }

    }

    @Nested
    class GetValue {

        @Test
        void ReturnsInstant_ValidIssued() {
            // Given
            var issuedValue = Instant.parse("2025-01-01T10:00:00Z");
            var issued = Issued.of(issuedValue);

            // When
            var actual = issued.value();

            // Then
            assertThat(actual).isEqualTo(issuedValue);
        }

    }

    @Nested
    class GetAsDate {

        @Test
        void ReturnsDateWithSameInstant_ValidIssued() {
            // Given
            var issuedValue = Instant.parse("2025-01-01T10:00:00Z");
            var issued = Issued.of(issuedValue);
            var issuedDate = Date.from(issuedValue);
            var issuedTimeMillis = issuedValue.toEpochMilli();

            // When
            var actual = issued.asDate();

            // Then
            assertThat(actual).isEqualTo(issuedDate);
            assertThat(actual.getTime()).isEqualTo(issuedTimeMillis);
        }

        @Test
        void ReturnsNewDateInstance_ValidIssued() {
            // Given
            var issuedValue = Instant.parse("2025-01-01T10:00:00Z");
            var issued = Issued.of(issuedValue);

            // When
            var actual1 = issued.asDate();
            var actual2 = issued.asDate();

            // Then
            assertThat(actual1).isNotSameAs(actual2);
            assertThat(actual1).isEqualTo(actual2);
        }

    }

}

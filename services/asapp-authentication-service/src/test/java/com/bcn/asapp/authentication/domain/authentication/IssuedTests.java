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

import static java.time.temporal.ChronoUnit.MILLIS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

import java.time.Instant;
import java.util.Date;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class IssuedTests {

    private final Instant issuedValue = Instant.parse("2025-01-01T10:00:00Z");

    @Nested
    class CreateIssuedWithConstructor {

        @Test
        void ThrowsIllegalArgumentException_NullIssued() {
            // When
            var thrown = catchThrowable(() -> new Issued(null));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Issued must not be null");
        }

        @Test
        void ReturnsIssued_ValidIssued() {
            // When
            var actual = new Issued(issuedValue);

            // Then
            assertThat(actual.issued()).isEqualTo(issuedValue);
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
        void ThrowsIllegalArgumentException_NullIssuedInstant() {
            // When
            var thrown = catchThrowable(() -> Issued.of((Instant) null));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Issued must not be null");
        }

        @Test
        void ReturnsIssued_ValidIssuedInstant() {
            // When
            var actual = Issued.of(issuedValue);

            // Then
            assertThat(actual.issued()).isEqualTo(issuedValue);
        }

    }

    @Nested
    class CreateIssuedWithDateFactory {

        @Test
        void ThrowsIllegalArgumentException_NullIssuedDate() {
            // When
            var thrown = catchThrowable(() -> Issued.of((Date) null));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Issued date must not be null");
        }

        @Test
        void ReturnsIssued_ValidIssuedDate() {
            // Given
            var date = Date.from(issuedValue);

            // When
            var actual = Issued.of(date);

            // Then
            assertThat(actual.issued()).isEqualTo(issuedValue);
        }

    }

    @Nested
    class GetValue {

        @Test
        void ReturnsInstant_ValidIssued() {
            // Given
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
            var issued = Issued.of(issuedValue);

            // When
            var actual = issued.asDate();

            // Then
            assertThat(actual).isEqualTo(Date.from(issuedValue));
            assertThat(actual.getTime()).isEqualTo(issuedValue.toEpochMilli());
        }

        @Test
        void ReturnsNewDateInstance_ValidIssued() {
            // Given
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

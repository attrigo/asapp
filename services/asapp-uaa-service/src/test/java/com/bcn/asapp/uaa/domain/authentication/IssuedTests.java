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

package com.bcn.asapp.uaa.domain.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class IssuedTests {

    private final Instant issuedValue = Instant.parse("2025-10-01T10:30:00Z");

    @Nested
    class CreateIssuedWithConstructor {

        @Test
        void ThenThrowsIllegalArgumentException_GivenIssuedIsNull() {
            // When
            var thrown = catchThrowable(() -> new Issued(null));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Issued must not be null");
        }

        @Test
        void ThenReturnsIssued_GivenIssuedIsValid() {
            // When
            var actual = new Issued(issuedValue);

            // Then
            assertThat(actual.issued()).isEqualTo(issuedValue);
        }

    }

    @Nested
    class CreateIssuedWithNowFactoryMethod {

        @Test
        void ThenReturnsIssuedCloseToCurrentTime() {
            // When
            var actual = Issued.now();

            // Then
            assertThat(actual.issued()).isCloseTo(Instant.now(), within(100, ChronoUnit.MILLIS));
        }

    }

    @Nested
    class CreateIssuedWithInstantFactoryMethod {

        @Test
        void ThenThrowsIllegalArgumentException_GivenIssuedInstantIsNull() {
            // When
            var thrown = catchThrowable(() -> Issued.of((Instant) null));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Issued must not be null");
        }

        @Test
        void ThenReturnsIssued_GivenIssuedInstantIsValid() {
            // When
            var actual = Issued.of(issuedValue);

            // Then
            assertThat(actual.issued()).isEqualTo(issuedValue);
        }

    }

    @Nested
    class CreateIssuedWithDateFactory {

        @Test
        void ThenThrowsIllegalArgumentException_GivenIssuedDateIsNull() {
            // When
            var thrown = catchThrowable(() -> Issued.of((Date) null));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Issued date must not be null");
        }

        @Test
        void ThenReturnsIssued_GivenIssuedDateIsValid() {
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
        void ThenReturnsInstant_GivenIssuedIsValid() {
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
        void ThenReturnsDateWithSameInstant_GivenIssuedIsValid() {
            // Given
            var issued = Issued.of(issuedValue);

            // When
            var actual = issued.asDate();

            // Then
            assertThat(actual).isEqualTo(Date.from(issuedValue));
            assertThat(actual.getTime()).isEqualTo(issuedValue.toEpochMilli());
        }

        @Test
        void ThenReturnsNewDateInstance_GivenIssuedIsValid() {
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

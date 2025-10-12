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

import java.time.Instant;
import java.util.Date;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ExpirationTests {

    private final Instant expirationValue = Instant.parse("2025-10-01T10:30:00Z");

    @Nested
    class CreateExpirationWithConstructor {

        @Test
        void ThenThrowsIllegalArgumentException_GivenExpirationIsNull() {
            // When
            var thrown = catchThrowable(() -> new Expiration(null));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Expiration must not be null");
        }

        @Test
        void ThenReturnsExpiration_GivenExpirationIsValid() {
            // When
            var actual = new Expiration(expirationValue);

            // Then
            assertThat(actual.expiration()).isEqualTo(expirationValue);
        }

    }

    @Nested
    class CreateExpirationWithIssuedAndExpirationTimeFactoryMethod {

        @Test
        void ThenThrowsIllegalArgumentException_GivenIssuedIsNull() {
            // When
            var thrown = catchThrowable(() -> Expiration.of(null, 1000L));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Issued must not be null");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenExpirationTimeIsNull() {
            // When
            var thrown = catchThrowable(() -> Expiration.of(Issued.of(expirationValue), null));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Expiration time in milliseconds must not be null");
        }

        @Test
        void ThenReturnsExpiration_GivenIssuedAndExpirationTimeAreValid() {
            // When
            var actual = Expiration.of(Issued.of(expirationValue), 1000L);

            // Then
            assertThat(actual.expiration()).isEqualTo(expirationValue.plusMillis(1000L));
        }

    }

    @Nested
    class CreateExpirationWithDateFactory {

        @Test
        void ThenThrowsIllegalArgumentException_GivenExpirationDateIsNull() {
            // When
            var thrown = catchThrowable(() -> Expiration.of(null));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Expiration date must not be null");
        }

        @Test
        void ThenReturnsExpiration_GivenExpirationDateIsValid() {
            // Given
            var date = Date.from(expirationValue);

            // When
            var actual = Expiration.of(date);

            // Then
            assertThat(actual.expiration()).isEqualTo(expirationValue);
        }

    }

    @Nested
    class GetValue {

        @Test
        void ThenReturnsInstant_GivenExpirationIsValid() {
            // Given
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
        void ThenReturnsDateWithSameInstant_GivenExpirationIsValid() {
            // Given
            var expiration = new Expiration(expirationValue);

            // When
            var actual = expiration.asDate();

            // Then
            assertThat(actual).isEqualTo(Date.from(expirationValue));
            assertThat(actual.getTime()).isEqualTo(expirationValue.toEpochMilli());
        }

        @Test
        void ThenReturnsNewDateInstance_GivenExpirationIsValid() {
            // Given
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

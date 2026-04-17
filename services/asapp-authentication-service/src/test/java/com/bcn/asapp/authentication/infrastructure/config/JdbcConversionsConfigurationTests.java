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

package com.bcn.asapp.authentication.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.postgresql.util.PGobject;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import com.bcn.asapp.authentication.infrastructure.authentication.persistence.JdbcJwtClaimsEntity;

/**
 * Tests {@link JdbcConversionsConfiguration} JSONB to claims and claims to JSONB conversion.
 * <p>
 * Coverage:
 * <li>Converts JSONB PostgreSQL object to JWT claims entity</li>
 * <li>Rejects invalid JSONB values during reading conversion</li>
 * <li>Converts JWT claims entity to PostgreSQL JSONB object</li>
 * <li>Rejects serialization failures during writing conversion</li>
 */
@ExtendWith(MockitoExtension.class)
class JdbcConversionsConfigurationTests {

    @Nested
    class ConvertPgObjectToClaims {

        private final JdbcConversionsConfiguration.ClaimsReadingConverter converter = new JdbcConversionsConfiguration.ClaimsReadingConverter(new JsonMapper());

        @Test
        void ReturnsClaims_ValidJsonbSource() throws Exception {
            // Given
            var pgObject = new PGobject();
            pgObject.setType("jsonb");
            pgObject.setValue("{\"sub\":\"user@asapp.com\"}");

            // When
            var actual = converter.convert(pgObject);

            // Then
            assertThat(actual).isNotNull();
            assertThat(actual.claims()).containsEntry("sub", "user@asapp.com");
        }

        @Test
        void ThrowsIllegalArgumentException_InvalidJsonbSource() throws Exception {
            // Given
            var pgObject = new PGobject();
            pgObject.setType("jsonb");
            pgObject.setValue("not-valid-json");

            // When
            var actual = catchThrowable(() -> converter.convert(pgObject));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Failed to convert JSON string to JWT claims");
        }

    }

    @Nested
    class ConvertClaimsToPgObject {

        @Mock
        private ObjectMapper objectMapper;

        @Test
        void ReturnsPgObject_ValidClaimsSource() {
            // Given
            var realObjectMapper = new JsonMapper();
            var converter = new JdbcConversionsConfiguration.ClaimsWritingConverter(realObjectMapper);
            var jwtClaims = new JdbcJwtClaimsEntity(Map.of("sub", "user@asapp.com"));

            // When
            var actual = converter.convert(jwtClaims);

            // Then
            assertThat(actual).isNotNull();
            assertThat(actual.getType()).isEqualTo("jsonb");
            assertThat(actual.getValue()).contains("\"sub\"");
        }

        @Test
        void ThrowsIllegalArgumentException_SerializationFails() {
            // Given
            var converter = new JdbcConversionsConfiguration.ClaimsWritingConverter(objectMapper);
            var claimsEntity = new JdbcJwtClaimsEntity(Map.of("sub", "user@asapp.com"));

            given(objectMapper.writeValueAsString(any())).willThrow(new JacksonException("Serialization failed") {});

            // When
            var actual = catchThrowable(() -> converter.convert(claimsEntity));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Failed to convert JWT claim to PGobject");
        }

    }

}

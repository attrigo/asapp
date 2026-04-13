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

package com.bcn.asapp.tasks.infrastructure.config;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureWebMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.InvalidFormatException;

/**
 * Tests {@link ObjectMapper} JSON serialization configuration and temporal format handling.
 * <p>
 * Coverage:
 * <li>Excludes null values from JSON output</li>
 * <li>Includes empty collections in JSON output</li>
 * <li>Serializes temporal types to ISO-8601 format (LocalDate, LocalDateTime, Instant)</li>
 * <li>Rejects non-ISO-8601 date formats during deserialization</li>
 * <li>Deserializes ISO-8601 dates to corresponding Java temporal types</li>
 */
@ExtendWith(SpringExtension.class)
@TestPropertySource(locations = "classpath:application.properties")
@AutoConfigureWebMvc
class JacksonConfigurationIT {

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    class SerializeEntityToJson {

        @Test
        void ReturnsJsonWithoutNullValues_EntityWithNullValues() {
            // Given
            var uuid = UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479");
            var testEntity = new TestEntity(uuid, "Test", null);

            // When
            var actual = objectMapper.writeValueAsString(testEntity);

            // Then
            assertThatJson(actual).isObject()
                                  .containsEntry("uuid", uuid.toString())
                                  .containsEntry("string", "Test");
        }

        @Test
        void ReturnsJsonWithEmptyList_EntityWithEmptyList() {
            // Given
            var uuid = UUID.fromString("f47ac10b-58cc-4372-a567-0e02b2c3d479");
            var testEntity = new TestEntity(uuid, "Test", Collections.emptyList());

            // When
            var actual = objectMapper.writeValueAsString(testEntity);

            // Then
            assertThatJson(actual).isObject()
                                  .containsEntry("uuid", uuid.toString())
                                  .containsEntry("string", "Test")
                                  .containsEntry("list", List.of());
        }

        @Test
        void ReturnsJsonDatesInISO8601Format_EntityWithDates() {
            // Given
            var localDate = LocalDate.of(2024, 6, 15);
            var localDateTime = LocalDateTime.of(2024, 6, 15, 14, 23, 40);
            var instant = Instant.parse("2024-06-15T15:14:40.123Z");
            var testEntity = new DateTestEntity(localDate, localDateTime, instant);

            // When
            var actual = objectMapper.writeValueAsString(testEntity);

            // Then
            assertThatJson(actual).isObject()
                                  .containsEntry("localDate", "2024-06-15")
                                  .containsEntry("localDateTime", "2024-06-15T14:23:40")
                                  .containsEntry("instant", "2024-06-15T15:14:40.123Z");
        }

    }

    @Nested
    class DeserializeJsonToEntity {

        @Test
        void ReturnsEntity_RawJsonWithValidDates() throws JacksonException {
            // Given
            var localDateAsJson = """
                    {"localDate":"2024-06-15","localDateTime":"2024-06-15T14:23:40","instant":"2024-06-15T15:14:40.123Z"}
                    """.trim();
            var localDate = LocalDate.of(2024, 6, 15);
            var localDateTime = LocalDateTime.of(2024, 6, 15, 14, 23, 40);
            var instant = Instant.parse("2024-06-15T15:14:40.123Z");
            var dateTestEntity = new DateTestEntity(localDate, localDateTime, instant);

            // When
            var actual = objectMapper.readValue(localDateAsJson, DateTestEntity.class);

            // Then
            assertThat(actual).isEqualTo(dateTestEntity);
        }

        @Test
        void ThrowsInvalidFormatException_RawJsonWithWrongDateFormat() {
            // Given
            var localDateAsJson = """
                    {"localDate":"15/06/2024"}
                    """.trim();

            // When
            var actual = catchThrowable(() -> objectMapper.readValue(localDateAsJson, DateTestEntity.class));

            // Then
            assertThat(actual).isInstanceOf(InvalidFormatException.class);
        }

        @Test
        void ThrowsInvalidFormatException_RawJsonWithWrongDateTimeFormat() {
            // Given
            var localDateTimeAsJson = """
                    {"localDateTime":"15/06/2024 14:23:40"}
                    """.trim();

            // When
            var actual = catchThrowable(() -> objectMapper.readValue(localDateTimeAsJson, DateTestEntity.class));

            // Then
            assertThat(actual).isInstanceOf(InvalidFormatException.class);
        }

        @Test
        void ThrowsInvalidFormatException_RawJsonWithWrongInstantFormat() {
            // Given
            var instantAsJson = """
                    {"instant":"15/06/2024 14:23:40"}
                    """.trim();

            // When
            var actual = catchThrowable(() -> objectMapper.readValue(instantAsJson, DateTestEntity.class));

            // Then
            assertThat(actual).isInstanceOf(InvalidFormatException.class);
        }

    }

    // Test Entities

    record TestEntity(
            UUID uuid,
            String string,
            List<String> list
    ) {}

    record DateTestEntity(
            LocalDate localDate,
            LocalDateTime localDateTime,
            Instant instant
    ) {}

}

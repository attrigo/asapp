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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;

@ExtendWith(SpringExtension.class)
@TestPropertySource(locations = "classpath:application.properties")
@AutoConfigureWebMvc
class JacksonConfigurationIT {

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    class SerializeEntityToJson {

        @Test
        void SerializesJsonWithoutNullValues_EntityHasNullValues() throws Exception {
            // Given
            var uuid = UUID.randomUUID();
            var testEntity = new TestEntity(uuid, "Test", null);

            // When
            var actualJson = objectMapper.writeValueAsString(testEntity);

            // Then
            assertThatJson(actualJson).isObject()
                                      .containsEntry("uuid", uuid.toString())
                                      .containsEntry("string", "Test");
        }

        @Test
        void SerializesJsonWithEmptyList_EntityHasEmptyList() throws Exception {
            // Given
            var uuid = UUID.randomUUID();
            var testEntity = new TestEntity(uuid, "Test", Collections.emptyList());

            // When
            var actualJson = objectMapper.writeValueAsString(testEntity);

            // Then
            assertThatJson(actualJson).isObject()
                                      .containsEntry("uuid", uuid.toString())
                                      .containsEntry("string", "Test")
                                      .containsEntry("list", List.of());
        }

        @Test
        void SerializesJsonDatesInISO8601Format_EntityHasDates() throws Exception {
            // Given
            var localDate = LocalDate.of(2024, 6, 15);
            var localDateTime = LocalDateTime.of(2024, 6, 15, 14, 23, 40);
            var instant = Instant.parse("2024-06-15T15:14:40.123Z");
            var testEntity = new DateTestEntity(localDate, localDateTime, instant);

            // When
            var actualJson = objectMapper.writeValueAsString(testEntity);

            // Then
            assertThatJson(actualJson).isObject()
                                      .containsEntry("localDate", "2024-06-15")
                                      .containsEntry("localDateTime", "2024-06-15T14:23:40")
                                      .containsEntry("instant", "2024-06-15T15:14:40.123Z");
        }

    }

    @Nested
    class DeserializeJsonToEntity {

        @Test
        void ThrowsInvalidFormatException_RawJsonHasWrongDateFormat() {
            // Given
            var localDateAsJson = """
                    {"localDate":"15/06/2024"}
                    """.trim();

            // When
            Throwable thrown = catchThrowable(() -> objectMapper.readValue(localDateAsJson, DateTestEntity.class));

            // Then
            assertThat(thrown).isInstanceOf(InvalidFormatException.class);
        }

        @Test
        void ThrowsInvalidFormatException_RawJsonHasWrongDateTimeFormat() {
            // Given
            var localDateAsJson = """
                    {"localDateTime":"15/06/2024 14:23:40"}
                    """.trim();

            // When
            Throwable thrown = catchThrowable(() -> objectMapper.readValue(localDateAsJson, DateTestEntity.class));

            // Then
            assertThat(thrown).isInstanceOf(InvalidFormatException.class);
        }

        @Test
        void ThrowsInvalidFormatException_RawJsonHasWrongInstantFormat() {
            // Given
            var localDateAsJson = """
                    {"instant":"15/06/2024 14:23:40"}
                    """.trim();

            // When
            Throwable thrown = catchThrowable(() -> objectMapper.readValue(localDateAsJson, DateTestEntity.class));

            // Then
            assertThat(thrown).isInstanceOf(InvalidFormatException.class);
        }

        @Test
        void DeserializesRawJsonToEntity_RawJsonHasValidDates() throws JsonProcessingException {
            // Given
            var localDateAsJson = """
                    {"localDate":"2024-06-15","localDateTime":"2024-06-15T14:23:40","instant":"2024-06-15T15:14:40.123Z"}
                    """.trim();

            // When
            var actualEntity = objectMapper.readValue(localDateAsJson, DateTestEntity.class);

            // Then
            var localDate = LocalDate.of(2024, 6, 15);
            var localDateTime = LocalDateTime.of(2024, 6, 15, 14, 23, 40);
            var instant = Instant.parse("2024-06-15T15:14:40.123Z");
            var expectedEntity = new DateTestEntity(localDate, localDateTime, instant);
            assertThat(actualEntity).isEqualTo(expectedEntity);
        }

    }

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

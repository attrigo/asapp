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

package com.bcn.asapp.projects.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
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

    @Test
    @DisplayName("GIVEN entity with null values WHEN convert the entity to Json THEN Json does not contain the fields with null values")
    void EntityWithNullValues_ConvertEntityToJson_JsonNotContainsNullValuesFields() throws Exception {
        // Given
        var uuid = UUID.randomUUID();
        var testEntity = new TestEntity(uuid, "Test", null);

        // When
        var actualJson = objectMapper.writeValueAsString(testEntity);

        // Then
        var expectedJson = """
                {"uuid":"%s","string":"Test"}
                """.formatted(uuid.toString())
                   .trim();
        assertEquals(expectedJson, actualJson);
    }

    @Test
    @DisplayName("GIVEN entity with empty list WHEN convert the entity to Json THEN Json contains the empty list")
    void EntityWithEmptyList_ConvertEntityToJson_JsonNotContainsEmptyList() throws Exception {
        // Given
        var uuid = UUID.randomUUID();
        var testEntity = new TestEntity(uuid, "Test", Collections.emptyList());

        // When
        var actualJson = objectMapper.writeValueAsString(testEntity);

        // Then
        var expectedJson = """
                {"uuid":"%s","string":"Test","list":[]}
                """.formatted(uuid.toString())
                   .trim();
        assertEquals(expectedJson, actualJson);
    }

    @Test
    @DisplayName("GIVEN entity with date fields WHEN convert the entity to Json THEN Json contains dates in ISO-8601 format")
    void EntityWithDateFields_ConvertEntityToJson_JsonContainsDatesInISO8601Format() throws Exception {
        // Given
        var localDate = LocalDate.of(2024, 6, 15);
        var localDateTime = LocalDateTime.of(2024, 6, 15, 14, 23, 40);
        var instant = Instant.parse("2024-06-15T15:14:40.123Z");
        var testEntity = new DateTestEntity(localDate, localDateTime, instant);

        // When
        var actualJson = objectMapper.writeValueAsString(testEntity);

        // Then
        var expectedJson = """
                {"localDate":"2024-06-15","localDateTime":"2024-06-15T14:23:40","instant":"2024-06-15T15:14:40.123Z"}
                """.trim();
        assertEquals(expectedJson, actualJson);
    }

    @Test
    @DisplayName("GIVEN Json has wrong date format WHEN convert the Json to LocalDate THEN throws InvalidFormatException")
    void JsonHasWrongDateFormat_ConvertJsonToLocalDate_ThrowsInvalidFormatException() {
        // Given
        var localDateAsJson = """
                {"localDate":"15/06/2024"}
                """.trim();

        // When
        Executable executable = () -> objectMapper.readValue(localDateAsJson, DateTestEntity.class);

        // Then
        assertThrows(InvalidFormatException.class, executable);
    }

    @Test
    @DisplayName("GIVEN Json has wrong date format WHEN convert the Json to LocalDateTime THEN throws InvalidFormatException")
    void JsonHasWrongDateFormat_ConvertJsonToLocalDateTime_ThrowsInvalidFormatException() {
        // Given
        var localDateAsJson = """
                {"localDateTime":"15/06/2024 14:23:40"}
                """.trim();

        // When
        Executable executable = () -> objectMapper.readValue(localDateAsJson, DateTestEntity.class);

        // Then
        assertThrows(InvalidFormatException.class, executable);
    }

    @Test
    @DisplayName("GIVEN Json has wrong date format WHEN convert the Json to Instant THEN throws InvalidFormatException")
    void JsonHasWrongDateFormat_ConvertJsonToInstant_ThrowsInvalidFormatException() {
        // Given
        var localDateAsJson = """
                {"instant":"15/06/2024 14:23:40"}
                """.trim();

        // When
        Executable executable = () -> objectMapper.readValue(localDateAsJson, DateTestEntity.class);

        // Then
        assertThrows(InvalidFormatException.class, executable);
    }

    @Test
    @DisplayName("GIVEN Json has valid dates formats WHEN convert the Json to dates THEN converts the Json to dates")
    void JsonHasValidDatesFormats_ConvertJsonToDates_ConvertsJsonToDates() throws JsonProcessingException {
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
        var entityExpected = new DateTestEntity(localDate, localDateTime, instant);
        assertEquals(entityExpected, actualEntity);
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

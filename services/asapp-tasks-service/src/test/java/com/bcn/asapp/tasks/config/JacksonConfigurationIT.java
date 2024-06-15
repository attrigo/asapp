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
package com.bcn.asapp.tasks.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(SpringExtension.class)
@AutoConfigureWebMvc
@TestPropertySource(locations = "classpath:application.properties")
class JacksonConfigurationIT {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GIVEN entity with null values WHEN convert the entity to Json THEN Json does not contain the fields with null values")
    void EntityWithNullValues_ConvertEntityToJson_JsonNotContainsNullValuesFields() throws Exception {
        var uuid = UUID.randomUUID();
        var testEntity = new TestEntity(uuid, "Test", null);

        var actualJson = objectMapper.writeValueAsString(testEntity);

        var expectedJson = """
                {"uuid":"%s","string":"Test"}
                """.formatted(uuid.toString())
                   .trim();

        assertEquals(expectedJson, actualJson);
    }

    @Test
    @DisplayName("GIVEN entity with empty list WHEN convert the entity to Json THEN Json contains the empty list")
    void EntityWithEmptyList_ConvertEntityToJson_JsonNotContainsEmptyList() throws Exception {
        var uuid = UUID.randomUUID();
        var testEntity = new TestEntity(uuid, "Test", Collections.emptyList());

        var actualJson = objectMapper.writeValueAsString(testEntity);

        var expectedJson = """
                {"uuid":"%s","string":"Test","list":[]}
                """.formatted(uuid.toString())
                   .trim();

        assertEquals(expectedJson, actualJson);
    }

    @Test
    @DisplayName("GIVEN entity with date fields WHEN convert the entity to Json THEN Json contains dates in ISO-8601 format")
    void EntityWithDateFields_ConvertEntityToJson_JsonContainsDatesInISO8601Format() throws Exception {
        var localDate = LocalDate.of(2024, 6, 15);
        var localDateTime = LocalDateTime.of(2024, 6, 15, 14, 23, 40);
        var instant = Instant.parse("2024-06-15T15:14:40.123Z");
        var testEntity = new DateTestEntity(localDate, localDateTime, instant);

        var actualJson = objectMapper.writeValueAsString(testEntity);

        var expectedJson = """
                {"localDate":"2024-06-15","localDateTime":"2024-06-15T14:23:40","instant":"2024-06-15T15:14:40.123Z"}
                """.trim();

        assertEquals(expectedJson, actualJson);
    }

    record TestEntity(UUID uuid, String string, List<String> list) {}

    record DateTestEntity(LocalDate localDate, LocalDateTime localDateTime, Instant instant) {}

}

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

package com.bcn.asapp.clients.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

import java.util.stream.Stream;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

class DefaultUriHandlerTests {

    private static final String BASE_URL = "http://localhost:8081/asapp-service";

    @Nested
    class CreateDefaultUriHandler {

        @ParameterizedTest
        @NullAndEmptySource
        void ThenThrowsIllegalArgumentException_GivenBaseUriIsNullOrEmpty(String baseUri) {
            // When
            var thrown = catchThrowable(() -> new DefaultUriHandler(baseUri));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Base URI must not be null or blank");
        }

        @ParameterizedTest
        @MethodSource("com.bcn.asapp.clients.util.DefaultUriHandlerTests#provideBlankStrings")
        void ThenThrowsIllegalArgumentException_GivenBaseUriIsBlank(String baseUri) {
            // When
            var thrown = catchThrowable(() -> new DefaultUriHandler(baseUri));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Base URI must not be null or blank");
        }

        @Test
        void ThenReturnsUriHandler_GivenBaseUriIsValid() {
            // When
            var actual = new DefaultUriHandler(BASE_URL);

            // Then
            assertThat(actual).isNotNull();
        }

    }

    @Nested
    class NewInstance {

        @Test
        void ThenReturnsNewUriBuilder_GivenFirstCall() {
            // Given
            var uriHandler = new DefaultUriHandler(BASE_URL);

            // When
            var actual = uriHandler.newInstance();

            // Then
            assertThat(actual).isNotNull();
        }

        @Test
        void ThenReturnsDifferentInstances_GivenMultipleCalls() {
            // Given
            var uriHandler = new DefaultUriHandler(BASE_URL);
            var firstInstance = uriHandler.newInstance();

            // When
            var secondInstance = uriHandler.newInstance();

            // Then
            assertThat(secondInstance).isNotNull()
                                      .isNotSameAs(firstInstance);
        }

        @Test
        void ThenBuildsUriWithBasePath_GivenNoAdditionalPath() {
            // Given
            var uriHandler = new DefaultUriHandler(BASE_URL);

            // When
            var actual = uriHandler.newInstance()
                                   .build();

            // Then
            assertThat(actual).hasToString(BASE_URL);
        }

        @Test
        void ThenBuildsUriWithAppendedPath_GivenAdditionalPath() {
            // Given
            var uriHandler = new DefaultUriHandler(BASE_URL);
            var path = "/api/users";

            // When
            var actual = uriHandler.newInstance()
                                   .path(path)
                                   .build();

            // Then
            assertThat(actual).hasToString(BASE_URL + path);
        }

        @Test
        void ThenBuildsUriWithPathVariable_GivenPathTemplate() {
            // Given
            var uriHandler = new DefaultUriHandler(BASE_URL);
            var pathTemplate = "/api/users/{id}";
            var userId = "123";

            // When
            var actual = uriHandler.newInstance()
                                   .path(pathTemplate)
                                   .build(userId);

            // Then
            assertThat(actual).hasToString(BASE_URL + "/api/users/123");
        }

        @Test
        void ThenBuildsUriWithQueryParams_GivenQueryParameters() {
            // Given
            var uriHandler = new DefaultUriHandler(BASE_URL);
            var path = "/api/users";

            // When
            var actual = uriHandler.newInstance()
                                   .path(path)
                                   .queryParam("page", "1")
                                   .queryParam("size", "10")
                                   .build();

            // Then
            assertThat(actual).hasToString(BASE_URL + path + "?page=1&size=10");
        }

    }

    private static Stream<String> provideBlankStrings() {
        return Stream.of(" ", "  ", "\t", "\n", "\r", "   \t\n");
    }

}

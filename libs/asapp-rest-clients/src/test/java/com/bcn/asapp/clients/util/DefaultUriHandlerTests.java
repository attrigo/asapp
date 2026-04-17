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

package com.bcn.asapp.clients.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

import java.util.stream.Stream;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

/**
 * Tests {@link DefaultUriHandler} URI construction, path appending, and template resolution.
 * <p>
 * Coverage:
 * <li>Rejects null, empty, or blank base URIs with validation exception</li>
 * <li>Returns new builder instances for each invocation (factory pattern)</li>
 * <li>Constructs URIs with base path only when no additional paths provided</li>
 * <li>Appends paths to base URI maintaining proper structure</li>
 * <li>Resolves path templates with variable substitution</li>
 * <li>Builds URIs with query parameters maintaining proper encoding</li>
 */
class DefaultUriHandlerTests {

    private static final String BASE_URL = "http://localhost:8081/asapp-service";

    @Nested
    class CreateDefaultUriHandler {

        @Test
        void ReturnsUriHandler_ValidBaseUri() {
            // When
            var actual = new DefaultUriHandler(BASE_URL);

            // Then
            assertThat(actual).isNotNull();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @MethodSource("com.bcn.asapp.clients.util.DefaultUriHandlerTests#provideBlankStrings")
        void ThrowsIllegalArgumentException_NullEmptyOrBlankBaseUri(String baseUri) {
            // When
            var actual = catchThrowable(() -> new DefaultUriHandler(baseUri));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Base URI must not be null or blank");
        }

    }

    @Nested
    class NewInstance {

        @Test
        void ReturnsNewUriBuilder_FirstCall() {
            // Given
            var uriHandler = new DefaultUriHandler(BASE_URL);

            // When
            var actual = uriHandler.newInstance();

            // Then
            assertThat(actual).isNotNull();
        }

        @Test
        void ReturnsDifferentInstances_MultipleCalls() {
            // Given
            var uriHandler = new DefaultUriHandler(BASE_URL);
            var instance = uriHandler.newInstance();

            // When
            var actual = uriHandler.newInstance();

            // Then
            assertThat(actual).isNotNull()
                              .isNotSameAs(instance);
        }

        @Test
        void ReturnsUriWithBasePath_MissingAdditionalPath() {
            // Given
            var uriHandler = new DefaultUriHandler(BASE_URL);

            // When
            var actual = uriHandler.newInstance()
                                   .build();

            // Then
            assertThat(actual).hasToString(BASE_URL);
        }

        @Test
        void ReturnsUriWithAppendedPath_AdditionalPath() {
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
        void ReturnsUriWithPathVariable_PathTemplate() {
            // Given
            var uriHandler = new DefaultUriHandler(BASE_URL);
            var path = "/api/users/{id}";
            var userId = "123";

            // When
            var actual = uriHandler.newInstance()
                                   .path(path)
                                   .build(userId);

            // Then
            assertThat(actual).hasToString(BASE_URL + "/api/users/123");
        }

        @Test
        void ReturnsUriWithQueryParams_QueryParameters() {
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

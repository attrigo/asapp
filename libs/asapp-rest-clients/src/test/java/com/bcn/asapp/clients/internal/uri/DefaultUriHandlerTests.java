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
package com.bcn.asapp.clients.internal.uri;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DefaultUriHandlerTests {

    private UriHandler uriHandler;

    @BeforeEach
    void beforeEach() {
        uriHandler = new DefaultUriHandler("http://localhost:8081/");
    }

    // newInstance
    @Test
    @DisplayName("GIVEN is the first UriBuilder requested WHEN new instance THEN returns a new UriBuilder instance")
    void IsFirstUriBuilderRequested_NewInstance_ReturnsNewUriBuilderInstance() {
        // When
        var actualUriBuilderInstance = uriHandler.newInstance();

        // Then
        assertNotNull(actualUriBuilderInstance);
    }

    @Test
    @DisplayName("GIVEN is not the first UriBuilder requested WHEN new instance THEN returns another UriBuilder instance")
    void IsNotFirstUriBuilderRequested_NewInstance_ReturnsAnotherUriBuilderInstance() {
        // Given
        var unexpectedUriBuilderInstance = uriHandler.newInstance();

        // When
        var actualUriBuilderInstance = uriHandler.newInstance();

        // Then
        assertNotNull(actualUriBuilderInstance);
        assertNotEquals(unexpectedUriBuilderInstance, actualUriBuilderInstance);
    }

}

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

package com.bcn.asapp.tasks;

import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;

/**
 * Tests {@link AsappTasksServiceApplication} Spring Boot entry point.
 * <p>
 * Coverage:
 * <li>Delegates application startup to Spring Boot runtime with no arguments</li>
 * <li>Delegates application startup to Spring Boot runtime forwarding command-line arguments</li>
 */
class AsappTasksServiceApplicationTests {

    @Nested
    class Main {

        @Test
        void StartsApplication_NoArgs() {
            try (var mockedStatic = mockStatic(SpringApplication.class)) {
                // When
                AsappTasksServiceApplication.main(new String[] {});

                // Then
                mockedStatic.verify(() -> SpringApplication.run(AsappTasksServiceApplication.class, new String[] {}));
            }
        }

        @Test
        void StartsApplication_WithArgs() {
            // Given
            var args = new String[] { "--server.port=8081" };

            try (var mockedStatic = mockStatic(SpringApplication.class)) {
                // When
                AsappTasksServiceApplication.main(args);

                // Then
                mockedStatic.verify(() -> SpringApplication.run(AsappTasksServiceApplication.class, args));
            }
        }

    }

}

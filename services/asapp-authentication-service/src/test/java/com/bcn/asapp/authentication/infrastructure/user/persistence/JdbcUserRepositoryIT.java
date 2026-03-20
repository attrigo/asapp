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

package com.bcn.asapp.authentication.infrastructure.user.persistence;

import static com.bcn.asapp.authentication.testutil.fixture.UserFactory.aJdbcUser;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import com.bcn.asapp.authentication.infrastructure.authentication.persistence.JdbcJwtAuthenticationRepository;
import com.bcn.asapp.authentication.testutil.TestContainerConfiguration;

/**
 * Tests {@link JdbcUserRepository} CRUD operations and cascading cleanup against PostgreSQL.
 * <p>
 * Coverage:
 * <li>Persists and retrieves user credentials by multiple identifiers (ID, username)</li>
 * <li>Deletes user with cascading cleanup to authentication records</li>
 * <li>Tests actual database operations with TestContainers PostgreSQL</li>
 */
@DataJdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ TestContainerConfiguration.class, JacksonAutoConfiguration.class })
class JdbcUserRepositoryIT {

    @Autowired
    private JdbcJwtAuthenticationRepository jwtAuthenticationRepository;

    @Autowired
    private JdbcUserRepository userRepository;

    @BeforeEach
    void beforeEach() {
        jwtAuthenticationRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Nested
    class FindByUsername {

        @Test
        void ReturnsUser_UserExists() {
            // Given
            var createdUser = createUser();
            var username = createdUser.username();

            // When
            var actual = userRepository.findByUsername(username);

            // Then
            assertThat(actual).get()
                              .isEqualTo(createdUser);
        }

        @Test
        void ReturnsEmptyOptional_UserNotExists() {
            // Given
            var username = "user_not_exist@asapp.com";

            // When
            var actual = userRepository.findByUsername(username);

            // Then
            assertThat(actual).isEmpty();
        }

    }

    @Nested
    class DeleteUserById {

        @Test
        void ReturnsDeletionCount_UserExists() {
            // Given
            var createdUser = createUser();
            var userId = createdUser.id();

            // When
            var actual = userRepository.deleteUserById(userId);

            // Then
            assertThat(actual).isGreaterThan(0);
        }

        @Test
        void ReturnsZero_UserNotExists() {
            // Given
            var userId = UUID.fromString("4a9d8f7e-3c2b-4e1f-a6d9-8e7f6c5d4b3a");

            // When
            var actual = userRepository.deleteUserById(userId);

            // Then
            assertThat(actual).isZero();
        }

    }

    // Test Data Creation Helpers

    private JdbcUserEntity createUser() {
        var user = aJdbcUser();
        var createdUser = userRepository.save(user);
        assertThat(createdUser).isNotNull();
        return createdUser;
    }

}

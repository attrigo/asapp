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

package com.bcn.asapp.users.infrastructure.user.persistence;

import static com.bcn.asapp.users.testutil.fixture.UserFactory.aJdbcUser;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import com.bcn.asapp.users.testutil.TestContainerConfiguration;

/**
 * Tests {@link JdbcUserRepository} delete operations against PostgreSQL.
 * <p>
 * Coverage:
 * <li>Deletes user by identifier returning zero when not found</li>
 * <li>Deletes user by identifier returning count when successfully deleted</li>
 * <li>Tests actual database operations with TestContainers PostgreSQL</li>
 */
@DataJdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestContainerConfiguration.class)
class JdbcUserRepositoryIT {

    @Autowired
    private JdbcUserRepository userRepository;

    @BeforeEach
    void beforeEach() {
        userRepository.deleteAll();
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
            var userId = UUID.fromString("9c3a7f0e-4d2b-4e8a-9f1c-5b6d7e8f9a0b");

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

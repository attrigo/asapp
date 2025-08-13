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
package com.bcn.asapp.uaa.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@DataJdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class UserRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>(DockerImageName.parse("postgres:latest"));

    @Autowired
    private UserRepository userRepository;

    private String fakeUserUsername;

    private String fakeUserPassword;

    private Role fakeUserRole;

    @BeforeEach
    void beforeEach() {
        userRepository.deleteAll();

        fakeUserUsername = "TEST USERNAME";
        fakeUserPassword = "TEST PASSWORD";
        fakeUserRole = Role.USER;
    }

    @Nested
    class FindByUsername {

        @Test
        @DisplayName("GIVEN username does not exists WHEN find a user by username THEN does not find any user And returns an empty optional")
        void UsernameNotExists_FindByUsername_DoesNotFindUserAndReturnsEmptyOptional() {
            // When
            var usernameToFind = fakeUserUsername;

            var actualUser = userRepository.findByUsername(usernameToFind);

            // Then
            assertTrue(actualUser.isEmpty());
        }

        @Test
        @DisplayName("GIVEN username exists WHEN find a user by username THEN finds the user And returns the user found")
        void UsernameExists_FindByUsername_FindsTasksAndReturnsTasksFound() {
            // Given
            var fakeUser = new User(null, fakeUserUsername, fakeUserPassword, fakeUserRole);
            var userToBeFound = userRepository.save(fakeUser);
            assertNotNull(userToBeFound);

            // When
            var usernameToFind = fakeUserUsername;

            var actualUser = userRepository.findByUsername(usernameToFind);

            // Then
            assertTrue(actualUser.isPresent());
            assertEquals(userToBeFound, actualUser.get());
        }

    }

    @Nested
    class DeleteUserById {

        @Test
        @DisplayName("GIVEN user id not exists WHEN delete a user by id THEN does not delete the user And returns zero")
        void UserIdNotExists_DeleteUserById_DoesNotDeleteUserAndReturnsZero() {
            // When
            var idToDelete = UUID.randomUUID();

            var actual = userRepository.deleteUserById(idToDelete);

            // Then
            assertEquals(0L, actual);
        }

        @Test
        @DisplayName("GIVEN user id exists WHEN delete a user by id THEN deletes the user And returns the amount of users deleted")
        void UserIdExists_DeleteUserById_DeletesUserAndReturnsAmountOfUsersDeleted() {
            // Given
            var fakeUser = new User(null, fakeUserUsername, fakeUserPassword, fakeUserRole);
            var userToBeDeleted = userRepository.save(fakeUser);
            assertNotNull(userToBeDeleted);

            // When
            var idToDelete = userToBeDeleted.id();

            var actual = userRepository.deleteUserById(idToDelete);

            // Then
            assertEquals(1L, actual);

            assertFalse(userRepository.findById(userToBeDeleted.id())
                                      .isPresent());
        }

    }

}

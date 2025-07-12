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
package com.bcn.asapp.uaa.security.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
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

import com.bcn.asapp.uaa.testutil.JwtFaker;

@Testcontainers(disabledWithoutDocker = true)
@DataJdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AccessTokenRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>(DockerImageName.parse("postgres:latest"));

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccessTokenRepository accessTokenRepository;

    private JwtFaker jwtFaker;

    private String fakeUserUsername;

    private String fakeUserPassword;

    private Role fakeUserRole;

    private String fakeAccessTokenJwt;

    private Instant fakeAccessTokenCreatedAt;

    private Instant fakeAccessTokenExpiresAt;

    @BeforeEach
    void beforeEach() {
        accessTokenRepository.deleteAll();
        userRepository.deleteAll();

        fakeUserUsername = "TEST USERNAME";
        fakeUserPassword = "TEST PASSWORD";
        fakeUserRole = Role.USER;

        jwtFaker = new JwtFaker();

        fakeAccessTokenJwt = jwtFaker.fakeJwt(JwtType.ACCESS_TOKEN);
        fakeAccessTokenCreatedAt = Instant.now()
                                          .truncatedTo(ChronoUnit.MILLIS);
        fakeAccessTokenExpiresAt = Instant.now()
                                          .truncatedTo(ChronoUnit.MILLIS);
    }

    @Nested
    class FindByUserId {

        @Test
        @DisplayName("GIVEN user id does not exists WHEN find access token by user id THEN does not find any access token And returns an empty optional")
        void UserIdNotExists_FindByUserId_DoesNotFindUserAndReturnsEmptyOptional() {
            // When
            var userIdToFind = UUID.randomUUID();

            var actualUser = accessTokenRepository.findByUserId(userIdToFind);

            // Then
            assertTrue(actualUser.isEmpty());
        }

        @Test
        @DisplayName("GIVEN user id exists WHEN find access token by user id THEN finds the access token And returns the user found")
        void UserIdExists_FindByUserId_FindsTasksAndReturnsTasksFound() {
            // Given
            var fakeUser = new User(null, fakeUserUsername, fakeUserPassword, fakeUserRole);
            var userToBeFound = userRepository.save(fakeUser);
            assertNotNull(userToBeFound);

            var fakeAccessToken = new AccessToken(null, userToBeFound.id(), fakeAccessTokenJwt, fakeAccessTokenCreatedAt, fakeAccessTokenExpiresAt);
            var accessTokenToBeFound = accessTokenRepository.save(fakeAccessToken);
            assertNotNull(accessTokenToBeFound);

            // When
            var userIdToFind = userToBeFound.id();

            var actualAccessToken = accessTokenRepository.findByUserId(userIdToFind);

            // Then
            assertTrue(actualAccessToken.isPresent());
            assertEquals(accessTokenToBeFound, actualAccessToken.get());
        }

    }

    @Nested
    class ExistsByUserIdAndJwt {

        @Test
        @DisplayName("GIVEN access token does not exists WHEN access token exists by user id and Jwt THEN does not find any access token And returns false")
        void AccessTokenNotExists_ExistsByUserIdAndJwt_DoesNotFindAccessTokenAndReturnsEmptyOptional() {
            // When
            var userIdToFInd = UUID.randomUUID();
            var jwtToFind = jwtFaker.fakeJwt(JwtType.ACCESS_TOKEN);

            var actualExists = accessTokenRepository.existsByUserIdAndJwt(userIdToFInd, jwtToFind);

            // Then
            assertFalse(actualExists);
        }

        @Test
        @DisplayName("GIVEN access token exists WHEN access token exists by user id and Jwt THEN finds the access token And returns true")
        void AccessTokenExists_ExistsByUserIdAndJwt_FindsAccessTokenAndReturnsAccessTokenFound() {
            // Given
            var fakeUser = new User(null, fakeUserUsername, fakeUserPassword, fakeUserRole);
            var userToBeFound = userRepository.save(fakeUser);
            assertNotNull(userToBeFound);

            var fakeAccessToken = new AccessToken(null, userToBeFound.id(), fakeAccessTokenJwt, fakeAccessTokenCreatedAt, fakeAccessTokenExpiresAt);
            var accessTokenToBeFound = accessTokenRepository.save(fakeAccessToken);
            assertNotNull(accessTokenToBeFound);

            // When
            var userIdToFind = userToBeFound.id();
            var jwtToFind = fakeAccessTokenJwt;

            var actualAccessToken = accessTokenRepository.existsByUserIdAndJwt(userIdToFind, jwtToFind);

            // Then
            assertTrue(actualAccessToken);
        }

    }

    @Nested
    class DeleteByUserId {

        @Test
        @DisplayName("GIVEN access token does not exists WHEN delete access token by user id THEN does not delete the access token And returns zero")
        void AccessTokenNotExists_DeleteByUserId_DoesNotDeleteAccessTokenAndReturnsZero() {
            // When
            var userIdToDelete = UUID.randomUUID();

            var actualDeleted = accessTokenRepository.deleteByUserId(userIdToDelete);

            // Then
            assertEquals(0L, actualDeleted);
        }

        @Test
        @DisplayName("GIVEN access token exists WHEN delete access token by user id THEN deletes the access token And returns one")
        void AccessTokenExists_DeleteByUserId_DeletesJwtAndReturnsOne() {
            // Given
            var fakeUser = new User(null, fakeUserUsername, fakeUserPassword, fakeUserRole);
            var fakeUsePersisted = userRepository.save(fakeUser);
            assertNotNull(fakeUsePersisted);

            var fakeAccessToken = new AccessToken(null, fakeUsePersisted.id(), fakeAccessTokenJwt, fakeAccessTokenCreatedAt, fakeAccessTokenExpiresAt);
            var accessTokenToBeDeleted = accessTokenRepository.save(fakeAccessToken);
            assertNotNull(accessTokenToBeDeleted);

            // When
            var userIdToDelete = fakeUsePersisted.id();

            var actualDeleted = accessTokenRepository.deleteByUserId(userIdToDelete);

            // Then
            assertEquals(1L, actualDeleted);
        }

    }

}

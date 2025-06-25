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
class RefreshTokenRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>(DockerImageName.parse("postgres:latest"));

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private JwtFaker jwtFaker;

    private String fakeUserUsername;

    private String fakeUserPassword;

    private Role fakeUserRole;

    private String fakeRefreshTokenJwt;

    private Instant fakeRefreshTokenCreatedAt;

    private Instant fakeRefreshTokenExpiresAt;

    @BeforeEach
    void beforeEach() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        jwtFaker = new JwtFaker();

        fakeUserUsername = "TEST USERNAME";
        fakeUserPassword = "TEST PASSWORD";
        fakeUserRole = Role.USER;

        fakeRefreshTokenJwt = jwtFaker.fakeJwt(JwtType.REFRESH_TOKEN);
        fakeRefreshTokenCreatedAt = Instant.now()
                                           .truncatedTo(ChronoUnit.MILLIS);
        fakeRefreshTokenExpiresAt = Instant.now()
                                           .truncatedTo(ChronoUnit.MILLIS);
    }

    @Nested
    class FindByUserId {

        @Test
        @DisplayName("GIVEN user id does not exists WHEN find user by id THEN does not find any user And returns an empty optional")
        void UserIdNotExists_FindByUsername_DoesNotFindUserAndReturnsEmptyOptional() {
            // When
            var userIdToFind = UUID.randomUUID();

            var actualUser = refreshTokenRepository.findByUserId(userIdToFind);

            // Then
            assertTrue(actualUser.isEmpty());
        }

        @Test
        @DisplayName("GIVEN user id exists WHEN find user by id THEN finds the user And returns the user found")
        void UserIdExists_FindByUsername_FindsTasksAndReturnsTasksFound() {
            // Given
            var fakeUser = new User(null, fakeUserUsername, fakeUserPassword, fakeUserRole);
            var userToBeFound = userRepository.save(fakeUser);
            assertNotNull(userToBeFound);

            var fakeRefreshToken = new RefreshToken(null, userToBeFound.id(), fakeRefreshTokenJwt, fakeRefreshTokenCreatedAt, fakeRefreshTokenExpiresAt);
            var refreshTokenToBeFound = refreshTokenRepository.save(fakeRefreshToken);
            assertNotNull(refreshTokenToBeFound);

            // When
            var userIdToFind = userToBeFound.id();

            var actualRefreshToken = refreshTokenRepository.findByUserId(userIdToFind);

            // Then
            assertTrue(actualRefreshToken.isPresent());
            assertEquals(refreshTokenToBeFound, actualRefreshToken.get());
        }

    }

    @Nested
    class ExistsByUserIdAndJwt {

        @Test
        @DisplayName("GIVEN refresh token does not exists WHEN exists by user id and Jwt THEN does not find any refresh token And returns false")
        void RefreshTokenNotExists_ExistsByUserIdAndJwt_DoesNotFindRefreshTokenAndReturnsEmptyOptional() {
            // When
            var userIdToFInd = UUID.randomUUID();
            var jwtToFind = jwtFaker.fakeJwt(JwtType.REFRESH_TOKEN);

            var actualExists = refreshTokenRepository.existsByUserIdAndJwt(userIdToFInd, jwtToFind);

            // Then
            assertFalse(actualExists);
        }

        @Test
        @DisplayName("GIVEN refresh token exists WHEN exists by user id and Jwt THEN finds the refresh token And returns true")
        void RefreshTokenExists_ExistsByUserIdAndJwt_FindsRefreshTokenAndReturnsRefreshTokenFound() {
            // Given
            var fakeUser = new User(null, fakeUserUsername, fakeUserPassword, fakeUserRole);
            var userToBeFound = userRepository.save(fakeUser);
            assertNotNull(userToBeFound);

            var fakeRefreshToken = new RefreshToken(null, userToBeFound.id(), fakeRefreshTokenJwt, fakeRefreshTokenCreatedAt, fakeRefreshTokenExpiresAt);
            var refreshTokenToBeFound = refreshTokenRepository.save(fakeRefreshToken);
            assertNotNull(refreshTokenToBeFound);

            // When
            var userIdToFind = userToBeFound.id();
            var jwtToFind = fakeRefreshTokenJwt;

            var actualRefreshToken = refreshTokenRepository.existsByUserIdAndJwt(userIdToFind, jwtToFind);

            // Then
            assertTrue(actualRefreshToken);
        }

    }

}

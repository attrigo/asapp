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

package com.bcn.asapp.uaa.security.authentication.revoker;

import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.bcn.asapp.uaa.security.authentication.JwtIntegrityViolationException;
import com.bcn.asapp.uaa.security.core.AccessToken;
import com.bcn.asapp.uaa.security.core.AccessTokenRepository;
import com.bcn.asapp.uaa.security.core.JwtType;
import com.bcn.asapp.uaa.security.core.RefreshToken;
import com.bcn.asapp.uaa.security.core.RefreshTokenRepository;
import com.bcn.asapp.uaa.testutil.JwtFaker;
import com.bcn.asapp.uaa.user.Role;
import com.bcn.asapp.uaa.user.User;
import com.bcn.asapp.uaa.user.UserRepository;

@DataJdbcTest
@TestPropertySource(locations = "classpath:application.properties")
@Import(JwtRevoker.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
@Transactional(propagation = Propagation.NEVER)
class JwtRevokerIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>(DockerImageName.parse("postgres:latest"));

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccessTokenRepository accessTokenRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private JwtRevoker jwtRevoker;

    private JwtFaker jwtFaker;

    private String fakeUsername;

    private String fakePassword;

    private String fakePasswordBcryptEncoded;

    @BeforeEach
    void beforeEach() {
        accessTokenRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        this.jwtFaker = new JwtFaker();

        this.fakeUsername = "TEST USERNAME";
        this.fakePassword = "TEST PASSWORD";
        this.fakePasswordBcryptEncoded = "{bcrypt}" + new BCryptPasswordEncoder().encode(fakePassword);
    }

    @Nested
    class RevokeAuthenticationByAuthentication {

        @Test
        @DisplayName("GIVEN user not exists WHEN revoke an authentication THEN does not revoke the authentication And throws UsernameNotFoundException")
        void UserNotExists_RevokeAuthentication_DoesNotRevokeAuthenticationAndThrowsUsernameNotFoundException() {
            // When
            var authentication = new UsernamePasswordAuthenticationToken(fakeUsername, fakePassword, List.of(new SimpleGrantedAuthority("USER")));

            Executable executable = () -> jwtRevoker.revokeAuthentication(authentication);

            // Then
            var exceptionThrown = assertThrows(UsernameNotFoundException.class, executable);
            assertEquals("User not exists by username " + fakeUsername, exceptionThrown.getMessage());
        }

        @Test
        @DisplayName("GIVEN user does not have access token WHEN revoke an authentication THEN does not revoke the access token")
        void UserHasNotAccessToken_RevokeAuthentication_RevokesAccessToken() {
            // Given
            var fakeUser = new User(null, fakeUsername, fakePasswordBcryptEncoded, Role.USER);
            var fakeUserSaved = userRepository.save(fakeUser);
            assertNotNull(fakeUserSaved);

            var fakeRefreshJwt = jwtFaker.fakeJwt(JwtType.REFRESH_TOKEN);
            var fakeRefreshToken = new RefreshToken(null, fakeUserSaved.id(), fakeRefreshJwt, Instant.now(), Instant.now());
            var refreshTokenSaved = refreshTokenRepository.save(fakeRefreshToken);
            assertNotNull(refreshTokenSaved);

            // When
            var authentication = new UsernamePasswordAuthenticationToken(fakeUsername, fakePassword, List.of(new SimpleGrantedAuthority("USER")));

            jwtRevoker.revokeAuthentication(authentication);

            // Then
            var actualAccessToken = accessTokenRepository.findByUserId(fakeUserSaved.id());
            assertTrue(actualAccessToken.isEmpty());
            var actualRefreshToken = refreshTokenRepository.findByUserId(fakeUserSaved.id());
            assertTrue(actualRefreshToken.isEmpty());
        }

        @Test
        @DisplayName("GIVEN user does not have refresh token WHEN revoke an authentication THEN does not revoke the refresh token")
        void UserHasNotRefreshToken_RevokeAuthentication_DoesNotRevokeRefreshToken() {
            // Given
            var fakeUser = new User(null, fakeUsername, fakePasswordBcryptEncoded, Role.USER);
            var fakeUserSaved = userRepository.save(fakeUser);
            assertNotNull(fakeUserSaved);

            var fakeAccessJwt = jwtFaker.fakeJwt(JwtType.ACCESS_TOKEN);
            var fakeAccessToken = new AccessToken(null, fakeUserSaved.id(), fakeAccessJwt, Instant.now(), Instant.now());
            var accessTokenSaved = accessTokenRepository.save(fakeAccessToken);
            assertNotNull(accessTokenSaved);

            // When
            var authentication = new UsernamePasswordAuthenticationToken(fakeUsername, fakePassword, List.of(new SimpleGrantedAuthority("USER")));

            jwtRevoker.revokeAuthentication(authentication);

            // Then
            var actualAccessToken = accessTokenRepository.findByUserId(fakeUserSaved.id());
            assertTrue(actualAccessToken.isEmpty());
            var actualRefreshToken = refreshTokenRepository.findByUserId(fakeUserSaved.id());
            assertTrue(actualRefreshToken.isEmpty());
        }

        @Test
        @DisplayName("GIVEN access token could not be revoked WHEN revoke an authentication THEN do not revoke the tokens And throws JwtIntegrityViolationException")
        void AccessTokenCouldNotBeRevoked_RevokeAuthentication_DoesNotRevokeAuthenticationAndThrowsJwtIntegrityViolationException() {
            // Given
            var fakeUser = new User(null, fakeUsername, fakePasswordBcryptEncoded, Role.USER);
            var fakeUserSaved = userRepository.save(fakeUser);
            assertNotNull(fakeUserSaved);

            var fakeRefreshJwt = jwtFaker.fakeJwt(JwtType.REFRESH_TOKEN);
            var fakeRefreshToken = new RefreshToken(null, fakeUserSaved.id(), fakeRefreshJwt, Instant.now(), Instant.now());
            var refreshTokenSaved = refreshTokenRepository.save(fakeRefreshToken);
            assertNotNull(refreshTokenSaved);

            // Rename the table to cause a database error
            jdbcTemplate.execute("ALTER TABLE access_token RENAME TO access_token_tmp");

            try {
                // When
                var authentication = new UsernamePasswordAuthenticationToken(fakeUsername, fakePassword, List.of(new SimpleGrantedAuthority("USER")));

                Executable executable = () -> jwtRevoker.revokeAuthentication(authentication);

                // Then
                var exceptionThrown = assertThrows(JwtIntegrityViolationException.class, executable);
                assertThat(exceptionThrown.getMessage(), startsWith("Authentication could not be revoked due to:"));
            } finally {
                // Restore the table name
                jdbcTemplate.execute("ALTER TABLE access_token_tmp RENAME TO access_token");
            }
        }

        @Test
        @DisplayName("GIVEN refresh token could not be revoked WHEN revoke an authentication THEN do not revoke the tokens And throws JwtIntegrityViolationException")
        void RefreshTokenCouldNotBeRevoked_RevokeAuthentication_DoesNotRevokeAuthenticationAndThrowsJwtIntegrityViolationException() {
            // Given
            var fakeUser = new User(null, fakeUsername, fakePasswordBcryptEncoded, Role.USER);
            var fakeUserSaved = userRepository.save(fakeUser);
            assertNotNull(fakeUserSaved);

            var fakeAccessJwt = jwtFaker.fakeJwt(JwtType.ACCESS_TOKEN);
            var fakeAccessToken = new RefreshToken(null, fakeUserSaved.id(), fakeAccessJwt, Instant.now(), Instant.now());
            var accessTokenSaved = refreshTokenRepository.save(fakeAccessToken);
            assertNotNull(accessTokenSaved);

            // Rename the table to cause a database error
            jdbcTemplate.execute("ALTER TABLE refresh_token RENAME TO refresh_token_tmp");

            try {
                // When
                var authentication = new UsernamePasswordAuthenticationToken(fakeUsername, fakePassword, List.of(new SimpleGrantedAuthority("USER")));

                Executable executable = () -> jwtRevoker.revokeAuthentication(authentication);

                // Then
                var exceptionThrown = assertThrows(JwtIntegrityViolationException.class, executable);
                assertThat(exceptionThrown.getMessage(), startsWith("Authentication could not be revoked due to:"));
            } finally {
                // Restore the table name
                jdbcTemplate.execute("ALTER TABLE refresh_token_tmp RENAME TO refresh_token");
            }
        }

        @Test
        @DisplayName("GIVEN user has both access and refresh tokens WHEN revoke an authentication THEN revokes both tokens")
        void UserHasBothTokens_RevokeAuthentication_RevokesBothTokens() {
            // Given
            var fakeUser = new User(null, fakeUsername, fakePasswordBcryptEncoded, Role.USER);
            var fakeUserSaved = userRepository.save(fakeUser);
            assertNotNull(fakeUserSaved);

            var fakeAccessJwt = jwtFaker.fakeJwt(JwtType.ACCESS_TOKEN);
            var fakeAccessToken = new AccessToken(null, fakeUserSaved.id(), fakeAccessJwt, Instant.now(), Instant.now());
            var accessTokenSaved = accessTokenRepository.save(fakeAccessToken);
            assertNotNull(accessTokenSaved);

            var fakeRefreshJwt = jwtFaker.fakeJwt(JwtType.REFRESH_TOKEN);
            var fakeRefreshToken = new RefreshToken(null, fakeUserSaved.id(), fakeRefreshJwt, Instant.now(), Instant.now());
            var refreshTokenSaved = refreshTokenRepository.save(fakeRefreshToken);
            assertNotNull(refreshTokenSaved);

            // When
            var authentication = new UsernamePasswordAuthenticationToken(fakeUsername, fakePassword, List.of(new SimpleGrantedAuthority("USER")));

            jwtRevoker.revokeAuthentication(authentication);

            // Then
            var actualAccessToken = accessTokenRepository.findByUserId(fakeUserSaved.id());
            assertTrue(actualAccessToken.isEmpty());
            var actualRefreshToken = refreshTokenRepository.findByUserId(fakeUserSaved.id());
            assertTrue(actualRefreshToken.isEmpty());
        }

    }

    @Nested
    class RevokeAuthenticationByUser {

        @Test
        @DisplayName("GIVEN user does not have access token WHEN revoke an authentication THEN does not revoke the access token")
        void UserHasNotAccessToken_RevokeAuthentication_RevokesAccessToken() {
            // Given
            var fakeUser = new User(null, fakeUsername, fakePasswordBcryptEncoded, Role.USER);
            var fakeUserSaved = userRepository.save(fakeUser);
            assertNotNull(fakeUserSaved);

            var fakeRefreshJwt = jwtFaker.fakeJwt(JwtType.REFRESH_TOKEN);
            var fakeRefreshToken = new RefreshToken(null, fakeUserSaved.id(), fakeRefreshJwt, Instant.now(), Instant.now());
            var refreshTokenSaved = refreshTokenRepository.save(fakeRefreshToken);
            assertNotNull(refreshTokenSaved);

            // When
            jwtRevoker.revokeAuthentication(fakeUserSaved);

            // Then
            var actualAccessToken = accessTokenRepository.findByUserId(fakeUserSaved.id());
            assertTrue(actualAccessToken.isEmpty());
            var actualRefreshToken = refreshTokenRepository.findByUserId(fakeUserSaved.id());
            assertTrue(actualRefreshToken.isEmpty());
        }

        @Test
        @DisplayName("GIVEN user does not have refresh token WHEN revoke an authentication THEN does not revoke the refresh token")
        void UserHasNotRefreshToken_RevokeAuthentication_DoesNotRevokeRefreshToken() {
            // Given
            var fakeUser = new User(null, fakeUsername, fakePasswordBcryptEncoded, Role.USER);
            var fakeUserSaved = userRepository.save(fakeUser);
            assertNotNull(fakeUserSaved);

            var fakeAccessJwt = jwtFaker.fakeJwt(JwtType.ACCESS_TOKEN);
            var fakeAccessToken = new AccessToken(null, fakeUserSaved.id(), fakeAccessJwt, Instant.now(), Instant.now());
            var accessTokenSaved = accessTokenRepository.save(fakeAccessToken);
            assertNotNull(accessTokenSaved);

            // When
            jwtRevoker.revokeAuthentication(fakeUserSaved);

            // Then
            var actualAccessToken = accessTokenRepository.findByUserId(fakeUserSaved.id());
            assertTrue(actualAccessToken.isEmpty());
            var actualRefreshToken = refreshTokenRepository.findByUserId(fakeUserSaved.id());
            assertTrue(actualRefreshToken.isEmpty());
        }

        @Test
        @DisplayName("GIVEN access token could not be revoked WHEN revoke an authentication THEN do not revoke the tokens And throws JwtIntegrityViolationException")
        void AccessTokenCouldNotBeRevoked_RevokeAuthentication_DoesNotRevokeAuthenticationAndThrowsJwtIntegrityViolationException() {
            // Given
            var fakeUser = new User(null, fakeUsername, fakePasswordBcryptEncoded, Role.USER);
            var fakeUserSaved = userRepository.save(fakeUser);
            assertNotNull(fakeUserSaved);

            var fakeRefreshJwt = jwtFaker.fakeJwt(JwtType.REFRESH_TOKEN);
            var fakeRefreshToken = new RefreshToken(null, fakeUserSaved.id(), fakeRefreshJwt, Instant.now(), Instant.now());
            var refreshTokenSaved = refreshTokenRepository.save(fakeRefreshToken);
            assertNotNull(refreshTokenSaved);

            // Rename the table to cause a database error
            jdbcTemplate.execute("ALTER TABLE access_token RENAME TO access_token_tmp");

            try {
                // When
                Executable executable = () -> jwtRevoker.revokeAuthentication(fakeUserSaved);

                // Then
                var exceptionThrown = assertThrows(JwtIntegrityViolationException.class, executable);
                assertThat(exceptionThrown.getMessage(), startsWith("Authentication could not be revoked due to:"));
            } finally {
                // Restore the table name
                jdbcTemplate.execute("ALTER TABLE access_token_tmp RENAME TO access_token");
            }
        }

        @Test
        @DisplayName("GIVEN refresh token could not be revoked WHEN revoke an authentication THEN do not revoke the tokens And throws JwtIntegrityViolationException")
        void RefreshTokenCouldNotBeRevoked_RevokeAuthentication_DoesNotRevokeAuthenticationAndThrowsJwtIntegrityViolationException() {
            // Given
            var fakeUser = new User(null, fakeUsername, fakePasswordBcryptEncoded, Role.USER);
            var fakeUserSaved = userRepository.save(fakeUser);
            assertNotNull(fakeUserSaved);

            var fakeAccessJwt = jwtFaker.fakeJwt(JwtType.ACCESS_TOKEN);
            var fakeAccessToken = new RefreshToken(null, fakeUserSaved.id(), fakeAccessJwt, Instant.now(), Instant.now());
            var accessTokenSaved = refreshTokenRepository.save(fakeAccessToken);
            assertNotNull(accessTokenSaved);

            // Rename the table to cause a database error
            jdbcTemplate.execute("ALTER TABLE refresh_token RENAME TO refresh_token_tmp");

            try {
                // When
                Executable executable = () -> jwtRevoker.revokeAuthentication(fakeUserSaved);

                // Then
                var exceptionThrown = assertThrows(JwtIntegrityViolationException.class, executable);
                assertThat(exceptionThrown.getMessage(), startsWith("Authentication could not be revoked due to:"));
            } finally {
                // Restore the table name
                jdbcTemplate.execute("ALTER TABLE refresh_token_tmp RENAME TO refresh_token");
            }
        }

        @Test
        @DisplayName("GIVEN user has both access and refresh tokens WHEN revoke an authentication THEN revokes both tokens")
        void UserHasBothTokens_RevokeAuthentication_RevokesBothTokens() {
            // Given
            var fakeUser = new User(null, fakeUsername, fakePasswordBcryptEncoded, Role.USER);
            var fakeUserSaved = userRepository.save(fakeUser);
            assertNotNull(fakeUserSaved);

            var fakeAccessJwt = jwtFaker.fakeJwt(JwtType.ACCESS_TOKEN);
            var fakeAccessToken = new AccessToken(null, fakeUserSaved.id(), fakeAccessJwt, Instant.now(), Instant.now());
            var accessTokenSaved = accessTokenRepository.save(fakeAccessToken);
            assertNotNull(accessTokenSaved);

            var fakeRefreshJwt = jwtFaker.fakeJwt(JwtType.REFRESH_TOKEN);
            var fakeRefreshToken = new RefreshToken(null, fakeUserSaved.id(), fakeRefreshJwt, Instant.now(), Instant.now());
            var refreshTokenSaved = refreshTokenRepository.save(fakeRefreshToken);
            assertNotNull(refreshTokenSaved);

            // When
            jwtRevoker.revokeAuthentication(fakeUserSaved);

            // Then
            var actualAccessToken = accessTokenRepository.findByUserId(fakeUserSaved.id());
            assertTrue(actualAccessToken.isEmpty());
            var actualRefreshToken = refreshTokenRepository.findByUserId(fakeUserSaved.id());
            assertTrue(actualRefreshToken.isEmpty());
        }

    }

}

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

package com.bcn.asapp.uaa.security.authentication.issuer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willCallRealMethod;
import static org.mockito.BDDMockito.willReturn;

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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
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
@Import({ JwtIssuer.class, JwtProvider.class })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
@Transactional(propagation = Propagation.NEVER)
class JwtIssuerIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>(DockerImageName.parse("postgres:latest"));

    @MockitoSpyBean
    private JwtProvider jwtProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccessTokenRepository accessTokenRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private JwtIssuer jwtIssuer;

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
    class IssueAuthentication {

        @Test
        @DisplayName("GIVEN user not exists WHEN issue an authentication THEN does not issue the authentication And throws UsernameNotFoundException")
        void UserNotExists_IssueAuthentication_DoesNotIssueAuthenticationAndThrowsUsernameNotFoundException() {
            // When
            var authentication = new UsernamePasswordAuthenticationToken(fakeUsername, fakePassword, List.of(new SimpleGrantedAuthority("USER")));

            Executable executable = () -> jwtIssuer.issueAuthentication(authentication);

            // Then
            var exceptionThrown = assertThrows(UsernameNotFoundException.class, executable);
            assertEquals("User not exists by username " + fakeUsername, exceptionThrown.getMessage());
        }

        @Test
        @DisplayName("GIVEN access token could not be saved WHEN issue an authentication THEN do not save tokens And throws JwtIntegrityViolationException")
        void AccessTokenCouldNotBeSaved_IssueAuthentication_DoesNotSaveTokensAndThrowsJwtIntegrityViolationException() {
            // Given
            var fakeUser = new User(null, fakeUsername, fakePasswordBcryptEncoded, Role.USER);
            var fakeUserSaved = userRepository.save(fakeUser);
            assertNotNull(fakeUserSaved);

            var fakeAccessToken = new AccessToken(null, null, null, null, null);

            willReturn(fakeAccessToken).given(jwtProvider)
                                       .generateAccessToken(any(Authentication.class));

            // When
            var authentication = new UsernamePasswordAuthenticationToken(fakeUsername, fakePassword, List.of(new SimpleGrantedAuthority("USER")));

            Executable executable = () -> jwtIssuer.issueAuthentication(authentication);

            // Then
            assertThrows(JwtIntegrityViolationException.class, executable);

            var actualAccessToken = accessTokenRepository.findByUserId(fakeUserSaved.id());
            assertTrue(actualAccessToken.isEmpty());
            var actualRefreshToken = refreshTokenRepository.findByUserId(fakeUserSaved.id());
            assertTrue(actualRefreshToken.isEmpty());
        }

        @Test
        @DisplayName("GIVEN refresh token could not be saved WHEN issue an authentication THEN do not save tokens And throws JwtIntegrityViolationException")
        void RefreshTokenCouldNotBeSaved_IssueAuthentication_DoesNotSaveTokensAndThrowsJwtIntegrityViolationException() {
            // Given
            var fakeUser = new User(null, fakeUsername, fakePasswordBcryptEncoded, Role.USER);
            var fakeUserSaved = userRepository.save(fakeUser);
            assertNotNull(fakeUserSaved);

            var fakeRefreshToken = new RefreshToken(null, null, null, null, null);

            willCallRealMethod().given(jwtProvider)
                                .generateAccessToken(any(Authentication.class));
            willReturn(fakeRefreshToken).given(jwtProvider)
                                        .generateRefreshToken(any(Authentication.class));

            // When
            var authentication = new UsernamePasswordAuthenticationToken(fakeUsername, fakePassword, List.of(new SimpleGrantedAuthority("USER")));

            Executable executable = () -> jwtIssuer.issueAuthentication(authentication);

            // Then
            assertThrows(JwtIntegrityViolationException.class, executable);

            var actualAccessToken = accessTokenRepository.findByUserId(fakeUserSaved.id());
            assertTrue(actualAccessToken.isEmpty());
            var actualRefreshToken = refreshTokenRepository.findByUserId(fakeUserSaved.id());
            assertTrue(actualRefreshToken.isEmpty());
        }

        @Test
        @DisplayName("GIVEN user has authentication WHEN issue an authentication THEN issues new authentication overriding existing one And returns new authentication")
        void UserHasAuthentication_IssueAuthentication_IssuesNewAuthenticationOverridingExistingOneAndReturnsNewAuthentication() {
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

            var actualAuthentication = jwtIssuer.issueAuthentication(authentication);

            // Then
            assertNotNull(actualAuthentication);
            assertEquals(accessTokenSaved.id(), actualAuthentication.accessToken()
                                                                    .id());
            assertEquals(refreshTokenSaved.id(), actualAuthentication.refreshToken()
                                                                     .id());
            assertNotEquals(accessTokenSaved.jwt(), actualAuthentication.accessToken()
                                                                        .jwt());
            assertNotEquals(refreshTokenSaved.jwt(), actualAuthentication.refreshToken()
                                                                         .jwt());
        }

        @Test
        @DisplayName("GIVEN user has not authentication WHEN issue an authentication THEN issues new authentication And returns new authentication")
        void UserHasNotAuthentication_IssueAuthentication_IssuesNewAuthenticationAndReturnsNewAuthentication() {
            // Given
            var fakeUser = new User(null, fakeUsername, fakePasswordBcryptEncoded, Role.USER);
            var fakeUserSaved = userRepository.save(fakeUser);
            assertNotNull(fakeUserSaved);

            // When
            var authentication = new UsernamePasswordAuthenticationToken(fakeUsername, fakePassword, List.of(new SimpleGrantedAuthority("USER")));

            var actualAuthentication = jwtIssuer.issueAuthentication(authentication);

            // Then
            assertNotNull(actualAuthentication);
            assertNotNull(actualAuthentication.accessToken()
                                              .id());
            assertNotNull(actualAuthentication.refreshToken()
                                              .id());
            assertNotNull(actualAuthentication.accessToken()
                                              .jwt());
            assertNotNull(actualAuthentication.refreshToken()
                                              .jwt());
        }

    }

}

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

import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.relational.core.conversion.DbAction;
import org.springframework.data.relational.core.conversion.DbActionExecutionException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.bcn.asapp.uaa.security.core.AccessToken;
import com.bcn.asapp.uaa.security.core.AccessTokenRepository;
import com.bcn.asapp.uaa.security.core.JwtType;
import com.bcn.asapp.uaa.security.core.RefreshToken;
import com.bcn.asapp.uaa.security.core.RefreshTokenRepository;
import com.bcn.asapp.uaa.security.core.Role;
import com.bcn.asapp.uaa.security.core.User;
import com.bcn.asapp.uaa.security.core.UserRepository;
import com.bcn.asapp.uaa.testutil.JwtFaker;

@ExtendWith(SpringExtension.class)
class JwtIssuerTests {

    @Mock
    private JwtProvider jwtProviderMock;

    @Mock
    private UserRepository userRepositoryMock;

    @Mock
    private AccessTokenRepository accessTokenRepositoryMock;

    @Mock
    private RefreshTokenRepository refreshTokenRepositoryMock;

    @InjectMocks
    private JwtIssuer jwtIssuer;

    private JwtFaker jwtFaker;

    private UUID fakeUserId;

    private String fakeUsername;

    private String fakePassword;

    @BeforeEach
    void beforeEach() {
        this.jwtFaker = new JwtFaker();

        this.fakeUserId = UUID.randomUUID();
        this.fakeUsername = "TEST USERNAME";
        this.fakePassword = "TEST PASSWORD";
    }

    @Nested
    class IssueAuthentication {

        @Test
        @DisplayName("GIVEN user not exists WHEN issue authentication THEN does not issue the authentication And throws UsernameNotFoundException")
        void UserNotExists_IssueAuthentication_DoesNotIssueAuthenticationAndThrowsUsernameNotFoundException() {
            // Given
            given(userRepositoryMock.findByUsername(anyString())).willReturn(Optional.empty());

            // When
            var authentication = new UsernamePasswordAuthenticationToken(fakeUsername, fakePassword);

            Executable executable = () -> jwtIssuer.issueAuthentication(authentication);

            // Then
            var exceptionThrown = assertThrows(UsernameNotFoundException.class, executable);
            assertEquals("User not exists by username " + fakeUsername, exceptionThrown.getMessage());

            then(userRepositoryMock).should(times(1))
                                    .findByUsername(anyString());
            then(jwtProviderMock).should(never())
                                 .generateAccessToken(any(Authentication.class));
            then(jwtProviderMock).should(never())
                                 .generateRefreshToken(any(Authentication.class));
            then(accessTokenRepositoryMock).should(never())
                                           .save(any(AccessToken.class));
            then(refreshTokenRepositoryMock).should(never())
                                            .save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("GIVEN tokens could not be saved  WHEN issue authentication THEN does not issue the authentication And throws UsernameNotFoundException")
        void TokenCouldNotBeSaved_IssueAuthentication_DoesNotIssueAuthenticationAndThrowsDataIntegrityViolationException() {
            // Given
            var fakeUser = new User(fakeUserId, fakeUsername, fakePassword, Role.USER);
            var issuedAT = new AccessToken(null, fakeUserId, jwtFaker.fakeJwt(JwtType.ACCESS_TOKEN), Instant.now(), Instant.now());
            var issuedRT = new RefreshToken(null, fakeUserId, jwtFaker.fakeJwt(JwtType.REFRESH_TOKEN), Instant.now(), Instant.now());
            DbAction<?> fakeDbAction = new DbAction.Insert<>(null, null, null, Map.of(), null);
            given(userRepositoryMock.findByUsername(anyString())).willReturn(Optional.of(fakeUser));
            given(jwtProviderMock.generateAccessToken(any(Authentication.class))).willReturn(issuedAT);
            given(jwtProviderMock.generateRefreshToken(any(Authentication.class))).willReturn(issuedRT);
            given(accessTokenRepositoryMock.save(any(AccessToken.class))).willThrow(
                    new DbActionExecutionException(fakeDbAction, new RuntimeException("TEST EXCEPTION")));

            // When
            var authentication = new UsernamePasswordAuthenticationToken(fakeUsername, fakePassword);

            Executable executable = () -> jwtIssuer.issueAuthentication(authentication);

            // Then
            var exceptionThrown = assertThrows(DataIntegrityViolationException.class, executable);
            assertThat(exceptionThrown.getMessage(), startsWith("Authentication could not be issued due to:"));
        }

        @Test
        @DisplayName("GIVEN user has authentication WHEN issue authentication THEN issues new authentication overriding existing one And returns new authentication")
        void UserHasAuthentication_IssueAuthentication_IssuesNewAuthenticationOverridingExistingOneAndReturnsNewAuthentication() {
            // Given
            var fakeUser = new User(fakeUserId, fakeUsername, fakePassword, Role.USER);
            var issuedAT = new AccessToken(null, fakeUserId, jwtFaker.fakeJwt(JwtType.ACCESS_TOKEN), Instant.now(), Instant.now());
            var issuedRT = new RefreshToken(null, fakeUserId, jwtFaker.fakeJwt(JwtType.REFRESH_TOKEN), Instant.now(), Instant.now());
            var existingAT = new AccessToken(UUID.randomUUID(), fakeUserId, jwtFaker.fakeJwt(JwtType.ACCESS_TOKEN), Instant.now(), Instant.now());
            var existingRT = new RefreshToken(UUID.randomUUID(), fakeUserId, jwtFaker.fakeJwt(JwtType.REFRESH_TOKEN), Instant.now(), Instant.now());
            var savedAT = new AccessToken(existingAT.id(), fakeUserId, issuedAT.jwt(), issuedRT.createdAt(), issuedRT.expiresAt());
            var savedRT = new RefreshToken(existingRT.id(), fakeUserId, issuedRT.jwt(), issuedRT.createdAt(), issuedRT.expiresAt());

            given(userRepositoryMock.findByUsername(anyString())).willReturn(Optional.of(fakeUser));
            given(jwtProviderMock.generateAccessToken(any(Authentication.class))).willReturn(issuedAT);
            given(jwtProviderMock.generateRefreshToken(any(Authentication.class))).willReturn(issuedRT);
            given(accessTokenRepositoryMock.findByUserId(any(UUID.class))).willReturn(Optional.of(existingAT));
            given(refreshTokenRepositoryMock.findByUserId(any(UUID.class))).willReturn(Optional.of(existingRT));
            given(accessTokenRepositoryMock.save(any(AccessToken.class))).willReturn(savedAT);
            given(refreshTokenRepositoryMock.save(any(RefreshToken.class))).willReturn(savedRT);

            // When
            var authentication = new UsernamePasswordAuthenticationToken(fakeUsername, fakePassword);

            var actualAuthentication = jwtIssuer.issueAuthentication(authentication);

            // Then
            assertNotNull(actualAuthentication);
            assertEquals(existingAT.id(), actualAuthentication.accessToken()
                                                              .id());
            assertEquals(existingRT.id(), actualAuthentication.refreshToken()
                                                              .id());
            assertNotEquals(existingAT.jwt(), actualAuthentication.accessToken()
                                                                  .jwt());
            assertNotEquals(existingRT.jwt(), actualAuthentication.refreshToken()
                                                                  .jwt());

            then(userRepositoryMock).should(times(1))
                                    .findByUsername(anyString());
            then(jwtProviderMock).should(times(1))
                                 .generateAccessToken(any(Authentication.class));
            then(jwtProviderMock).should(times(1))
                                 .generateRefreshToken(any(Authentication.class));
            then(accessTokenRepositoryMock).should(times(1))
                                           .findByUserId(any(UUID.class));
            then(refreshTokenRepositoryMock).should(times(1))
                                            .findByUserId(any(UUID.class));
            then(accessTokenRepositoryMock).should(times(1))
                                           .save(any(AccessToken.class));
            then(refreshTokenRepositoryMock).should(times(1))
                                            .save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("GIVEN user has not authentication WHEN issue authentication THEN issues new authentication And returns new authentication")
        void UserHasNotAuthentication_IssueAuthentication_IssuesNewAuthenticationAndReturnsNewAuthentication() {
            // Given
            var fakeUser = new User(fakeUserId, fakeUsername, fakePassword, Role.USER);
            var issuedAT = new AccessToken(null, fakeUserId, jwtFaker.fakeJwt(JwtType.ACCESS_TOKEN), Instant.now(), Instant.now());
            var issuedRT = new RefreshToken(null, fakeUserId, jwtFaker.fakeJwt(JwtType.REFRESH_TOKEN), Instant.now(), Instant.now());
            var savedAT = new AccessToken(UUID.randomUUID(), fakeUserId, issuedAT.jwt(), issuedRT.createdAt(), issuedRT.expiresAt());
            var savedRT = new RefreshToken(UUID.randomUUID(), fakeUserId, issuedRT.jwt(), issuedRT.createdAt(), issuedRT.expiresAt());

            given(userRepositoryMock.findByUsername(anyString())).willReturn(Optional.of(fakeUser));
            given(jwtProviderMock.generateAccessToken(any(Authentication.class))).willReturn(issuedAT);
            given(jwtProviderMock.generateRefreshToken(any(Authentication.class))).willReturn(issuedRT);
            given(accessTokenRepositoryMock.findByUserId(any(UUID.class))).willReturn(Optional.empty());
            given(refreshTokenRepositoryMock.findByUserId(any(UUID.class))).willReturn(Optional.empty());
            given(accessTokenRepositoryMock.save(any(AccessToken.class))).willReturn(savedAT);
            given(refreshTokenRepositoryMock.save(any(RefreshToken.class))).willReturn(savedRT);

            // When
            var authentication = new UsernamePasswordAuthenticationToken(fakeUsername, fakePassword);

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

            then(userRepositoryMock).should(times(1))
                                    .findByUsername(anyString());
            then(jwtProviderMock).should(times(1))
                                 .generateAccessToken(any(Authentication.class));
            then(jwtProviderMock).should(times(1))
                                 .generateRefreshToken(any(Authentication.class));
            then(accessTokenRepositoryMock).should(times(1))
                                           .findByUserId(any(UUID.class));
            then(refreshTokenRepositoryMock).should(times(1))
                                            .findByUserId(any(UUID.class));
            then(accessTokenRepositoryMock).should(times(1))
                                           .save(any(AccessToken.class));
            then(refreshTokenRepositoryMock).should(times(1))
                                            .save(any(RefreshToken.class));
        }

    }

}

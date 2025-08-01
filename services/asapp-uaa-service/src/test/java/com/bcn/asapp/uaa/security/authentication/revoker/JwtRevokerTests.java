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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

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
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.bcn.asapp.uaa.security.authentication.JwtIntegrityViolationException;
import com.bcn.asapp.uaa.security.core.AccessToken;
import com.bcn.asapp.uaa.security.core.AccessTokenRepository;
import com.bcn.asapp.uaa.security.core.RefreshToken;
import com.bcn.asapp.uaa.security.core.RefreshTokenRepository;
import com.bcn.asapp.uaa.user.Role;
import com.bcn.asapp.uaa.user.User;
import com.bcn.asapp.uaa.user.UserRepository;

@ExtendWith(SpringExtension.class)
class JwtRevokerTests {

    @Mock
    private UserRepository userRepositoryMock;

    @Mock
    private AccessTokenRepository accessTokenRepositoryMock;

    @Mock
    private RefreshTokenRepository refreshTokenRepositoryMock;

    @InjectMocks
    private JwtRevoker jwtRevoker;

    private UUID fakeUserId;

    private String fakeUsername;

    private String fakePassword;

    @BeforeEach
    void beforeEach() {
        this.fakeUserId = UUID.randomUUID();
        this.fakeUsername = "TEST USERNAME";
        this.fakePassword = "TEST PASSWORD";
    }

    @Nested
    class RevokeAuthenticationByAuthentication {

        @Test
        @DisplayName("GIVEN user not exists WHEN revoke an authentication THEN does not revoke the authentication And throws UsernameNotFoundException")
        void UserNotExists_RevokeAuthentication_DoesNotRevokeAuthenticationAndThrowsUsernameNotFoundException() {
            // Given
            given(userRepositoryMock.findByUsername(anyString())).willReturn(Optional.empty());

            // When
            var authentication = new UsernamePasswordAuthenticationToken(fakeUsername, fakePassword);

            Executable executable = () -> jwtRevoker.revokeAuthentication(authentication);

            // Then
            var exceptionThrown = assertThrows(UsernameNotFoundException.class, executable);
            assertEquals("User not exists by username " + fakeUsername, exceptionThrown.getMessage());

            then(userRepositoryMock).should(times(1))
                                    .findByUsername(anyString());
            then(accessTokenRepositoryMock).should(never())
                                           .save(any(AccessToken.class));
            then(refreshTokenRepositoryMock).should(never())
                                            .save(any(RefreshToken.class));
        }

        @Test
        @DisplayName("GIVEN access token could not be revoked WHEN revoke an authentication THEN do not revoke the tokens And throws JwtIntegrityViolationException")
        void AccessTokenCouldNotBeRevoked_RevokeAuthentication_DoesNotRevokeAuthenticationAndThrowsJwtIntegrityViolationException() {
            // Given
            var fakeUser = new User(fakeUserId, fakeUsername, fakePassword, Role.USER);
            given(userRepositoryMock.findByUsername(anyString())).willReturn(Optional.of(fakeUser));
            doThrow(new DataAccessResourceFailureException("TEST EXCEPTION")).when(accessTokenRepositoryMock)
                                                                             .deleteByUserId(any(UUID.class));

            // When
            var authentication = new UsernamePasswordAuthenticationToken(fakeUsername, fakePassword);

            Executable executable = () -> jwtRevoker.revokeAuthentication(authentication);

            // Then
            var exceptionThrown = assertThrows(JwtIntegrityViolationException.class, executable);
            assertThat(exceptionThrown.getMessage(), startsWith("Authentication could not be revoked due to:"));
        }

        @Test
        @DisplayName("GIVEN refresh token could not be revoked WHEN revoke an authentication THEN do not revoke the tokens And throws JwtIntegrityViolationException")
        void RefreshTokenCouldNotBeRevoked_RevokeAuthentication_DoesNotRevokeAuthenticationAndThrowsJwtIntegrityViolationException() {
            // Given
            var fakeUser = new User(fakeUserId, fakeUsername, fakePassword, Role.USER);
            given(userRepositoryMock.findByUsername(anyString())).willReturn(Optional.of(fakeUser));
            doThrow(new DataAccessResourceFailureException("TEST EXCEPTION")).when(refreshTokenRepositoryMock)
                                                                             .deleteByUserId(any(UUID.class));

            // When
            var authentication = new UsernamePasswordAuthenticationToken(fakeUsername, fakePassword);

            Executable executable = () -> jwtRevoker.revokeAuthentication(authentication);

            // Then
            var exceptionThrown = assertThrows(JwtIntegrityViolationException.class, executable);
            assertThat(exceptionThrown.getMessage(), startsWith("Authentication could not be revoked due to:"));
        }

        @Test
        @DisplayName("GIVEN user has both access and refresh tokens WHEN revoke an authentication THEN revokes both tokens")
        void UserHasBothTokens_RevokeAuthentication_RevokesBothTokens() {
            // Given
            var fakeUser = new User(fakeUserId, fakeUsername, fakePassword, Role.USER);

            given(userRepositoryMock.findByUsername(anyString())).willReturn(Optional.of(fakeUser));
            willDoNothing().given(accessTokenRepositoryMock)
                           .deleteByUserId(any(UUID.class));
            willDoNothing().given(refreshTokenRepositoryMock)
                           .deleteByUserId(any(UUID.class));

            // When
            var authentication = new UsernamePasswordAuthenticationToken(fakeUsername, fakePassword);

            jwtRevoker.revokeAuthentication(authentication);

            // Then
            then(userRepositoryMock).should(times(1))
                                    .findByUsername(anyString());
            then(accessTokenRepositoryMock).should(times(1))
                                           .deleteByUserId(any(UUID.class));
            then(refreshTokenRepositoryMock).should(times(1))
                                            .deleteByUserId(any(UUID.class));
        }

    }

    @Nested
    class RevokeAuthenticationByUser {

        @Test
        @DisplayName("GIVEN access token could not be revoked WHEN revoke an authentication THEN do not revoke the tokens And throws JwtIntegrityViolationException")
        void AccessTokenCouldNotBeRevoked_RevokeAuthentication_DoesNotRevokeAuthenticationAndThrowsJwtIntegrityViolationException() {
            // Given
            var fakeUser = new User(fakeUserId, fakeUsername, fakePassword, Role.USER);
            given(userRepositoryMock.findByUsername(anyString())).willReturn(Optional.of(fakeUser));
            doThrow(new DataAccessResourceFailureException("TEST EXCEPTION")).when(accessTokenRepositoryMock)
                                                                             .deleteByUserId(any(UUID.class));

            // When
            var user = new User(fakeUserId, fakeUsername, fakePassword, Role.USER);

            Executable executable = () -> jwtRevoker.revokeAuthentication(user);

            // Then
            var exceptionThrown = assertThrows(JwtIntegrityViolationException.class, executable);
            assertThat(exceptionThrown.getMessage(), startsWith("Authentication could not be revoked due to:"));
        }

        @Test
        @DisplayName("GIVEN refresh token could not be revoked WHEN revoke an authentication THEN do not revoke the tokens And throws JwtIntegrityViolationException")
        void RefreshTokenCouldNotBeRevoked_RevokeAuthentication_DoesNotRevokeAuthenticationAndThrowsJwtIntegrityViolationException() {
            // Given
            var fakeUser = new User(fakeUserId, fakeUsername, fakePassword, Role.USER);
            given(userRepositoryMock.findByUsername(anyString())).willReturn(Optional.of(fakeUser));
            doThrow(new DataAccessResourceFailureException("TEST EXCEPTION")).when(refreshTokenRepositoryMock)
                                                                             .deleteByUserId(any(UUID.class));

            // When
            var authentication = new UsernamePasswordAuthenticationToken(fakeUsername, fakePassword);

            Executable executable = () -> jwtRevoker.revokeAuthentication(authentication);

            // Then
            var exceptionThrown = assertThrows(JwtIntegrityViolationException.class, executable);
            assertThat(exceptionThrown.getMessage(), startsWith("Authentication could not be revoked due to:"));
        }

        @Test
        @DisplayName("GIVEN user has both access and refresh tokens WHEN revoke an authentication THEN revokes both tokens")
        void UserHasBothTokens_RevokeAuthentication_RevokesBothTokens() {
            // Given
            var fakeUser = new User(fakeUserId, fakeUsername, fakePassword, Role.USER);

            given(userRepositoryMock.findByUsername(anyString())).willReturn(Optional.of(fakeUser));
            willDoNothing().given(accessTokenRepositoryMock)
                           .deleteByUserId(any(UUID.class));
            willDoNothing().given(refreshTokenRepositoryMock)
                           .deleteByUserId(any(UUID.class));

            // When
            var user = new User(fakeUserId, fakeUsername, fakePassword, Role.USER);

            jwtRevoker.revokeAuthentication(user);

            // Then
            then(accessTokenRepositoryMock).should(times(1))
                                           .deleteByUserId(any(UUID.class));
            then(refreshTokenRepositoryMock).should(times(1))
                                            .deleteByUserId(any(UUID.class));
        }

    }

}

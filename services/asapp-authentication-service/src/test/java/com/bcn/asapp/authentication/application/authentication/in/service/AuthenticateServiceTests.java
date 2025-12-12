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

package com.bcn.asapp.authentication.application.authentication.in.service;

import static com.bcn.asapp.authentication.domain.user.Role.USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;

import com.bcn.asapp.authentication.application.authentication.in.command.AuthenticateCommand;
import com.bcn.asapp.authentication.application.authentication.out.CredentialsAuthenticator;
import com.bcn.asapp.authentication.application.authentication.out.JwtAuthenticationRepository;
import com.bcn.asapp.authentication.application.authentication.out.JwtStore;
import com.bcn.asapp.authentication.application.authentication.out.TokenIssuer;
import com.bcn.asapp.authentication.domain.authentication.EncodedToken;
import com.bcn.asapp.authentication.domain.authentication.Expiration;
import com.bcn.asapp.authentication.domain.authentication.Issued;
import com.bcn.asapp.authentication.domain.authentication.Jwt;
import com.bcn.asapp.authentication.domain.authentication.JwtAuthentication;
import com.bcn.asapp.authentication.domain.authentication.JwtAuthenticationId;
import com.bcn.asapp.authentication.domain.authentication.JwtClaims;
import com.bcn.asapp.authentication.domain.authentication.JwtPair;
import com.bcn.asapp.authentication.domain.authentication.JwtType;
import com.bcn.asapp.authentication.domain.authentication.Subject;
import com.bcn.asapp.authentication.domain.authentication.UserAuthentication;
import com.bcn.asapp.authentication.domain.user.RawPassword;
import com.bcn.asapp.authentication.domain.user.Role;
import com.bcn.asapp.authentication.domain.user.UserId;
import com.bcn.asapp.authentication.domain.user.Username;

@ExtendWith(MockitoExtension.class)
class AuthenticateServiceTests {

    @Mock
    private CredentialsAuthenticator credentialsAuthenticator;

    @Mock
    private TokenIssuer tokenIssuer;

    @Mock
    private JwtAuthenticationRepository jwtAuthenticationRepository;

    @Mock
    private JwtStore jwtStore;

    @InjectMocks
    private AuthenticateService authenticateService;

    private final String usernameValue = "user@asapp.com";

    private final String passwordValue = "TEST@09_password?!";

    private final UUID userId = UUID.fromString("61c5064b-1906-4d11-a8ab-5bfd309e2631");

    private final Role role = USER;

    @Nested
    class Authenticate {

        @Test
        void ThrowsBadCredentialsException_CredentialsAuthenticationFails() {
            // Given
            var command = new AuthenticateCommand(usernameValue, passwordValue);
            var username = Username.of(usernameValue);
            var password = RawPassword.of(passwordValue);

            willThrow(new BadCredentialsException("Invalid credentials")).given(credentialsAuthenticator)
                                                                         .authenticate(username, password);

            // When
            var thrown = catchThrowable(() -> authenticateService.authenticate(command));

            // Then
            assertThat(thrown).isInstanceOf(BadCredentialsException.class)
                              .hasMessageContaining("Invalid credentials");

            then(credentialsAuthenticator).should(times(1))
                                          .authenticate(username, password);
            then(tokenIssuer).should(never())
                             .issueAccessToken(any(UserAuthentication.class));
            then(tokenIssuer).should(never())
                             .issueRefreshToken(any(UserAuthentication.class));
            then(jwtAuthenticationRepository).should(never())
                                             .save(any(JwtAuthentication.class));
            then(jwtStore).should(never())
                          .save(any(JwtPair.class));
        }

        @Test
        void ThrowsBadCredentialsException_AccessTokenIssuanceFails() {
            // Given
            var command = new AuthenticateCommand(usernameValue, passwordValue);
            var username = Username.of(usernameValue);
            var password = RawPassword.of(passwordValue);
            var userAuth = UserAuthentication.authenticated(UserId.of(userId), username, role);

            given(credentialsAuthenticator.authenticate(username, password)).willReturn(userAuth);
            willThrow(new RuntimeException("Token issuance failed")).given(tokenIssuer)
                                                                    .issueAccessToken(userAuth);

            // When
            var thrown = catchThrowable(() -> authenticateService.authenticate(command));

            // Then
            assertThat(thrown).isInstanceOf(BadCredentialsException.class)
                              .hasMessageContaining("Authentication could not be granted due to")
                              .hasCauseInstanceOf(RuntimeException.class);

            then(credentialsAuthenticator).should(times(1))
                                          .authenticate(username, password);
            then(tokenIssuer).should(times(1))
                             .issueAccessToken(userAuth);
            then(tokenIssuer).should(never())
                             .issueRefreshToken(any(UserAuthentication.class));
            then(jwtAuthenticationRepository).should(never())
                                             .save(any(JwtAuthentication.class));
            then(jwtStore).should(never())
                          .save(any(JwtPair.class));
        }

        @Test
        void ThrowsBadCredentialsException_RefreshTokenIssuanceFails() {
            // Given
            var command = new AuthenticateCommand(usernameValue, passwordValue);
            var username = Username.of(usernameValue);
            var password = RawPassword.of(passwordValue);
            var userAuth = UserAuthentication.authenticated(UserId.of(userId), username, role);
            var accessToken = createJwt(JwtType.ACCESS_TOKEN);

            given(credentialsAuthenticator.authenticate(username, password)).willReturn(userAuth);
            given(tokenIssuer.issueAccessToken(userAuth)).willReturn(accessToken);
            willThrow(new RuntimeException("Token issuance failed")).given(tokenIssuer)
                                                                    .issueRefreshToken(userAuth);

            // When
            var thrown = catchThrowable(() -> authenticateService.authenticate(command));

            // Then
            assertThat(thrown).isInstanceOf(BadCredentialsException.class)
                              .hasMessageContaining("Authentication could not be granted due to")
                              .hasCauseInstanceOf(RuntimeException.class);

            then(credentialsAuthenticator).should(times(1))
                                          .authenticate(username, password);
            then(tokenIssuer).should(times(1))
                             .issueAccessToken(userAuth);
            then(tokenIssuer).should(times(1))
                             .issueRefreshToken(userAuth);
            then(jwtAuthenticationRepository).should(never())
                                             .save(any(JwtAuthentication.class));
            then(jwtStore).should(never())
                          .save(any(JwtPair.class));
        }

        @Test
        void ThrowsBadCredentialsException_RepositorySaveFails() {
            // Given
            var command = new AuthenticateCommand(usernameValue, passwordValue);
            var username = Username.of(usernameValue);
            var password = RawPassword.of(passwordValue);
            var userAuth = UserAuthentication.authenticated(UserId.of(userId), username, role);
            var accessToken = createJwt(JwtType.ACCESS_TOKEN);
            var refreshToken = createJwt(JwtType.REFRESH_TOKEN);

            given(credentialsAuthenticator.authenticate(username, password)).willReturn(userAuth);
            given(tokenIssuer.issueAccessToken(userAuth)).willReturn(accessToken);
            given(tokenIssuer.issueRefreshToken(userAuth)).willReturn(refreshToken);
            willThrow(new RuntimeException("Repository save failed")).given(jwtAuthenticationRepository)
                                                                     .save(any(JwtAuthentication.class));

            // When
            var thrown = catchThrowable(() -> authenticateService.authenticate(command));

            // Then
            assertThat(thrown).isInstanceOf(BadCredentialsException.class)
                              .hasMessageContaining("Authentication could not be granted due to")
                              .hasCauseInstanceOf(RuntimeException.class);

            then(credentialsAuthenticator).should(times(1))
                                          .authenticate(username, password);
            then(tokenIssuer).should(times(1))
                             .issueAccessToken(userAuth);
            then(tokenIssuer).should(times(1))
                             .issueRefreshToken(userAuth);
            then(jwtAuthenticationRepository).should(times(1))
                                             .save(any(JwtAuthentication.class));
            then(jwtStore).should(never())
                          .save(any(JwtPair.class));
        }

        @Test
        void ThrowsBadCredentialsException_StoreSaveFails() {
            // Given
            var command = new AuthenticateCommand(usernameValue, passwordValue);
            var username = Username.of(usernameValue);
            var password = RawPassword.of(passwordValue);
            var userAuth = UserAuthentication.authenticated(UserId.of(userId), username, role);
            var accessToken = createJwt(JwtType.ACCESS_TOKEN);
            var refreshToken = createJwt(JwtType.REFRESH_TOKEN);
            var savedAuthentication = JwtAuthentication.authenticated(JwtAuthenticationId.of(UUID.randomUUID()), UserId.of(userId), accessToken, refreshToken);
            var jwtPair = savedAuthentication.getJwtPair();

            given(credentialsAuthenticator.authenticate(username, password)).willReturn(userAuth);
            given(tokenIssuer.issueAccessToken(userAuth)).willReturn(accessToken);
            given(tokenIssuer.issueRefreshToken(userAuth)).willReturn(refreshToken);
            given(jwtAuthenticationRepository.save(any(JwtAuthentication.class))).willReturn(savedAuthentication);
            willThrow(new RuntimeException("Redis connection failed")).given(jwtStore)
                                                                      .save(jwtPair);

            // When
            var thrown = catchThrowable(() -> authenticateService.authenticate(command));

            // Then
            assertThat(thrown).isInstanceOf(BadCredentialsException.class)
                              .hasMessageContaining("Authentication could not be granted due to")
                              .hasCauseInstanceOf(RuntimeException.class);

            then(credentialsAuthenticator).should(times(1))
                                          .authenticate(username, password);
            then(tokenIssuer).should(times(1))
                             .issueAccessToken(userAuth);
            then(tokenIssuer).should(times(1))
                             .issueRefreshToken(userAuth);
            then(jwtAuthenticationRepository).should(times(1))
                                             .save(any(JwtAuthentication.class));
            then(jwtStore).should(times(1))
                          .save(jwtPair);
        }

        @Test
        void ReturnsAuthenticatedJwtAuthentication_ValidCredentials() {
            // Given
            var command = new AuthenticateCommand(usernameValue, passwordValue);
            var username = Username.of(usernameValue);
            var password = RawPassword.of(passwordValue);
            var userAuth = UserAuthentication.authenticated(UserId.of(userId), username, role);
            var accessToken = createJwt(JwtType.ACCESS_TOKEN);
            var refreshToken = createJwt(JwtType.REFRESH_TOKEN);
            var savedAuthentication = JwtAuthentication.authenticated(JwtAuthenticationId.of(UUID.randomUUID()), UserId.of(userId), accessToken, refreshToken);
            var jwtPair = savedAuthentication.getJwtPair();

            given(credentialsAuthenticator.authenticate(username, password)).willReturn(userAuth);
            given(tokenIssuer.issueAccessToken(userAuth)).willReturn(accessToken);
            given(tokenIssuer.issueRefreshToken(userAuth)).willReturn(refreshToken);
            given(jwtAuthenticationRepository.save(any(JwtAuthentication.class))).willReturn(savedAuthentication);

            // When
            var result = authenticateService.authenticate(command);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(savedAuthentication.getId());
            assertThat(result.getUserId()).isEqualTo(savedAuthentication.getUserId());
            assertThat(result.getJwtPair()).isEqualTo(savedAuthentication.getJwtPair());

            then(credentialsAuthenticator).should(times(1))
                                          .authenticate(username, password);
            then(tokenIssuer).should(times(1))
                             .issueAccessToken(userAuth);
            then(tokenIssuer).should(times(1))
                             .issueRefreshToken(userAuth);
            then(jwtAuthenticationRepository).should(times(1))
                                             .save(any(JwtAuthentication.class));
            then(jwtStore).should(times(1))
                          .save(jwtPair);
        }

    }

    private Jwt createJwt(JwtType type) {
        var encodedToken = EncodedToken.of("test.token.value");
        var subject = Subject.of(usernameValue);
        var claims = JwtClaims.of("role", role.name(), "token_use", type == JwtType.ACCESS_TOKEN ? "access" : "refresh");
        var issued = Issued.of(Instant.now());
        var expiration = Expiration.of(issued, 300000L);

        return Jwt.of(encodedToken, type, subject, claims, issued, expiration);
    }

}

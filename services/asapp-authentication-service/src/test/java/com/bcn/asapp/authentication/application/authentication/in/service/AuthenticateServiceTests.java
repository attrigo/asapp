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

import com.bcn.asapp.authentication.application.CompensatingTransactionException;
import com.bcn.asapp.authentication.application.authentication.TokenStoreException;
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
        void ThrowsRuntimeException_AuthenticateCredentialsFails() {
            // Given
            var command = new AuthenticateCommand(usernameValue, passwordValue);
            var username = Username.of(usernameValue);
            var password = RawPassword.of(passwordValue);

            willThrow(new RuntimeException("Authentication failed")).given(credentialsAuthenticator)
                                                                    .authenticate(username, password);

            // When
            var thrown = catchThrowable(() -> authenticateService.authenticate(command));

            // Then
            assertThat(thrown).isInstanceOf(RuntimeException.class)
                              .hasMessageContaining("Authentication failed");

            then(credentialsAuthenticator).should(times(1))
                                          .authenticate(username, password);
            then(tokenIssuer).should(never())
                             .issueTokenPair(any(UserAuthentication.class));
            then(jwtAuthenticationRepository).should(never())
                                             .save(any(JwtAuthentication.class));
            then(jwtStore).should(never())
                          .save(any(JwtPair.class));
        }

        @Test
        void ThrowsRuntimeException_GenerateTokenPairFails() {
            // Given
            var command = new AuthenticateCommand(usernameValue, passwordValue);
            var username = Username.of(usernameValue);
            var password = RawPassword.of(passwordValue);
            var userAuth = UserAuthentication.authenticated(UserId.of(userId), username, role);

            given(credentialsAuthenticator.authenticate(username, password)).willReturn(userAuth);
            willThrow(new RuntimeException("Token issuance failed")).given(tokenIssuer)
                                                                    .issueTokenPair(userAuth);

            // When
            var thrown = catchThrowable(() -> authenticateService.authenticate(command));

            // Then
            assertThat(thrown).isInstanceOf(RuntimeException.class)
                              .hasMessageContaining("Token issuance failed");

            then(credentialsAuthenticator).should(times(1))
                                          .authenticate(username, password);
            then(tokenIssuer).should(times(1))
                             .issueTokenPair(userAuth);
            then(jwtAuthenticationRepository).should(never())
                                             .save(any(JwtAuthentication.class));
            then(jwtStore).should(never())
                          .save(any(JwtPair.class));
        }

        @Test
        void ThrowsRuntimeException_PersistAuthenticationFails() {
            // Given
            var command = new AuthenticateCommand(usernameValue, passwordValue);
            var username = Username.of(usernameValue);
            var password = RawPassword.of(passwordValue);
            var userAuth = UserAuthentication.authenticated(UserId.of(userId), username, role);
            var accessToken = createJwt(JwtType.ACCESS_TOKEN);
            var refreshToken = createJwt(JwtType.REFRESH_TOKEN);
            var jwtPair = JwtPair.of(accessToken, refreshToken);

            given(credentialsAuthenticator.authenticate(username, password)).willReturn(userAuth);
            given(tokenIssuer.issueTokenPair(userAuth)).willReturn(jwtPair);
            willThrow(new RuntimeException("Repository save failed")).given(jwtAuthenticationRepository)
                                                                     .save(any(JwtAuthentication.class));

            // When
            var thrown = catchThrowable(() -> authenticateService.authenticate(command));

            // Then
            assertThat(thrown).isInstanceOf(RuntimeException.class)
                              .hasMessageContaining("Repository save failed");

            then(credentialsAuthenticator).should(times(1))
                                          .authenticate(username, password);
            then(tokenIssuer).should(times(1))
                             .issueTokenPair(userAuth);
            then(jwtAuthenticationRepository).should(times(1))
                                             .save(any(JwtAuthentication.class));
            then(jwtStore).should(never())
                          .save(any(JwtPair.class));
        }

        @Test
        void ThrowsTokenStoreException_ActivateTokensFailsAndCompensationSucceeds() {
            // Given
            var command = new AuthenticateCommand(usernameValue, passwordValue);
            var username = Username.of(usernameValue);
            var password = RawPassword.of(passwordValue);
            var userAuth = UserAuthentication.authenticated(UserId.of(userId), username, role);
            var accessToken = createJwt(JwtType.ACCESS_TOKEN);
            var refreshToken = createJwt(JwtType.REFRESH_TOKEN);
            var jwtPair = JwtPair.of(accessToken, refreshToken);
            var savedAuthentication = JwtAuthentication.authenticated(JwtAuthenticationId.of(UUID.randomUUID()), UserId.of(userId), jwtPair);

            given(credentialsAuthenticator.authenticate(username, password)).willReturn(userAuth);
            given(tokenIssuer.issueTokenPair(userAuth)).willReturn(jwtPair);
            given(jwtAuthenticationRepository.save(any(JwtAuthentication.class))).willReturn(savedAuthentication);
            willThrow(new TokenStoreException("Could not store tokens in fast-access store",
                    new RuntimeException("Token store connection failed"))).given(jwtStore)
                                                                           .save(jwtPair);

            // When
            var thrown = catchThrowable(() -> authenticateService.authenticate(command));

            // Then
            assertThat(thrown).isInstanceOf(TokenStoreException.class)
                              .hasMessageContaining("Could not store tokens in fast-access store")
                              .hasCauseInstanceOf(RuntimeException.class);

            then(credentialsAuthenticator).should(times(1))
                                          .authenticate(username, password);
            then(tokenIssuer).should(times(1))
                             .issueTokenPair(userAuth);
            then(jwtAuthenticationRepository).should(times(1))
                                             .save(any(JwtAuthentication.class));
            then(jwtStore).should(times(1))
                          .save(jwtPair);
            then(jwtAuthenticationRepository).should(times(1))
                                             .deleteById(savedAuthentication.getId());
        }

        @Test
        void ThrowsCompensatingTransactionException_ActivateTokensFailsAndCompensationFails() {
            // Given
            var command = new AuthenticateCommand(usernameValue, passwordValue);
            var username = Username.of(usernameValue);
            var password = RawPassword.of(passwordValue);
            var userAuth = UserAuthentication.authenticated(UserId.of(userId), username, role);
            var accessToken = createJwt(JwtType.ACCESS_TOKEN);
            var refreshToken = createJwt(JwtType.REFRESH_TOKEN);
            var jwtPair = JwtPair.of(accessToken, refreshToken);
            var savedAuthentication = JwtAuthentication.authenticated(JwtAuthenticationId.of(UUID.randomUUID()), UserId.of(userId), jwtPair);

            given(credentialsAuthenticator.authenticate(username, password)).willReturn(userAuth);
            given(tokenIssuer.issueTokenPair(userAuth)).willReturn(jwtPair);
            given(jwtAuthenticationRepository.save(any(JwtAuthentication.class))).willReturn(savedAuthentication);
            willThrow(new TokenStoreException("Could not store tokens in fast-access store",
                    new RuntimeException("Token store connection failed"))).given(jwtStore)
                                                                           .save(jwtPair);
            willThrow(new RuntimeException("Compensation failed")).given(jwtAuthenticationRepository)
                                                                  .deleteById(savedAuthentication.getId());

            // When
            var thrown = catchThrowable(() -> authenticateService.authenticate(command));

            // Then
            assertThat(thrown).isInstanceOf(CompensatingTransactionException.class)
                              .hasMessageContaining("Failed to compensate repository persistence after token activation failure")
                              .hasCauseInstanceOf(RuntimeException.class);

            then(credentialsAuthenticator).should(times(1))
                                          .authenticate(username, password);
            then(tokenIssuer).should(times(1))
                             .issueTokenPair(userAuth);
            then(jwtAuthenticationRepository).should(times(1))
                                             .save(any(JwtAuthentication.class));
            then(jwtStore).should(times(1))
                          .save(jwtPair);
            then(jwtAuthenticationRepository).should(times(1))
                                             .deleteById(savedAuthentication.getId());
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
            var jwtPair = JwtPair.of(accessToken, refreshToken);
            var savedAuthentication = JwtAuthentication.authenticated(JwtAuthenticationId.of(UUID.randomUUID()), UserId.of(userId), jwtPair);

            given(credentialsAuthenticator.authenticate(username, password)).willReturn(userAuth);
            given(tokenIssuer.issueTokenPair(userAuth)).willReturn(jwtPair);
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
                             .issueTokenPair(userAuth);
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

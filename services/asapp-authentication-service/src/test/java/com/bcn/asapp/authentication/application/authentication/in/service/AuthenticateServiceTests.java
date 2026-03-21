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

import static com.bcn.asapp.authentication.testutil.fixture.JwtAuthenticationFactory.anAuthenticatedJwtAuthentication;
import static com.bcn.asapp.authentication.testutil.fixture.JwtFactory.aJwtBuilder;
import static com.bcn.asapp.authentication.testutil.fixture.UserAuthenticationFactory.anAuthenticatedUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.willThrow;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bcn.asapp.authentication.application.authentication.TokenStoreException;
import com.bcn.asapp.authentication.application.authentication.in.command.AuthenticateCommand;
import com.bcn.asapp.authentication.application.authentication.out.CredentialsAuthenticator;
import com.bcn.asapp.authentication.application.authentication.out.JwtAuthenticationRepository;
import com.bcn.asapp.authentication.application.authentication.out.JwtStore;
import com.bcn.asapp.authentication.application.authentication.out.TokenIssuer;
import com.bcn.asapp.authentication.domain.authentication.JwtAuthentication;
import com.bcn.asapp.authentication.domain.authentication.JwtAuthenticationId;
import com.bcn.asapp.authentication.domain.authentication.JwtPair;
import com.bcn.asapp.authentication.domain.authentication.UserAuthentication;
import com.bcn.asapp.authentication.domain.user.RawPassword;
import com.bcn.asapp.authentication.domain.user.Username;

/**
 * Tests {@link AuthenticateService} credential validation, token generation, and persistence.
 * <p>
 * Coverage:
 * <li>Credential validation failures propagate without executing downstream steps</li>
 * <li>Token generation failures prevent persistence and activation</li>
 * <li>Persistence failures prevent token activation</li>
 * <li>Token activation failures propagate without executing compensation</li>
 * <li>Successful authentication completes all orchestration steps</li>
 */
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

    @Nested
    class Authenticate {

        @Test
        void ReturnsAuthenticatedJwtAuthentication_ValidCredentials() {
            // Given
            var usernameValue = "user@asapp.com";
            var passwordValue = "TEST@09_password?!";
            var command = new AuthenticateCommand(usernameValue, passwordValue);
            var username = Username.of(usernameValue);
            var password = RawPassword.of(passwordValue);
            var user = anAuthenticatedUser();
            var jwtAuthentication = anAuthenticatedJwtAuthentication();
            var jwtPair = jwtAuthentication.getJwtPair();

            given(credentialsAuthenticator.authenticate(username, password)).willReturn(user);
            given(tokenIssuer.issueTokenPair(user)).willReturn(jwtPair);
            given(jwtAuthenticationRepository.save(any(JwtAuthentication.class))).willReturn(jwtAuthentication);

            // When
            var actual = authenticateService.authenticate(command);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual).as("JWT authentication").isNotNull();
                softly.assertThat(actual.getId()).as("ID").isEqualTo(jwtAuthentication.getId());
                softly.assertThat(actual.getUserId()).as("user ID").isEqualTo(jwtAuthentication.getUserId());
                softly.assertThat(actual.getJwtPair()).as("JWT pair").isEqualTo(jwtPair);
                // @formatter:on
            });

            then(credentialsAuthenticator).should(times(1))
                                          .authenticate(username, password);
            then(tokenIssuer).should(times(1))
                             .issueTokenPair(user);
            then(jwtAuthenticationRepository).should(times(1))
                                             .save(any(JwtAuthentication.class));
            then(jwtStore).should(times(1))
                          .save(jwtPair);
        }

        @Test
        void ThrowsRuntimeException_CredentialAuthenticationFails() {
            // Given
            var usernameValue = "user@asapp.com";
            var passwordValue = "TEST@09_password?!";
            var command = new AuthenticateCommand(usernameValue, passwordValue);
            var username = Username.of(usernameValue);
            var password = RawPassword.of(passwordValue);

            willThrow(new RuntimeException("Authentication failed")).given(credentialsAuthenticator)
                                                                    .authenticate(username, password);

            // When
            var actual = catchThrowable(() -> authenticateService.authenticate(command));

            // Then
            assertThat(actual).isInstanceOf(RuntimeException.class)
                              .hasMessage("Authentication failed");

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
        void ThrowsRuntimeException_TokenPairGenerationFails() {
            // Given
            var usernameValue = "user@asapp.com";
            var passwordValue = "TEST@09_password?!";
            var command = new AuthenticateCommand(usernameValue, passwordValue);
            var username = Username.of(usernameValue);
            var password = RawPassword.of(passwordValue);
            var user = anAuthenticatedUser();

            given(credentialsAuthenticator.authenticate(username, password)).willReturn(user);
            willThrow(new RuntimeException("Token issuance failed")).given(tokenIssuer)
                                                                    .issueTokenPair(user);

            // When
            var actual = catchThrowable(() -> authenticateService.authenticate(command));

            // Then
            assertThat(actual).isInstanceOf(RuntimeException.class)
                              .hasMessage("Token issuance failed");

            then(credentialsAuthenticator).should(times(1))
                                          .authenticate(username, password);
            then(tokenIssuer).should(times(1))
                             .issueTokenPair(user);
            then(jwtAuthenticationRepository).should(never())
                                             .save(any(JwtAuthentication.class));
            then(jwtStore).should(never())
                          .save(any(JwtPair.class));
        }

        @Test
        void ThrowsRuntimeException_AuthenticationPersistenceFails() {
            // Given
            var usernameValue = "user@asapp.com";
            var passwordValue = "TEST@09_password?!";
            var command = new AuthenticateCommand(usernameValue, passwordValue);
            var username = Username.of(usernameValue);
            var password = RawPassword.of(passwordValue);
            var user = anAuthenticatedUser();
            var accessToken = aJwtBuilder().accessToken()
                                           .build();
            var refreshToken = aJwtBuilder().refreshToken()
                                            .build();
            var jwtPair = JwtPair.of(accessToken, refreshToken);

            given(credentialsAuthenticator.authenticate(username, password)).willReturn(user);
            given(tokenIssuer.issueTokenPair(user)).willReturn(jwtPair);
            willThrow(new RuntimeException("Repository save failed")).given(jwtAuthenticationRepository)
                                                                     .save(any(JwtAuthentication.class));

            // When
            var actual = catchThrowable(() -> authenticateService.authenticate(command));

            // Then
            assertThat(actual).isInstanceOf(RuntimeException.class)
                              .hasMessage("Repository save failed");

            then(credentialsAuthenticator).should(times(1))
                                          .authenticate(username, password);
            then(tokenIssuer).should(times(1))
                             .issueTokenPair(user);
            then(jwtAuthenticationRepository).should(times(1))
                                             .save(any(JwtAuthentication.class));
            then(jwtStore).should(never())
                          .save(any(JwtPair.class));
        }

        @Test
        void ThrowsTokenStoreException_TokenActivationFails() {
            // Given
            var usernameValue = "user@asapp.com";
            var passwordValue = "TEST@09_password?!";
            var command = new AuthenticateCommand(usernameValue, passwordValue);
            var username = Username.of(usernameValue);
            var password = RawPassword.of(passwordValue);
            var user = anAuthenticatedUser();
            var jwtAuthentication = anAuthenticatedJwtAuthentication();
            var jwtPair = jwtAuthentication.getJwtPair();
            var tokenStoreException = new TokenStoreException("Could not store tokens in fast-access store",
                    new RuntimeException("Token store connection failed"));

            given(credentialsAuthenticator.authenticate(username, password)).willReturn(user);
            given(tokenIssuer.issueTokenPair(user)).willReturn(jwtPair);
            given(jwtAuthenticationRepository.save(any(JwtAuthentication.class))).willReturn(jwtAuthentication);
            willThrow(tokenStoreException).given(jwtStore)
                                          .save(jwtPair);

            // When
            var actual = catchThrowable(() -> authenticateService.authenticate(command));

            // Then
            assertThat(actual).isInstanceOf(TokenStoreException.class)
                              .hasMessage("Could not store tokens in fast-access store")
                              .hasCauseInstanceOf(RuntimeException.class);

            then(credentialsAuthenticator).should(times(1))
                                          .authenticate(username, password);
            then(tokenIssuer).should(times(1))
                             .issueTokenPair(user);
            then(jwtAuthenticationRepository).should(times(1))
                                             .save(any(JwtAuthentication.class));
            then(jwtStore).should(times(1))
                          .save(jwtPair);
            then(jwtAuthenticationRepository).should(never())
                                             .deleteById(any(JwtAuthenticationId.class));
        }

    }

}

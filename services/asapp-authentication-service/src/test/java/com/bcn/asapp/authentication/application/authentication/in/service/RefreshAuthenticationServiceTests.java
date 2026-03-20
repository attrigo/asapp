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

import static com.bcn.asapp.authentication.testutil.fixture.JwtAuthenticationFactory.aJwtAuthenticationBuilder;
import static com.bcn.asapp.authentication.testutil.fixture.JwtFactory.aJwtBuilder;
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

import com.bcn.asapp.authentication.application.CompensatingTransactionException;
import com.bcn.asapp.authentication.application.authentication.AuthenticationNotFoundException;
import com.bcn.asapp.authentication.application.authentication.TokenStoreException;
import com.bcn.asapp.authentication.application.authentication.UnexpectedJwtTypeException;
import com.bcn.asapp.authentication.application.authentication.out.JwtAuthenticationRepository;
import com.bcn.asapp.authentication.application.authentication.out.JwtStore;
import com.bcn.asapp.authentication.application.authentication.out.TokenIssuer;
import com.bcn.asapp.authentication.application.authentication.out.TokenVerifier;
import com.bcn.asapp.authentication.domain.authentication.EncodedToken;
import com.bcn.asapp.authentication.domain.authentication.JwtAuthentication;
import com.bcn.asapp.authentication.domain.authentication.JwtPair;
import com.bcn.asapp.authentication.domain.authentication.Subject;
import com.bcn.asapp.authentication.domain.user.Role;

/**
 * Tests {@link RefreshAuthenticationService} token rotation, store activation, and compensation on failure.
 * <p>
 * Coverage:
 * <li>Token verification failures propagate without executing refresh operations</li>
 * <li>Type mismatch failures (access token provided) propagate without executing refresh operations</li>
 * <li>Authentication retrieval failures propagate without executing refresh operations</li>
 * <li>Token generation failures propagate without modifying existing authentication</li>
 * <li>Token activation failures trigger compensation to reactivate old token pair</li>
 * <li>Compensation failures wrap the original cause and propagate to the caller</li>
 * <li>Successful refresh verifies token, retrieves authentication, generates new tokens, rotates in store, and persists changes</li>
 */
@ExtendWith(MockitoExtension.class)
class RefreshAuthenticationServiceTests {

    @Mock
    private TokenVerifier tokenVerifier;

    @Mock
    private TokenIssuer tokenIssuer;

    @Mock
    private JwtAuthenticationRepository jwtAuthenticationRepository;

    @Mock
    private JwtStore jwtStore;

    @InjectMocks
    private RefreshAuthenticationService refreshAuthenticationService;

    @Nested
    class RefreshAuthentication {

        @Test
        void ReturnsRefreshedJwtAuthentication_ValidRefreshToken() {
            // Given
            var refreshTokenValue = "refresh.token";
            var encodedRefreshToken = EncodedToken.of(refreshTokenValue);
            var oldJwtAuthentication = aJwtAuthenticationBuilder().withTokenValues("access.token", refreshTokenValue)
                                                                  .build();
            var oldJwtPair = oldJwtAuthentication.getJwtPair();
            var oldRefreshToken = oldJwtPair.refreshToken();
            var oldRefreshTokenSubject = oldRefreshToken.subject();
            var oldRefreshTokenRoleClaim = oldRefreshToken.roleClaim();
            var newAccessToken = aJwtBuilder().accessToken()
                                              .withEncodedToken("new.access.token")
                                              .build();
            var newRefreshToken = aJwtBuilder().refreshToken()
                                               .withEncodedToken("new.refresh.token")
                                               .build();
            var newJwtPair = JwtPair.of(newAccessToken, newRefreshToken);
            given(jwtAuthenticationRepository.findByRefreshToken(encodedRefreshToken)).willReturn(oldJwtAuthentication);
            given(tokenIssuer.issueTokenPair(oldRefreshTokenSubject, oldRefreshTokenRoleClaim)).willReturn(newJwtPair);
            given(jwtAuthenticationRepository.save(oldJwtAuthentication)).willReturn(oldJwtAuthentication);

            // When
            var actual = refreshAuthenticationService.refreshAuthentication(refreshTokenValue);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual).as("JWT authentication").isNotNull();
                softly.assertThat(actual.getId()).as("ID").isEqualTo(oldJwtAuthentication.getId());
                softly.assertThat(actual.getUserId()).as("user ID").isEqualTo(oldJwtAuthentication.getUserId());
                softly.assertThat(actual.getJwtPair().accessToken()).as("access token").isEqualTo(newAccessToken);
                softly.assertThat(actual.getJwtPair().refreshToken()).as("refresh token").isEqualTo(newRefreshToken);
                // @formatter:on
            });

            then(tokenVerifier).should(times(1))
                               .verifyRefreshToken(encodedRefreshToken);
            then(jwtAuthenticationRepository).should(times(1))
                                             .findByRefreshToken(encodedRefreshToken);
            then(tokenIssuer).should(times(1))
                             .issueTokenPair(oldRefreshTokenSubject, oldRefreshTokenRoleClaim);
            then(jwtAuthenticationRepository).should(times(1))
                                             .save(oldJwtAuthentication);
            then(jwtStore).should(times(1))
                          .delete(oldJwtPair);
            then(jwtStore).should(times(1))
                          .save(newJwtPair);
        }

        @Test
        void ThrowsUnexpectedJwtTypeException_TokenNotRefreshType() {
            // Given
            var refreshTokenValue = "access.token";
            var encodedRefreshToken = EncodedToken.of(refreshTokenValue);

            willThrow(new UnexpectedJwtTypeException("Token is not a refresh token")).given(tokenVerifier)
                                                                                     .verifyRefreshToken(encodedRefreshToken);

            // When
            var actual = catchThrowable(() -> refreshAuthenticationService.refreshAuthentication(refreshTokenValue));

            // Then
            assertThat(actual).isInstanceOf(UnexpectedJwtTypeException.class)
                              .hasMessage("Token is not a refresh token");

            then(tokenVerifier).should(times(1))
                               .verifyRefreshToken(encodedRefreshToken);
            then(jwtAuthenticationRepository).should(never())
                                             .findByRefreshToken(any(EncodedToken.class));
        }

        @Test
        void ThrowsAuthenticationNotFoundException_TokenNotFoundInStore() {
            // Given
            var refreshTokenValue = "refresh.token";
            var encodedRefreshToken = EncodedToken.of(refreshTokenValue);

            willThrow(new AuthenticationNotFoundException("Refresh token not found in active sessions")).given(tokenVerifier)
                                                                                                        .verifyRefreshToken(encodedRefreshToken);

            // When
            var actual = catchThrowable(() -> refreshAuthenticationService.refreshAuthentication(refreshTokenValue));

            // Then
            assertThat(actual).isInstanceOf(AuthenticationNotFoundException.class)
                              .hasMessage("Refresh token not found in active sessions");

            then(tokenVerifier).should(times(1))
                               .verifyRefreshToken(encodedRefreshToken);
            then(jwtAuthenticationRepository).should(never())
                                             .findByRefreshToken(any(EncodedToken.class));
        }

        @Test
        void ThrowsAuthenticationNotFoundException_TokenNotFoundInRepository() {
            // Given
            var refreshTokenValue = "refresh.token";
            var encodedRefreshToken = EncodedToken.of(refreshTokenValue);

            given(jwtAuthenticationRepository.findByRefreshToken(encodedRefreshToken)).willThrow(
                    new AuthenticationNotFoundException("Authentication session not found in repository for refresh token"));

            // When
            var actual = catchThrowable(() -> refreshAuthenticationService.refreshAuthentication(refreshTokenValue));

            // Then
            assertThat(actual).isInstanceOf(AuthenticationNotFoundException.class)
                              .hasMessage("Authentication session not found in repository for refresh token");

            then(tokenVerifier).should(times(1))
                               .verifyRefreshToken(encodedRefreshToken);
            then(jwtAuthenticationRepository).should(times(1))
                                             .findByRefreshToken(encodedRefreshToken);
            then(tokenIssuer).should(never())
                             .issueTokenPair(any(Subject.class), any(Role.class));
        }

        @Test
        void ThrowsRuntimeException_TokenPairGenerationFails() {
            // Given
            var refreshTokenValue = "refresh.token";
            var encodedRefreshToken = EncodedToken.of(refreshTokenValue);
            var oldJwtAuthentication = aJwtAuthenticationBuilder().withTokenValues("access.token", refreshTokenValue)
                                                                  .build();
            var oldJwtPair = oldJwtAuthentication.getJwtPair();
            var oldRefreshToken = oldJwtPair.refreshToken();
            var oldRefreshTokenSubject = oldRefreshToken.subject();
            var oldRefreshTokenRoleClaim = oldRefreshToken.roleClaim();

            given(jwtAuthenticationRepository.findByRefreshToken(encodedRefreshToken)).willReturn(oldJwtAuthentication);
            willThrow(new RuntimeException("Token issuance failed")).given(tokenIssuer)
                                                                    .issueTokenPair(oldRefreshTokenSubject, oldRefreshTokenRoleClaim);

            // When
            var actual = catchThrowable(() -> refreshAuthenticationService.refreshAuthentication(refreshTokenValue));

            // Then
            assertThat(actual).isInstanceOf(RuntimeException.class)
                              .hasMessage("Token issuance failed");

            then(tokenVerifier).should(times(1))
                               .verifyRefreshToken(encodedRefreshToken);
            then(jwtAuthenticationRepository).should(times(1))
                                             .findByRefreshToken(encodedRefreshToken);
            then(tokenIssuer).should(times(1))
                             .issueTokenPair(oldRefreshTokenSubject, oldRefreshTokenRoleClaim);
            then(jwtAuthenticationRepository).should(never())
                                             .save(any(JwtAuthentication.class));
            then(jwtStore).should(never())
                          .delete(any(JwtPair.class));
            then(jwtStore).should(never())
                          .save(any(JwtPair.class));
        }

        @Test
        void ThrowsRuntimeException_AuthenticationPersistenceFails() {
            // Given
            var refreshTokenValue = "refresh.token";
            var encodedRefreshToken = EncodedToken.of(refreshTokenValue);
            var oldJwtAuthentication = aJwtAuthenticationBuilder().withTokenValues("access.token", refreshTokenValue)
                                                                  .build();
            var oldJwtPair = oldJwtAuthentication.getJwtPair();
            var oldRefreshToken = oldJwtPair.refreshToken();
            var oldRefreshTokenSubject = oldRefreshToken.subject();
            var oldRefreshTokenRoleClaim = oldRefreshToken.roleClaim();
            var newAccessToken = aJwtBuilder().accessToken()
                                              .withEncodedToken("new.access.token")
                                              .build();
            var newRefreshToken = aJwtBuilder().refreshToken()
                                               .withEncodedToken("new.refresh.token")
                                               .build();
            var newJwtPair = JwtPair.of(newAccessToken, newRefreshToken);

            given(jwtAuthenticationRepository.findByRefreshToken(encodedRefreshToken)).willReturn(oldJwtAuthentication);
            given(tokenIssuer.issueTokenPair(oldRefreshTokenSubject, oldRefreshTokenRoleClaim)).willReturn(newJwtPair);
            willThrow(new RuntimeException("Repository save failed")).given(jwtAuthenticationRepository)
                                                                     .save(oldJwtAuthentication);

            // When
            var actual = catchThrowable(() -> refreshAuthenticationService.refreshAuthentication(refreshTokenValue));

            // Then
            assertThat(actual).isInstanceOf(RuntimeException.class)
                              .hasMessage("Repository save failed");

            then(tokenVerifier).should(times(1))
                               .verifyRefreshToken(encodedRefreshToken);
            then(jwtAuthenticationRepository).should(times(1))
                                             .findByRefreshToken(encodedRefreshToken);
            then(tokenIssuer).should(times(1))
                             .issueTokenPair(oldRefreshTokenSubject, oldRefreshTokenRoleClaim);
            then(jwtAuthenticationRepository).should(times(1))
                                             .save(oldJwtAuthentication);
            then(jwtStore).should(never())
                          .delete(any(JwtPair.class));
            then(jwtStore).should(never())
                          .save(any(JwtPair.class));
        }

        @Test
        void ThrowsCompensatingTransactionException_TokenRotationFailsAndCompensationFails() {
            // Given
            var refreshTokenValue = "refresh.token";
            var encodedRefreshToken = EncodedToken.of(refreshTokenValue);
            var oldJwtAuthentication = aJwtAuthenticationBuilder().withTokenValues("access.token", refreshTokenValue)
                                                                  .build();
            var oldJwtPair = oldJwtAuthentication.getJwtPair();
            var oldRefreshToken = oldJwtPair.refreshToken();
            var oldRefreshTokenSubject = oldRefreshToken.subject();
            var oldRefreshTokenRoleClaim = oldRefreshToken.roleClaim();
            var newAccessToken = aJwtBuilder().accessToken()
                                              .withEncodedToken("new.access.token")
                                              .build();
            var newRefreshToken = aJwtBuilder().refreshToken()
                                               .withEncodedToken("new.refresh.token")
                                               .build();
            var newJwtPair = JwtPair.of(newAccessToken, newRefreshToken);
            var tokenStoreException = new TokenStoreException("Could not rotate tokens in fast-access store",
                    new RuntimeException("Token store connection failed"));

            given(jwtAuthenticationRepository.findByRefreshToken(encodedRefreshToken)).willReturn(oldJwtAuthentication);
            given(tokenIssuer.issueTokenPair(oldRefreshTokenSubject, oldRefreshTokenRoleClaim)).willReturn(newJwtPair);
            given(jwtAuthenticationRepository.save(oldJwtAuthentication)).willReturn(oldJwtAuthentication);
            willThrow(tokenStoreException).given(jwtStore)
                                          .delete(oldJwtPair);
            willThrow(new RuntimeException("Compensation failed")).given(jwtStore)
                                                                  .save(oldJwtPair);

            // When
            var actual = catchThrowable(() -> refreshAuthenticationService.refreshAuthentication(refreshTokenValue));

            // Then
            assertThat(actual).isInstanceOf(CompensatingTransactionException.class)
                              .hasMessage("Failed to compensate token rotation after token activation failure")
                              .hasCauseInstanceOf(RuntimeException.class);

            then(tokenVerifier).should(times(1))
                               .verifyRefreshToken(encodedRefreshToken);
            then(jwtAuthenticationRepository).should(times(1))
                                             .findByRefreshToken(encodedRefreshToken);
            then(tokenIssuer).should(times(1))
                             .issueTokenPair(oldRefreshTokenSubject, oldRefreshTokenRoleClaim);
            then(jwtAuthenticationRepository).should(times(1))
                                             .save(oldJwtAuthentication);
            then(jwtStore).should(times(1))
                          .delete(oldJwtPair);
            then(jwtStore).should(times(1))
                          .save(oldJwtPair);
        }

        @Test
        void ThrowsTokenStoreException_TokenRotationFailsAndCompensationSucceeds() {
            // Given
            var refreshTokenValue = "refresh.token";
            var encodedRefreshToken = EncodedToken.of(refreshTokenValue);
            var oldJwtAuthentication = aJwtAuthenticationBuilder().withTokenValues("access.token", refreshTokenValue)
                                                                  .build();
            var oldJwtPair = oldJwtAuthentication.getJwtPair();
            var oldRefreshToken = oldJwtPair.refreshToken();
            var oldRefreshTokenSubject = oldRefreshToken.subject();
            var oldRefreshTokenRoleClaim = oldRefreshToken.roleClaim();
            var newAccessToken = aJwtBuilder().accessToken()
                                              .withEncodedToken("new.access.token")
                                              .build();
            var newRefreshToken = aJwtBuilder().refreshToken()
                                               .withEncodedToken("new.refresh.token")
                                               .build();
            var newJwtPair = JwtPair.of(newAccessToken, newRefreshToken);
            var tokenStoreException = new TokenStoreException("Could not rotate tokens in fast-access store",
                    new RuntimeException("Token store connection failed"));

            given(jwtAuthenticationRepository.findByRefreshToken(encodedRefreshToken)).willReturn(oldJwtAuthentication);
            given(tokenIssuer.issueTokenPair(oldRefreshTokenSubject, oldRefreshTokenRoleClaim)).willReturn(newJwtPair);
            given(jwtAuthenticationRepository.save(oldJwtAuthentication)).willReturn(oldJwtAuthentication);
            willThrow(tokenStoreException).given(jwtStore)
                                          .delete(oldJwtPair);

            // When
            var actual = catchThrowable(() -> refreshAuthenticationService.refreshAuthentication(refreshTokenValue));

            // Then
            assertThat(actual).isInstanceOf(TokenStoreException.class)
                              .hasMessage("Could not rotate tokens in fast-access store")
                              .hasCauseInstanceOf(RuntimeException.class);

            then(tokenVerifier).should(times(1))
                               .verifyRefreshToken(encodedRefreshToken);
            then(jwtAuthenticationRepository).should(times(1))
                                             .findByRefreshToken(encodedRefreshToken);
            then(tokenIssuer).should(times(1))
                             .issueTokenPair(oldRefreshTokenSubject, oldRefreshTokenRoleClaim);
            then(jwtAuthenticationRepository).should(times(1))
                                             .save(oldJwtAuthentication);
            then(jwtStore).should(times(1))
                          .delete(oldJwtPair);
            then(jwtStore).should(times(1))
                          .save(oldJwtPair);
        }

        @Test
        void ThrowsCompensatingTransactionException_TokenActivationFailsAndCompensationFails() {
            // Given
            var refreshTokenValue = "refresh.token";
            var encodedRefreshToken = EncodedToken.of(refreshTokenValue);
            var oldJwtAuthentication = aJwtAuthenticationBuilder().withTokenValues("access.token", refreshTokenValue)
                                                                  .build();
            var oldJwtPair = oldJwtAuthentication.getJwtPair();
            var oldRefreshToken = oldJwtPair.refreshToken();
            var oldRefreshTokenSubject = oldRefreshToken.subject();
            var oldRefreshTokenRoleClaim = oldRefreshToken.roleClaim();
            var newAccessToken = aJwtBuilder().accessToken()
                                              .withEncodedToken("new.access.token")
                                              .build();
            var newRefreshToken = aJwtBuilder().refreshToken()
                                               .withEncodedToken("new.refresh.token")
                                               .build();
            var newJwtPair = JwtPair.of(newAccessToken, newRefreshToken);
            var tokenStoreException = new TokenStoreException("Could not rotate tokens in fast-access store",
                    new RuntimeException("Token store connection failed"));

            given(jwtAuthenticationRepository.findByRefreshToken(encodedRefreshToken)).willReturn(oldJwtAuthentication);
            given(tokenIssuer.issueTokenPair(oldRefreshTokenSubject, oldRefreshTokenRoleClaim)).willReturn(newJwtPair);
            given(jwtAuthenticationRepository.save(oldJwtAuthentication)).willReturn(oldJwtAuthentication);
            willThrow(tokenStoreException).given(jwtStore)
                                          .save(newJwtPair);
            willThrow(new RuntimeException("Compensation failed")).given(jwtStore)
                                                                  .save(oldJwtPair);

            // When
            var actual = catchThrowable(() -> refreshAuthenticationService.refreshAuthentication(refreshTokenValue));

            // Then
            assertThat(actual).isInstanceOf(CompensatingTransactionException.class)
                              .hasMessage("Failed to compensate token rotation after token activation failure")
                              .hasCauseInstanceOf(RuntimeException.class);

            then(tokenVerifier).should(times(1))
                               .verifyRefreshToken(encodedRefreshToken);
            then(jwtAuthenticationRepository).should(times(1))
                                             .findByRefreshToken(encodedRefreshToken);
            then(tokenIssuer).should(times(1))
                             .issueTokenPair(oldRefreshTokenSubject, oldRefreshTokenRoleClaim);
            then(jwtAuthenticationRepository).should(times(1))
                                             .save(oldJwtAuthentication);
            then(jwtStore).should(times(1))
                          .delete(oldJwtPair);
            then(jwtStore).should(times(1))
                          .save(newJwtPair);
            then(jwtStore).should(times(1))
                          .save(oldJwtPair);
        }

        @Test
        void ThrowsTokenStoreException_TokenActivationFailsAndCompensationSucceeds() {
            // Given
            var refreshTokenValue = "refresh.token";
            var encodedRefreshToken = EncodedToken.of(refreshTokenValue);
            var oldJwtAuthentication = aJwtAuthenticationBuilder().withTokenValues("access.token", refreshTokenValue)
                                                                  .build();
            var oldJwtPair = oldJwtAuthentication.getJwtPair();
            var oldRefreshToken = oldJwtPair.refreshToken();
            var oldRefreshTokenSubject = oldRefreshToken.subject();
            var oldRefreshTokenRoleClaim = oldRefreshToken.roleClaim();
            var newAccessToken = aJwtBuilder().accessToken()
                                              .withEncodedToken("new.access.token")
                                              .build();
            var newRefreshToken = aJwtBuilder().refreshToken()
                                               .withEncodedToken("new.refresh.token")
                                               .build();
            var newJwtPair = JwtPair.of(newAccessToken, newRefreshToken);
            var tokenStoreException = new TokenStoreException("Could not rotate tokens in fast-access store",
                    new RuntimeException("Token store connection failed"));

            given(jwtAuthenticationRepository.findByRefreshToken(encodedRefreshToken)).willReturn(oldJwtAuthentication);
            given(tokenIssuer.issueTokenPair(oldRefreshTokenSubject, oldRefreshTokenRoleClaim)).willReturn(newJwtPair);
            given(jwtAuthenticationRepository.save(oldJwtAuthentication)).willReturn(oldJwtAuthentication);
            willThrow(tokenStoreException).given(jwtStore)
                                          .save(newJwtPair);

            // When
            var actual = catchThrowable(() -> refreshAuthenticationService.refreshAuthentication(refreshTokenValue));

            // Then
            assertThat(actual).isInstanceOf(TokenStoreException.class)
                              .hasMessage("Could not rotate tokens in fast-access store")
                              .hasCauseInstanceOf(RuntimeException.class);

            then(tokenVerifier).should(times(1))
                               .verifyRefreshToken(encodedRefreshToken);
            then(jwtAuthenticationRepository).should(times(1))
                                             .findByRefreshToken(encodedRefreshToken);
            then(tokenIssuer).should(times(1))
                             .issueTokenPair(oldRefreshTokenSubject, oldRefreshTokenRoleClaim);
            then(jwtAuthenticationRepository).should(times(1))
                                             .save(oldJwtAuthentication);
            then(jwtStore).should(times(1))
                          .delete(oldJwtPair);
            then(jwtStore).should(times(1))
                          .save(newJwtPair);
            then(jwtStore).should(times(1))
                          .save(oldJwtPair);
        }

    }

}

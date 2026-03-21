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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowable;
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

import com.bcn.asapp.authentication.application.authentication.AuthenticationNotFoundException;
import com.bcn.asapp.authentication.application.authentication.AuthenticationPersistenceException;
import com.bcn.asapp.authentication.application.authentication.TokenStoreException;
import com.bcn.asapp.authentication.application.authentication.UnexpectedJwtTypeException;
import com.bcn.asapp.authentication.application.authentication.out.JwtAuthenticationRepository;
import com.bcn.asapp.authentication.application.authentication.out.JwtStore;
import com.bcn.asapp.authentication.application.authentication.out.TokenVerifier;
import com.bcn.asapp.authentication.domain.authentication.EncodedToken;
import com.bcn.asapp.authentication.domain.authentication.JwtPair;

/**
 * Tests {@link RevokeAuthenticationService} token revocation and store deactivation.
 * <p>
 * Coverage:
 * <li>Token verification failures propagate without executing revocation operations</li>
 * <li>Type mismatch failures (refresh token provided) propagate without executing revocation operations</li>
 * <li>Authentication retrieval failures propagate without executing revocation operations</li>
 * <li>DB deletion failures propagate without executing token deactivation</li>
 * <li>Token deactivation failures propagate after DB deletion has completed</li>
 * <li>Successful revocation verifies token, retrieves authentication, deletes from repository, and deactivates in store</li>
 */
@ExtendWith(MockitoExtension.class)
class RevokeAuthenticationServiceTests {

    @Mock
    private TokenVerifier tokenVerifier;

    @Mock
    private JwtAuthenticationRepository jwtAuthenticationRepository;

    @Mock
    private JwtStore jwtStore;

    @InjectMocks
    private RevokeAuthenticationService revokeAuthenticationService;

    @Nested
    class RevokeAuthentication {

        @Test
        void RevokesAuthentication_ValidAccessToken() {
            // Given
            var accessTokenValue = "access.token";
            var encodedAccessToken = EncodedToken.of(accessTokenValue);
            var jwtAuthentication = aJwtAuthenticationBuilder().withTokenValues(accessTokenValue, "refresh.token")
                                                               .build();
            var jwtPair = jwtAuthentication.getJwtPair();

            given(jwtAuthenticationRepository.findByAccessToken(encodedAccessToken)).willReturn(jwtAuthentication);

            // When & Then
            assertThatCode(() -> revokeAuthenticationService.revokeAuthentication(accessTokenValue)).doesNotThrowAnyException();

            then(tokenVerifier).should(times(1))
                               .verifyAccessToken(encodedAccessToken);
            then(jwtAuthenticationRepository).should(times(1))
                                             .findByAccessToken(encodedAccessToken);
            then(jwtAuthenticationRepository).should(times(1))
                                             .deleteById(jwtAuthentication.getId());
            then(jwtStore).should(times(1))
                          .delete(jwtPair);
        }

        @Test
        void ThrowsUnexpectedJwtTypeException_TokenNotAccessType() {
            // Given
            var accessTokenValue = "access.token";
            var encodedAccessToken = EncodedToken.of(accessTokenValue);

            willThrow(new UnexpectedJwtTypeException("Token is not an access token")).given(tokenVerifier)
                                                                                     .verifyAccessToken(encodedAccessToken);

            // When
            var actual = catchThrowable(() -> revokeAuthenticationService.revokeAuthentication(accessTokenValue));

            // Then
            assertThat(actual).isInstanceOf(UnexpectedJwtTypeException.class)
                              .hasMessage("Token is not an access token");

            then(tokenVerifier).should(times(1))
                               .verifyAccessToken(encodedAccessToken);
            then(jwtAuthenticationRepository).should(never())
                                             .findByAccessToken(any(EncodedToken.class));
        }

        @Test
        void ThrowsAuthenticationNotFoundException_TokenNotFoundInStore() {
            // Given
            var accessTokenValue = "access.token";
            var encodedAccessToken = EncodedToken.of(accessTokenValue);

            willThrow(new AuthenticationNotFoundException("Access token not found in active sessions")).given(tokenVerifier)
                                                                                                       .verifyAccessToken(encodedAccessToken);

            // When
            var actual = catchThrowable(() -> revokeAuthenticationService.revokeAuthentication(accessTokenValue));

            // Then
            assertThat(actual).isInstanceOf(AuthenticationNotFoundException.class)
                              .hasMessage("Access token not found in active sessions");

            then(tokenVerifier).should(times(1))
                               .verifyAccessToken(encodedAccessToken);
            then(jwtAuthenticationRepository).should(never())
                                             .findByAccessToken(any(EncodedToken.class));
        }

        @Test
        void ThrowsAuthenticationNotFoundException_TokenNotFoundInRepository() {
            // Given
            var accessTokenValue = "access.token";
            var encodedAccessToken = EncodedToken.of(accessTokenValue);

            given(jwtAuthenticationRepository.findByAccessToken(encodedAccessToken)).willThrow(
                    new AuthenticationNotFoundException("Authentication session not found in repository for access token"));

            // When
            var actual = catchThrowable(() -> revokeAuthenticationService.revokeAuthentication(accessTokenValue));

            // Then
            assertThat(actual).isInstanceOf(AuthenticationNotFoundException.class)
                              .hasMessage("Authentication session not found in repository for access token");

            then(tokenVerifier).should(times(1))
                               .verifyAccessToken(encodedAccessToken);
            then(jwtAuthenticationRepository).should(times(1))
                                             .findByAccessToken(encodedAccessToken);
            then(jwtStore).should(never())
                          .delete(any(JwtPair.class));
        }

        @Test
        void ThrowsAuthenticationPersistenceException_AuthenticationDeletionFails() {
            // Given
            var accessTokenValue = "access.token";
            var encodedAccessToken = EncodedToken.of(accessTokenValue);
            var jwtAuthentication = aJwtAuthenticationBuilder().withTokenValues(accessTokenValue, "refresh.token")
                                                               .build();
            var jwtAuthenticationId = jwtAuthentication.getId();
            var authenticationPersistenceException = new AuthenticationPersistenceException("Could not delete authentication from repository",
                    new RuntimeException("Repository delete failed"));

            given(jwtAuthenticationRepository.findByAccessToken(encodedAccessToken)).willReturn(jwtAuthentication);
            willThrow(authenticationPersistenceException).given(jwtAuthenticationRepository)
                                                         .deleteById(jwtAuthenticationId);

            // When
            var actual = catchThrowable(() -> revokeAuthenticationService.revokeAuthentication(accessTokenValue));

            // Then
            assertThat(actual).isInstanceOf(AuthenticationPersistenceException.class)
                              .hasMessage("Could not delete authentication from repository");

            then(tokenVerifier).should(times(1))
                               .verifyAccessToken(encodedAccessToken);
            then(jwtAuthenticationRepository).should(times(1))
                                             .findByAccessToken(encodedAccessToken);
            then(jwtAuthenticationRepository).should(times(1))
                                             .deleteById(jwtAuthenticationId);
            then(jwtStore).should(never())
                          .delete(any(JwtPair.class));
        }

        @Test
        void ThrowsTokenStoreException_TokenDeactivationFails() {
            // Given
            var accessTokenValue = "access.token";
            var encodedAccessToken = EncodedToken.of(accessTokenValue);
            var jwtAuthentication = aJwtAuthenticationBuilder().withTokenValues(accessTokenValue, "refresh.token")
                                                               .build();
            var jwtPair = jwtAuthentication.getJwtPair();
            var tokenStoreException = new TokenStoreException("Could not delete tokens from fast-access store",
                    new RuntimeException("Token store connection failed"));

            given(jwtAuthenticationRepository.findByAccessToken(encodedAccessToken)).willReturn(jwtAuthentication);
            willThrow(tokenStoreException).given(jwtStore)
                                          .delete(jwtPair);

            // When
            var actual = catchThrowable(() -> revokeAuthenticationService.revokeAuthentication(accessTokenValue));

            // Then
            assertThat(actual).isInstanceOf(TokenStoreException.class)
                              .hasMessage("Could not delete tokens from fast-access store")
                              .hasCauseInstanceOf(RuntimeException.class);

            then(tokenVerifier).should(times(1))
                               .verifyAccessToken(encodedAccessToken);
            then(jwtAuthenticationRepository).should(times(1))
                                             .findByAccessToken(encodedAccessToken);
            then(jwtAuthenticationRepository).should(times(1))
                                             .deleteById(jwtAuthentication.getId());
            then(jwtStore).should(times(1))
                          .delete(jwtPair);
            then(jwtStore).should(never())
                          .save(any(JwtPair.class));
        }

    }

}

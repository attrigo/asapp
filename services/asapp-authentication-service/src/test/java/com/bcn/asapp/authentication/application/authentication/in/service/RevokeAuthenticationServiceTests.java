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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bcn.asapp.authentication.application.CompensatingTransactionException;
import com.bcn.asapp.authentication.application.authentication.AuthenticationNotFoundException;
import com.bcn.asapp.authentication.application.authentication.AuthenticationPersistenceException;
import com.bcn.asapp.authentication.application.authentication.TokenStoreException;
import com.bcn.asapp.authentication.application.authentication.UnexpectedJwtTypeException;
import com.bcn.asapp.authentication.application.authentication.out.JwtAuthenticationRepository;
import com.bcn.asapp.authentication.application.authentication.out.JwtStore;
import com.bcn.asapp.authentication.application.authentication.out.TokenVerifier;
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
import com.bcn.asapp.authentication.domain.user.Role;
import com.bcn.asapp.authentication.domain.user.UserId;

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

    private final String accessTokenValue = "test.access.token";

    private final String usernameValue = "user@asapp.com";

    private final UUID userId = UUID.fromString("61c5064b-1906-4d11-a8ab-5bfd309e2631");

    private final Role role = USER;

    @Nested
    class RevokeAuthentication {

        @Test
        void ThrowsUnexpectedJwtTypeException_TokenIsNotAccessType() {
            // Given
            var encodedAccessToken = EncodedToken.of(accessTokenValue);
            willThrow(new UnexpectedJwtTypeException("Token is not an access token")).given(tokenVerifier)
                                                                                     .verifyAccessToken(encodedAccessToken);

            // When
            var thrown = catchThrowable(() -> revokeAuthenticationService.revokeAuthentication(accessTokenValue));

            // Then
            assertThat(thrown).isInstanceOf(UnexpectedJwtTypeException.class)
                              .hasMessageContaining("is not an access token");

            then(tokenVerifier).should(times(1))
                               .verifyAccessToken(encodedAccessToken);
            then(jwtAuthenticationRepository).should(never())
                                             .findByAccessToken(any(EncodedToken.class));
        }

        @Test
        void ThrowsAuthenticationNotFoundException_TokenNotFoundInStore() {
            // Given
            var encodedAccessToken = EncodedToken.of(accessTokenValue);
            willThrow(new AuthenticationNotFoundException("Access token not found in active sessions")).given(tokenVerifier)
                                                                                                       .verifyAccessToken(encodedAccessToken);

            // When
            var thrown = catchThrowable(() -> revokeAuthenticationService.revokeAuthentication(accessTokenValue));

            // Then
            assertThat(thrown).isInstanceOf(AuthenticationNotFoundException.class)
                              .hasMessageContaining("Access token not found in active sessions");

            then(tokenVerifier).should(times(1))
                               .verifyAccessToken(encodedAccessToken);
            then(jwtAuthenticationRepository).should(never())
                                             .findByAccessToken(any(EncodedToken.class));
        }

        @Test
        void ThrowsAuthenticationNotFoundException_TokenNotFoundInDatabase() {
            // Given
            var encodedAccessToken = EncodedToken.of(accessTokenValue);
            given(jwtAuthenticationRepository.findByAccessToken(encodedAccessToken)).willReturn(Optional.empty());

            // When
            var thrown = catchThrowable(() -> revokeAuthenticationService.revokeAuthentication(accessTokenValue));

            // Then
            assertThat(thrown).isInstanceOf(AuthenticationNotFoundException.class)
                              .hasMessageContaining("Authentication not found by access token");

            then(tokenVerifier).should(times(1))
                               .verifyAccessToken(encodedAccessToken);
            then(jwtAuthenticationRepository).should(times(1))
                                             .findByAccessToken(encodedAccessToken);
            then(jwtStore).should(never())
                          .delete(any(JwtPair.class));
        }

        @Test
        void ThrowsTokenStoreException_DeactivateTokensFails() {
            // Given
            var encodedAccessToken = EncodedToken.of(accessTokenValue);
            var accessToken = createJwt(JwtType.ACCESS_TOKEN, accessTokenValue);
            var refreshToken = createJwt(JwtType.REFRESH_TOKEN, "test.refresh.token");
            var authenticationId = JwtAuthenticationId.of(UUID.randomUUID());
            var jwtPair = JwtPair.of(accessToken, refreshToken);
            var authentication = JwtAuthentication.authenticated(authenticationId, UserId.of(userId), jwtPair);

            given(jwtAuthenticationRepository.findByAccessToken(encodedAccessToken)).willReturn(Optional.of(authentication));
            willThrow(new TokenStoreException("Could not delete tokens from fast-access store",
                    new RuntimeException("Token store connection failed"))).given(jwtStore)
                                                                           .delete(jwtPair);

            // When
            var thrown = catchThrowable(() -> revokeAuthenticationService.revokeAuthentication(accessTokenValue));

            // Then
            assertThat(thrown).isInstanceOf(TokenStoreException.class)
                              .hasMessageContaining("Could not delete tokens from fast-access store")
                              .hasCauseInstanceOf(RuntimeException.class);

            then(tokenVerifier).should(times(1))
                               .verifyAccessToken(encodedAccessToken);
            then(jwtAuthenticationRepository).should(times(1))
                                             .findByAccessToken(encodedAccessToken);
            then(jwtStore).should(times(1))
                          .delete(jwtPair);
            then(jwtAuthenticationRepository).should(never())
                                             .deleteById(any(JwtAuthenticationId.class));
        }

        @Test
        void ThrowsAuthenticationPersistenceException_DeleteAuthenticationFailsAndCompensationSucceeds() {
            // Given
            var encodedAccessToken = EncodedToken.of(accessTokenValue);
            var accessToken = createJwt(JwtType.ACCESS_TOKEN, accessTokenValue);
            var refreshToken = createJwt(JwtType.REFRESH_TOKEN, "test.refresh.token");
            var authenticationId = JwtAuthenticationId.of(UUID.randomUUID());
            var jwtPair = JwtPair.of(accessToken, refreshToken);
            var authentication = JwtAuthentication.authenticated(authenticationId, UserId.of(userId), jwtPair);

            given(jwtAuthenticationRepository.findByAccessToken(encodedAccessToken)).willReturn(Optional.of(authentication));
            willThrow(new AuthenticationPersistenceException("Could not delete authentication from repository",
                    new RuntimeException("Repository delete failed"))).given(jwtAuthenticationRepository)
                                                                      .deleteById(authenticationId);

            // When
            var thrown = catchThrowable(() -> revokeAuthenticationService.revokeAuthentication(accessTokenValue));

            // Then
            assertThat(thrown).isInstanceOf(AuthenticationPersistenceException.class)
                              .hasMessageContaining("Could not delete authentication from repository");

            then(tokenVerifier).should(times(1))
                               .verifyAccessToken(encodedAccessToken);
            then(jwtAuthenticationRepository).should(times(1))
                                             .findByAccessToken(encodedAccessToken);
            then(jwtStore).should(times(1))
                          .delete(jwtPair);
            then(jwtAuthenticationRepository).should(times(1))
                                             .deleteById(authenticationId);
            then(jwtStore).should(times(1))
                          .save(jwtPair);
        }

        @Test
        void ThrowsCompensatingTransactionException_DeleteAuthenticationFailsAndCompensationFails() {
            // Given
            var encodedAccessToken = EncodedToken.of(accessTokenValue);
            var accessToken = createJwt(JwtType.ACCESS_TOKEN, accessTokenValue);
            var refreshToken = createJwt(JwtType.REFRESH_TOKEN, "test.refresh.token");
            var authenticationId = JwtAuthenticationId.of(UUID.randomUUID());
            var jwtPair = JwtPair.of(accessToken, refreshToken);
            var authentication = JwtAuthentication.authenticated(authenticationId, UserId.of(userId), jwtPair);

            given(jwtAuthenticationRepository.findByAccessToken(encodedAccessToken)).willReturn(Optional.of(authentication));
            willThrow(new AuthenticationPersistenceException("Could not delete authentication from repository",
                    new RuntimeException("Repository delete failed"))).given(jwtAuthenticationRepository)
                                                                      .deleteById(authenticationId);
            willThrow(new RuntimeException("Compensation failed")).given(jwtStore)
                                                                  .save(jwtPair);

            // When
            var thrown = catchThrowable(() -> revokeAuthenticationService.revokeAuthentication(accessTokenValue));

            // Then
            assertThat(thrown).isInstanceOf(CompensatingTransactionException.class)
                              .hasMessageContaining("Failed to compensate token deactivation after repository deletion failure")
                              .hasCauseInstanceOf(RuntimeException.class);

            then(tokenVerifier).should(times(1))
                               .verifyAccessToken(encodedAccessToken);
            then(jwtAuthenticationRepository).should(times(1))
                                             .findByAccessToken(encodedAccessToken);
            then(jwtStore).should(times(1))
                          .delete(jwtPair);
            then(jwtAuthenticationRepository).should(times(1))
                                             .deleteById(authenticationId);
            then(jwtStore).should(times(1))
                          .save(jwtPair);
        }

        @Test
        void RevokesAuthentication_ValidAccessToken() {
            // Given
            var encodedAccessToken = EncodedToken.of(accessTokenValue);
            var accessToken = createJwt(JwtType.ACCESS_TOKEN, accessTokenValue);
            var refreshToken = createJwt(JwtType.REFRESH_TOKEN, "test.refresh.token");
            var authenticationId = JwtAuthenticationId.of(UUID.randomUUID());
            var jwtPair = JwtPair.of(accessToken, refreshToken);
            var authentication = JwtAuthentication.authenticated(authenticationId, UserId.of(userId), jwtPair);

            given(jwtAuthenticationRepository.findByAccessToken(encodedAccessToken)).willReturn(Optional.of(authentication));

            // When
            assertThatCode(() -> revokeAuthenticationService.revokeAuthentication(accessTokenValue)).doesNotThrowAnyException();

            // Then
            then(tokenVerifier).should(times(1))
                               .verifyAccessToken(encodedAccessToken);
            then(jwtAuthenticationRepository).should(times(1))
                                             .findByAccessToken(encodedAccessToken);
            then(jwtStore).should(times(1))
                          .delete(jwtPair);
            then(jwtAuthenticationRepository).should(times(1))
                                             .deleteById(authenticationId);
        }

    }

    private Jwt createJwt(JwtType type, String tokenValue) {
        var encodedToken = EncodedToken.of(tokenValue);
        var subject = Subject.of(usernameValue);
        var claims = JwtClaims.of("role", role.name(), "token_use", type == JwtType.ACCESS_TOKEN ? "access" : "refresh");
        var issued = Issued.of(Instant.now());
        var expiration = Expiration.of(issued, 300000L);

        return Jwt.of(encodedToken, type, subject, claims, issued, expiration);
    }

}

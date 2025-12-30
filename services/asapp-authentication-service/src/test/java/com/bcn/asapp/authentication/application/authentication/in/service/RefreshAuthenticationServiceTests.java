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
import com.bcn.asapp.authentication.application.authentication.AuthenticationNotFoundException;
import com.bcn.asapp.authentication.application.authentication.TokenStoreException;
import com.bcn.asapp.authentication.application.authentication.UnexpectedJwtTypeException;
import com.bcn.asapp.authentication.application.authentication.out.JwtAuthenticationRepository;
import com.bcn.asapp.authentication.application.authentication.out.JwtStore;
import com.bcn.asapp.authentication.application.authentication.out.TokenIssuer;
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

    private final String refreshTokenValue = "test.refresh.token";

    private final String usernameValue = "user@asapp.com";

    private final UUID userId = UUID.fromString("61c5064b-1906-4d11-a8ab-5bfd309e2631");

    private final Role role = USER;

    @Nested
    class RefreshAuthentication {

        @Test
        void ThrowsUnexpectedJwtTypeException_TokenIsNotRefreshType() {
            // Given
            var encodedRefreshToken = EncodedToken.of(refreshTokenValue);
            willThrow(new UnexpectedJwtTypeException("Token is not a refresh token")).given(tokenVerifier)
                                                                                     .verifyRefreshToken(encodedRefreshToken);

            // When
            var thrown = catchThrowable(() -> refreshAuthenticationService.refreshAuthentication(refreshTokenValue));

            // Then
            assertThat(thrown).isInstanceOf(UnexpectedJwtTypeException.class)
                              .hasMessageContaining("is not a refresh token");

            then(tokenVerifier).should(times(1))
                               .verifyRefreshToken(encodedRefreshToken);
            then(jwtAuthenticationRepository).should(never())
                                             .findByRefreshToken(any(EncodedToken.class));
        }

        @Test
        void ThrowsAuthenticationNotFoundException_TokenNotFoundInStore() {
            // Given
            var encodedRefreshToken = EncodedToken.of(refreshTokenValue);
            willThrow(new AuthenticationNotFoundException("Refresh token not found in active sessions")).given(tokenVerifier)
                                                                                                        .verifyRefreshToken(encodedRefreshToken);

            // When
            var thrown = catchThrowable(() -> refreshAuthenticationService.refreshAuthentication(refreshTokenValue));

            // Then
            assertThat(thrown).isInstanceOf(AuthenticationNotFoundException.class)
                              .hasMessageContaining("Refresh token not found in active sessions");

            then(tokenVerifier).should(times(1))
                               .verifyRefreshToken(encodedRefreshToken);
            then(jwtAuthenticationRepository).should(never())
                                             .findByRefreshToken(any(EncodedToken.class));
        }

        @Test
        void ThrowsAuthenticationNotFoundException_TokenNotFoundInDatabase() {
            // Given
            var encodedRefreshToken = EncodedToken.of(refreshTokenValue);

            given(jwtAuthenticationRepository.findByRefreshToken(encodedRefreshToken)).willThrow(
                    new AuthenticationNotFoundException("Authentication session not found in repository for refresh token"));

            // When
            var thrown = catchThrowable(() -> refreshAuthenticationService.refreshAuthentication(refreshTokenValue));

            // Then
            assertThat(thrown).isInstanceOf(AuthenticationNotFoundException.class)
                              .hasMessageContaining("Authentication session not found in repository for refresh token");

            then(tokenVerifier).should(times(1))
                               .verifyRefreshToken(encodedRefreshToken);
            then(jwtAuthenticationRepository).should(times(1))
                                             .findByRefreshToken(encodedRefreshToken);
            then(tokenIssuer).should(never())
                             .issueTokenPair(any(Subject.class), any(Role.class));
        }

        @Test
        void ThrowsRuntimeException_GenerateTokenPairFails() {
            // Given
            var encodedRefreshToken = EncodedToken.of(refreshTokenValue);
            var oldAccessToken = createJwt(JwtType.ACCESS_TOKEN, "old.access.token");
            var oldRefreshToken = createJwt(JwtType.REFRESH_TOKEN, refreshTokenValue);
            var oldJwtPair = JwtPair.of(oldAccessToken, oldRefreshToken);
            var authentication = JwtAuthentication.authenticated(JwtAuthenticationId.of(UUID.randomUUID()), UserId.of(userId), oldJwtPair);

            given(jwtAuthenticationRepository.findByRefreshToken(encodedRefreshToken)).willReturn(authentication);
            willThrow(new RuntimeException("Token issuance failed")).given(tokenIssuer)
                                                                    .issueTokenPair(oldRefreshToken.subject(), oldRefreshToken.roleClaim());

            // When
            var thrown = catchThrowable(() -> refreshAuthenticationService.refreshAuthentication(refreshTokenValue));

            // Then
            assertThat(thrown).isInstanceOf(RuntimeException.class)
                              .hasMessageContaining("Token issuance failed");

            then(tokenVerifier).should(times(1))
                               .verifyRefreshToken(encodedRefreshToken);
            then(jwtAuthenticationRepository).should(times(1))
                                             .findByRefreshToken(encodedRefreshToken);
            then(tokenIssuer).should(times(1))
                             .issueTokenPair(oldRefreshToken.subject(), oldRefreshToken.roleClaim());
            then(jwtAuthenticationRepository).should(never())
                                             .save(any(JwtAuthentication.class));
            then(jwtStore).should(never())
                          .delete(any(JwtPair.class));
            then(jwtStore).should(never())
                          .save(any(JwtPair.class));
        }

        @Test
        void ThrowsRuntimeException_PersistsAuthenticationFails() {
            // Given
            var encodedRefreshToken = EncodedToken.of(refreshTokenValue);
            var oldAccessToken = createJwt(JwtType.ACCESS_TOKEN, "old.access.token");
            var oldRefreshToken = createJwt(JwtType.REFRESH_TOKEN, refreshTokenValue);
            var oldJwtPair = JwtPair.of(oldAccessToken, oldRefreshToken);
            var authentication = JwtAuthentication.authenticated(JwtAuthenticationId.of(UUID.randomUUID()), UserId.of(userId), oldJwtPair);
            var newAccessToken = createJwt(JwtType.ACCESS_TOKEN, "new.access.token");
            var newRefreshToken = createJwt(JwtType.REFRESH_TOKEN, "new.refresh.token");
            var newJwtPair = JwtPair.of(newAccessToken, newRefreshToken);

            given(jwtAuthenticationRepository.findByRefreshToken(encodedRefreshToken)).willReturn(authentication);
            given(tokenIssuer.issueTokenPair(oldRefreshToken.subject(), oldRefreshToken.roleClaim())).willReturn(newJwtPair);
            willThrow(new RuntimeException("Repository save failed")).given(jwtAuthenticationRepository)
                                                                     .save(authentication);

            // When
            var thrown = catchThrowable(() -> refreshAuthenticationService.refreshAuthentication(refreshTokenValue));

            // Then
            assertThat(thrown).isInstanceOf(RuntimeException.class)
                              .hasMessageContaining("Repository save failed");

            then(tokenVerifier).should(times(1))
                               .verifyRefreshToken(encodedRefreshToken);
            then(jwtAuthenticationRepository).should(times(1))
                                             .findByRefreshToken(encodedRefreshToken);
            then(tokenIssuer).should(times(1))
                             .issueTokenPair(oldRefreshToken.subject(), oldRefreshToken.roleClaim());
            then(jwtAuthenticationRepository).should(times(1))
                                             .save(authentication);
            then(jwtStore).should(never())
                          .delete(any(JwtPair.class));
            then(jwtStore).should(never())
                          .save(any(JwtPair.class));
        }

        @Test
        void ThrowsTokenStoreException_TokenRotationFailsAndCompensationSucceeds() {
            // Given
            var encodedRefreshToken = EncodedToken.of(refreshTokenValue);
            var oldAccessToken = createJwt(JwtType.ACCESS_TOKEN, "old.access.token");
            var oldRefreshToken = createJwt(JwtType.REFRESH_TOKEN, refreshTokenValue);
            var oldJwtPair = JwtPair.of(oldAccessToken, oldRefreshToken);
            var authentication = JwtAuthentication.authenticated(JwtAuthenticationId.of(UUID.randomUUID()), UserId.of(userId), oldJwtPair);
            var newAccessToken = createJwt(JwtType.ACCESS_TOKEN, "new.access.token");
            var newRefreshToken = createJwt(JwtType.REFRESH_TOKEN, "new.refresh.token");
            var newJwtPair = JwtPair.of(newAccessToken, newRefreshToken);

            given(jwtAuthenticationRepository.findByRefreshToken(encodedRefreshToken)).willReturn(authentication);
            given(tokenIssuer.issueTokenPair(oldRefreshToken.subject(), oldRefreshToken.roleClaim())).willReturn(newJwtPair);
            given(jwtAuthenticationRepository.save(authentication)).willReturn(authentication);
            willThrow(new TokenStoreException("Could not rotate tokens in fast-access store",
                    new RuntimeException("Token store connection failed"))).given(jwtStore)
                                                                           .delete(oldJwtPair);

            // When
            var thrown = catchThrowable(() -> refreshAuthenticationService.refreshAuthentication(refreshTokenValue));

            // Then
            assertThat(thrown).isInstanceOf(TokenStoreException.class)
                              .hasMessageContaining("Could not rotate tokens in fast-access store")
                              .hasCauseInstanceOf(RuntimeException.class);

            then(tokenVerifier).should(times(1))
                               .verifyRefreshToken(encodedRefreshToken);
            then(jwtAuthenticationRepository).should(times(1))
                                             .findByRefreshToken(encodedRefreshToken);
            then(tokenIssuer).should(times(1))
                             .issueTokenPair(oldRefreshToken.subject(), oldRefreshToken.roleClaim());
            then(jwtAuthenticationRepository).should(times(1))
                                             .save(authentication);
            then(jwtStore).should(times(1))
                          .delete(oldJwtPair);
            then(jwtStore).should(times(1))
                          .save(oldJwtPair);
        }

        @Test
        void ThrowsTokenStoreException_TokenSaveFailsAndCompensationSucceeds() {
            // Given
            var encodedRefreshToken = EncodedToken.of(refreshTokenValue);
            var oldAccessToken = createJwt(JwtType.ACCESS_TOKEN, "old.access.token");
            var oldRefreshToken = createJwt(JwtType.REFRESH_TOKEN, refreshTokenValue);
            var oldJwtPair = JwtPair.of(oldAccessToken, oldRefreshToken);
            var authentication = JwtAuthentication.authenticated(JwtAuthenticationId.of(UUID.randomUUID()), UserId.of(userId), oldJwtPair);
            var newAccessToken = createJwt(JwtType.ACCESS_TOKEN, "new.access.token");
            var newRefreshToken = createJwt(JwtType.REFRESH_TOKEN, "new.refresh.token");
            var newJwtPair = JwtPair.of(newAccessToken, newRefreshToken);

            given(jwtAuthenticationRepository.findByRefreshToken(encodedRefreshToken)).willReturn(authentication);
            given(tokenIssuer.issueTokenPair(oldRefreshToken.subject(), oldRefreshToken.roleClaim())).willReturn(newJwtPair);
            given(jwtAuthenticationRepository.save(authentication)).willReturn(authentication);
            willThrow(new TokenStoreException("Could not rotate tokens in fast-access store",
                    new RuntimeException("Token store connection failed"))).willDoNothing()
                                                                           .given(jwtStore)
                                                                           .save(any(JwtPair.class));

            // When
            var thrown = catchThrowable(() -> refreshAuthenticationService.refreshAuthentication(refreshTokenValue));

            // Then
            assertThat(thrown).isInstanceOf(TokenStoreException.class)
                              .hasMessageContaining("Could not rotate tokens in fast-access store")
                              .hasCauseInstanceOf(RuntimeException.class);

            then(tokenVerifier).should(times(1))
                               .verifyRefreshToken(encodedRefreshToken);
            then(jwtAuthenticationRepository).should(times(1))
                                             .findByRefreshToken(encodedRefreshToken);
            then(tokenIssuer).should(times(1))
                             .issueTokenPair(oldRefreshToken.subject(), oldRefreshToken.roleClaim());
            then(jwtAuthenticationRepository).should(times(1))
                                             .save(authentication);
            then(jwtStore).should(times(1))
                          .delete(oldJwtPair);
            then(jwtStore).should(times(2))
                          .save(any(JwtPair.class));
        }

        @Test
        void ThrowsCompensatingTransactionException_TokenRotationFailsAndCompensationFails() {
            // Given
            var encodedRefreshToken = EncodedToken.of(refreshTokenValue);
            var oldAccessToken = createJwt(JwtType.ACCESS_TOKEN, "old.access.token");
            var oldRefreshToken = createJwt(JwtType.REFRESH_TOKEN, refreshTokenValue);
            var oldJwtPair = JwtPair.of(oldAccessToken, oldRefreshToken);
            var authentication = JwtAuthentication.authenticated(JwtAuthenticationId.of(UUID.randomUUID()), UserId.of(userId), oldJwtPair);
            var newAccessToken = createJwt(JwtType.ACCESS_TOKEN, "new.access.token");
            var newRefreshToken = createJwt(JwtType.REFRESH_TOKEN, "new.refresh.token");
            var newJwtPair = JwtPair.of(newAccessToken, newRefreshToken);

            given(jwtAuthenticationRepository.findByRefreshToken(encodedRefreshToken)).willReturn(authentication);
            given(tokenIssuer.issueTokenPair(oldRefreshToken.subject(), oldRefreshToken.roleClaim())).willReturn(newJwtPair);
            given(jwtAuthenticationRepository.save(authentication)).willReturn(authentication);
            willThrow(new TokenStoreException("Could not rotate tokens in fast-access store",
                    new RuntimeException("Token store connection failed"))).given(jwtStore)
                                                                           .delete(oldJwtPair);
            willThrow(new RuntimeException("Compensation failed")).given(jwtStore)
                                                                  .save(oldJwtPair);

            // When
            var thrown = catchThrowable(() -> refreshAuthenticationService.refreshAuthentication(refreshTokenValue));

            // Then
            assertThat(thrown).isInstanceOf(CompensatingTransactionException.class)
                              .hasMessageContaining("Failed to compensate token rotation after token activation failure")
                              .hasCauseInstanceOf(RuntimeException.class);

            then(tokenVerifier).should(times(1))
                               .verifyRefreshToken(encodedRefreshToken);
            then(jwtAuthenticationRepository).should(times(1))
                                             .findByRefreshToken(encodedRefreshToken);
            then(tokenIssuer).should(times(1))
                             .issueTokenPair(oldRefreshToken.subject(), oldRefreshToken.roleClaim());
            then(jwtAuthenticationRepository).should(times(1))
                                             .save(authentication);
            then(jwtStore).should(times(1))
                          .delete(oldJwtPair);
            then(jwtStore).should(times(1))
                          .save(oldJwtPair);
        }

        @Test
        void ThrowsCompensatingTransactionException_TokenSaveFailsAndCompensationFails() {
            // Given
            var encodedRefreshToken = EncodedToken.of(refreshTokenValue);
            var oldAccessToken = createJwt(JwtType.ACCESS_TOKEN, "old.access.token");
            var oldRefreshToken = createJwt(JwtType.REFRESH_TOKEN, refreshTokenValue);
            var oldJwtPair = JwtPair.of(oldAccessToken, oldRefreshToken);
            var authentication = JwtAuthentication.authenticated(JwtAuthenticationId.of(UUID.randomUUID()), UserId.of(userId), oldJwtPair);
            var newAccessToken = createJwt(JwtType.ACCESS_TOKEN, "new.access.token");
            var newRefreshToken = createJwt(JwtType.REFRESH_TOKEN, "new.refresh.token");
            var newJwtPair = JwtPair.of(newAccessToken, newRefreshToken);

            given(jwtAuthenticationRepository.findByRefreshToken(encodedRefreshToken)).willReturn(authentication);
            given(tokenIssuer.issueTokenPair(oldRefreshToken.subject(), oldRefreshToken.roleClaim())).willReturn(newJwtPair);
            given(jwtAuthenticationRepository.save(authentication)).willReturn(authentication);
            willThrow(new TokenStoreException("Could not rotate tokens in fast-access store",
                    new RuntimeException("Token store connection failed"))).given(jwtStore)
                                                                           .save(any(JwtPair.class));

            // When
            var thrown = catchThrowable(() -> refreshAuthenticationService.refreshAuthentication(refreshTokenValue));

            // Then
            assertThat(thrown).isInstanceOf(CompensatingTransactionException.class)
                              .hasMessageContaining("Failed to compensate token rotation after token activation failure")
                              .hasCauseInstanceOf(RuntimeException.class);

            then(tokenVerifier).should(times(1))
                               .verifyRefreshToken(encodedRefreshToken);
            then(jwtAuthenticationRepository).should(times(1))
                                             .findByRefreshToken(encodedRefreshToken);
            then(tokenIssuer).should(times(1))
                             .issueTokenPair(oldRefreshToken.subject(), oldRefreshToken.roleClaim());
            then(jwtAuthenticationRepository).should(times(1))
                                             .save(authentication);
            then(jwtStore).should(times(1))
                          .delete(oldJwtPair);
            then(jwtStore).should(times(2))
                          .save(any(JwtPair.class));
        }

        @Test
        void ReturnsRefreshedAuthentication_ValidRefreshToken() {
            // Given
            var encodedRefreshToken = EncodedToken.of(refreshTokenValue);
            var oldAccessToken = createJwt(JwtType.ACCESS_TOKEN, "old.access.token");
            var oldRefreshToken = createJwt(JwtType.REFRESH_TOKEN, refreshTokenValue);
            var oldJwtPair = JwtPair.of(oldAccessToken, oldRefreshToken);
            var authentication = JwtAuthentication.authenticated(JwtAuthenticationId.of(UUID.randomUUID()), UserId.of(userId), oldJwtPair);
            var newAccessToken = createJwt(JwtType.ACCESS_TOKEN, "new.access.token");
            var newRefreshToken = createJwt(JwtType.REFRESH_TOKEN, "new.refresh.token");
            var newJwtPair = JwtPair.of(newAccessToken, newRefreshToken);

            given(jwtAuthenticationRepository.findByRefreshToken(encodedRefreshToken)).willReturn(authentication);
            given(tokenIssuer.issueTokenPair(oldRefreshToken.subject(), oldRefreshToken.roleClaim())).willReturn(newJwtPair);
            given(jwtAuthenticationRepository.save(authentication)).willReturn(authentication);

            // When
            var result = refreshAuthenticationService.refreshAuthentication(refreshTokenValue);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(authentication.getId());
            assertThat(result.getUserId()).isEqualTo(authentication.getUserId());
            assertThat(result.getJwtPair()
                             .accessToken()).isEqualTo(newAccessToken);
            assertThat(result.getJwtPair()
                             .refreshToken()).isEqualTo(newRefreshToken);

            then(tokenVerifier).should(times(1))
                               .verifyRefreshToken(encodedRefreshToken);
            then(jwtAuthenticationRepository).should(times(1))
                                             .findByRefreshToken(encodedRefreshToken);
            then(tokenIssuer).should(times(1))
                             .issueTokenPair(oldRefreshToken.subject(), oldRefreshToken.roleClaim());
            then(jwtAuthenticationRepository).should(times(1))
                                             .save(authentication);
            then(jwtStore).should(times(1))
                          .delete(oldJwtPair);
            then(jwtStore).should(times(1))
                          .save(any(JwtPair.class));
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

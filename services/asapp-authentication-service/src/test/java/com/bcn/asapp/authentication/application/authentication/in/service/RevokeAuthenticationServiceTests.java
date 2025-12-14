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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bcn.asapp.authentication.application.authentication.out.JwtAuthenticationRepository;
import com.bcn.asapp.authentication.application.authentication.out.JwtStore;
import com.bcn.asapp.authentication.application.authentication.out.TokenDecoder;
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
import com.bcn.asapp.authentication.infrastructure.security.DecodedJwt;
import com.bcn.asapp.authentication.infrastructure.security.InvalidJwtAuthenticationException;
import com.bcn.asapp.authentication.infrastructure.security.InvalidJwtException;
import com.bcn.asapp.authentication.infrastructure.security.JwtAuthenticationNotFoundException;
import com.bcn.asapp.authentication.infrastructure.security.UnexpectedJwtTypeException;

@ExtendWith(MockitoExtension.class)
class RevokeAuthenticationServiceTests {

    @Mock
    private TokenDecoder tokenDecoder;

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
        void ThrowsInvalidJwtException_DecodeTokenFails() {
            // Given
            var encodedAccessToken = EncodedToken.of(accessTokenValue);
            given(tokenDecoder.decode(encodedAccessToken)).willThrow(new InvalidJwtException("Invalid token"));

            // When
            var thrown = catchThrowable(() -> revokeAuthenticationService.revokeAuthentication(accessTokenValue));

            // Then
            assertThat(thrown).isInstanceOf(InvalidJwtException.class)
                              .hasMessageContaining("Invalid token");

            then(tokenDecoder).should(times(1))
                              .decode(encodedAccessToken);
            then(jwtStore).should(never())
                          .accessTokenExists(any(EncodedToken.class));
        }

        @Test
        void ThrowsUnexpectedJwtTypeException_TokenIsNotAccessType() {
            // Given
            var encodedAccessToken = EncodedToken.of(accessTokenValue);
            var decodedToken = new DecodedJwt(accessTokenValue, "rt+jwt", usernameValue, Map.of("token_use", "refresh", "role", role.name()));
            given(tokenDecoder.decode(encodedAccessToken)).willReturn(decodedToken);

            // When
            var thrown = catchThrowable(() -> revokeAuthenticationService.revokeAuthentication(accessTokenValue));

            // Then
            assertThat(thrown).isInstanceOf(UnexpectedJwtTypeException.class)
                              .hasMessageContaining("is not an access token");

            then(tokenDecoder).should(times(1))
                              .decode(encodedAccessToken);
            then(jwtStore).should(never())
                          .accessTokenExists(any(EncodedToken.class));
        }

        @Test
        void ThrowsJwtAuthenticationNotFoundException_TokenNotFoundInStore() {
            // Given
            var encodedAccessToken = EncodedToken.of(accessTokenValue);
            var decodedToken = new DecodedJwt(accessTokenValue, "at+jwt", usernameValue, Map.of("token_use", "access", "role", role.name()));
            given(tokenDecoder.decode(encodedAccessToken)).willReturn(decodedToken);
            given(jwtStore.accessTokenExists(encodedAccessToken)).willReturn(false);

            // When
            var thrown = catchThrowable(() -> revokeAuthenticationService.revokeAuthentication(accessTokenValue));

            // Then
            assertThat(thrown).isInstanceOf(JwtAuthenticationNotFoundException.class)
                              .hasMessageContaining("Access token not found in active sessions");

            then(tokenDecoder).should(times(1))
                              .decode(encodedAccessToken);
            then(jwtStore).should(times(1))
                          .accessTokenExists(encodedAccessToken);
            then(jwtAuthenticationRepository).should(never())
                                             .findByAccessToken(any(EncodedToken.class));
        }

        @Test
        void ThrowsJwtAuthenticationNotFoundException_TokenNotFoundInDatabase() {
            // Given
            var encodedAccessToken = EncodedToken.of(accessTokenValue);
            var decodedToken = new DecodedJwt(accessTokenValue, "at+jwt", usernameValue, Map.of("token_use", "access", "role", role.name()));
            given(tokenDecoder.decode(encodedAccessToken)).willReturn(decodedToken);
            given(jwtStore.accessTokenExists(encodedAccessToken)).willReturn(true);
            given(jwtAuthenticationRepository.findByAccessToken(encodedAccessToken)).willReturn(Optional.empty());

            // When
            var thrown = catchThrowable(() -> revokeAuthenticationService.revokeAuthentication(accessTokenValue));

            // Then
            assertThat(thrown).isInstanceOf(JwtAuthenticationNotFoundException.class)
                              .hasMessageContaining("JWT authentication not found by access token");

            then(tokenDecoder).should(times(1))
                              .decode(encodedAccessToken);
            then(jwtStore).should(times(1))
                          .accessTokenExists(encodedAccessToken);
            then(jwtAuthenticationRepository).should(times(1))
                                             .findByAccessToken(encodedAccessToken);
            then(jwtStore).should(never())
                          .delete(any(JwtPair.class));
        }

        @Test
        void ThrowsInvalidJwtAuthenticationException_DeactivateTokensFails() {
            // Given
            var encodedAccessToken = EncodedToken.of(accessTokenValue);
            var decodedToken = new DecodedJwt(accessTokenValue, "at+jwt", usernameValue, Map.of("token_use", "access", "role", role.name()));
            var accessToken = createJwt(JwtType.ACCESS_TOKEN, accessTokenValue);
            var refreshToken = createJwt(JwtType.REFRESH_TOKEN, "test.refresh.token");
            var authenticationId = JwtAuthenticationId.of(UUID.randomUUID());
            var jwtPair = JwtPair.of(accessToken, refreshToken);
            var authentication = JwtAuthentication.authenticated(authenticationId, UserId.of(userId), jwtPair);

            given(tokenDecoder.decode(encodedAccessToken)).willReturn(decodedToken);
            given(jwtStore.accessTokenExists(encodedAccessToken)).willReturn(true);
            given(jwtAuthenticationRepository.findByAccessToken(encodedAccessToken)).willReturn(Optional.of(authentication));
            willThrow(new RuntimeException("Redis connection failed")).given(jwtStore)
                                                                      .delete(jwtPair);

            // When
            var thrown = catchThrowable(() -> revokeAuthenticationService.revokeAuthentication(accessTokenValue));

            // Then
            assertThat(thrown).isInstanceOf(InvalidJwtAuthenticationException.class)
                              .hasMessageContaining("Revocation failed: could not deactivate tokens")
                              .hasCauseInstanceOf(RuntimeException.class);

            then(tokenDecoder).should(times(1))
                              .decode(encodedAccessToken);
            then(jwtStore).should(times(1))
                          .accessTokenExists(encodedAccessToken);
            then(jwtAuthenticationRepository).should(times(1))
                                             .findByAccessToken(encodedAccessToken);
            then(jwtStore).should(times(1))
                          .delete(jwtPair);
            then(jwtAuthenticationRepository).should(never())
                                             .deleteById(any(JwtAuthenticationId.class));
            then(jwtStore).should(times(1))
                          .delete(jwtPair);
        }

        @Test
        void ThrowsInvalidJwtAuthenticationException_DeleteAuthenticationReactivateTokensSucceed() {
            // Given
            var encodedAccessToken = EncodedToken.of(accessTokenValue);
            var decodedToken = new DecodedJwt(accessTokenValue, "at+jwt", usernameValue, Map.of("token_use", "access", "role", role.name()));
            var accessToken = createJwt(JwtType.ACCESS_TOKEN, accessTokenValue);
            var refreshToken = createJwt(JwtType.REFRESH_TOKEN, "test.refresh.token");
            var authenticationId = JwtAuthenticationId.of(UUID.randomUUID());
            var jwtPair = JwtPair.of(accessToken, refreshToken);
            var authentication = JwtAuthentication.authenticated(authenticationId, UserId.of(userId), jwtPair);

            given(tokenDecoder.decode(encodedAccessToken)).willReturn(decodedToken);
            given(jwtStore.accessTokenExists(encodedAccessToken)).willReturn(true);
            given(jwtAuthenticationRepository.findByAccessToken(encodedAccessToken)).willReturn(Optional.of(authentication));
            willThrow(new RuntimeException("Database connection failed")).given(jwtAuthenticationRepository)
                                                                         .deleteById(authenticationId);

            // When
            var thrown = catchThrowable(() -> revokeAuthenticationService.revokeAuthentication(accessTokenValue));

            // Then
            assertThat(thrown).isInstanceOf(InvalidJwtAuthenticationException.class)
                              .hasMessageContaining("Revocation failed: could not delete from database")
                              .hasCauseInstanceOf(RuntimeException.class);

            then(tokenDecoder).should(times(1))
                              .decode(encodedAccessToken);
            then(jwtStore).should(times(1))
                          .accessTokenExists(encodedAccessToken);
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
            var decodedToken = new DecodedJwt(accessTokenValue, "at+jwt", usernameValue, Map.of("token_use", "access", "role", role.name()));
            var accessToken = createJwt(JwtType.ACCESS_TOKEN, accessTokenValue);
            var refreshToken = createJwt(JwtType.REFRESH_TOKEN, "test.refresh.token");
            var authenticationId = JwtAuthenticationId.of(UUID.randomUUID());
            var jwtPair = JwtPair.of(accessToken, refreshToken);
            var authentication = JwtAuthentication.authenticated(authenticationId, UserId.of(userId), jwtPair);

            given(tokenDecoder.decode(encodedAccessToken)).willReturn(decodedToken);
            given(jwtStore.accessTokenExists(encodedAccessToken)).willReturn(true);
            given(jwtAuthenticationRepository.findByAccessToken(encodedAccessToken)).willReturn(Optional.of(authentication));

            // When
            assertThatCode(() -> revokeAuthenticationService.revokeAuthentication(accessTokenValue)).doesNotThrowAnyException();

            // Then
            then(tokenDecoder).should(times(1))
                              .decode(encodedAccessToken);
            then(jwtStore).should(times(1))
                          .accessTokenExists(encodedAccessToken);
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

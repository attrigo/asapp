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

package com.bcn.asapp.authentication.infrastructure.authentication.out;

import static com.bcn.asapp.authentication.domain.authentication.JwtClaimNames.ACCESS_TOKEN_USE;
import static com.bcn.asapp.authentication.domain.authentication.JwtClaimNames.REFRESH_TOKEN_USE;
import static com.bcn.asapp.authentication.domain.authentication.JwtClaimNames.ROLE;
import static com.bcn.asapp.authentication.domain.authentication.JwtClaimNames.TOKEN_USE;
import static com.bcn.asapp.authentication.domain.authentication.JwtType.ACCESS_TOKEN;
import static com.bcn.asapp.authentication.domain.authentication.JwtType.REFRESH_TOKEN;
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
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bcn.asapp.authentication.application.authentication.out.JwtAuthenticationRepository;
import com.bcn.asapp.authentication.application.authentication.out.JwtPairStore;
import com.bcn.asapp.authentication.domain.authentication.EncodedToken;
import com.bcn.asapp.authentication.domain.authentication.Expiration;
import com.bcn.asapp.authentication.domain.authentication.Issued;
import com.bcn.asapp.authentication.domain.authentication.Jwt;
import com.bcn.asapp.authentication.domain.authentication.JwtAuthentication;
import com.bcn.asapp.authentication.domain.authentication.JwtAuthenticationId;
import com.bcn.asapp.authentication.domain.authentication.JwtClaims;
import com.bcn.asapp.authentication.domain.authentication.JwtPair;
import com.bcn.asapp.authentication.domain.authentication.Subject;
import com.bcn.asapp.authentication.domain.user.Role;
import com.bcn.asapp.authentication.domain.user.UserId;
import com.bcn.asapp.authentication.infrastructure.security.InvalidJwtAuthenticationException;
import com.bcn.asapp.authentication.infrastructure.security.JwtIssuer;

@ExtendWith(MockitoExtension.class)
class DefaultJwtAuthenticationRefresherTests {

    @Mock
    private JwtIssuer jwtIssuer;

    @Mock
    private JwtAuthenticationRepository jwtAuthenticationRepository;

    @Mock
    private JwtPairStore jwtPairStore;

    @InjectMocks
    private DefaultJwtAuthenticationRefresher defaultJwtAuthenticationRefresher;

    private final Subject subject = Subject.of("user@asapp.com");

    private final Role role = USER;

    private Jwt accessToken;

    private Jwt refreshToken;

    private JwtAuthentication jwtAuthentication;

    @BeforeEach
    void beforeEach() {
        var token = EncodedToken.of("eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0MiJ9.c2lnMg");
        var accessTokenClaims = JwtClaims.of(Map.of(TOKEN_USE, ACCESS_TOKEN_USE, ROLE, role.name()));
        var refreshTokenClaims = JwtClaims.of(Map.of(TOKEN_USE, REFRESH_TOKEN_USE, ROLE, role.name()));
        var issued = Issued.of(Instant.parse("2025-01-01T10:00:00Z"));
        var expiration = Expiration.of(issued, 30000L);
        accessToken = Jwt.of(token, ACCESS_TOKEN, subject, accessTokenClaims, issued, expiration);
        refreshToken = Jwt.of(token, REFRESH_TOKEN, subject, refreshTokenClaims, issued, expiration);

        var jwtAuthenticationId = JwtAuthenticationId.of(UUID.fromString("9c07df84-5c66-49c9-a8a8-78f1b4cf635c"));
        var userId = UserId.of(UUID.fromString("a8fc2926-473a-4e84-b5ce-3e063e7d2aab"));
        jwtAuthentication = JwtAuthentication.authenticated(jwtAuthenticationId, userId, accessToken, refreshToken);
    }

    @Nested
    class RefreshAuthentication {

        @Test
        void ThenThrowsInvalidJwtAuthenticationException_GivenAccessTokenIssuanceFails() {
            // Given
            willThrow(new RuntimeException("Token issuance failed")).given(jwtIssuer)
                                                                    .issueAccessToken(subject, role);

            // When
            var thrown = catchThrowable(() -> defaultJwtAuthenticationRefresher.refreshAuthentication(jwtAuthentication));

            // Then
            assertThat(thrown).isInstanceOf(InvalidJwtAuthenticationException.class)
                              .hasMessageContaining("Authentication could not be refreshed due to");

            then(jwtIssuer).should(times(1))
                           .issueAccessToken(subject, role);
            then(jwtIssuer).should(never())
                           .issueRefreshToken(any(), any());
            then(jwtAuthenticationRepository).should(never())
                                             .save(any(JwtAuthentication.class));
            then(jwtPairStore).should(never())
                              .delete(any(JwtPair.class));
            then(jwtPairStore).should(never())
                              .save(any(JwtPair.class));
        }

        @Test
        void ThenThrowsInvalidJwtAuthenticationException_GivenRefreshTokenIssuanceFails() {
            // Given
            given(jwtIssuer.issueAccessToken(subject, role)).willReturn(accessToken);

            willThrow(new RuntimeException("Token issuance failed")).given(jwtIssuer)
                                                                    .issueRefreshToken(subject, role);

            // When
            var thrown = catchThrowable(() -> defaultJwtAuthenticationRefresher.refreshAuthentication(jwtAuthentication));

            // Then
            assertThat(thrown).isInstanceOf(InvalidJwtAuthenticationException.class)
                              .hasMessageContaining("Authentication could not be refreshed due to");

            then(jwtIssuer).should(times(1))
                           .issueAccessToken(subject, role);
            then(jwtIssuer).should(times(1))
                           .issueRefreshToken(subject, role);
            then(jwtAuthenticationRepository).should(never())
                                             .save(any(JwtAuthentication.class));
            then(jwtPairStore).should(never())
                              .delete(any(JwtPair.class));
            then(jwtPairStore).should(never())
                              .save(any(JwtPair.class));
        }

        @Test
        void ThenThrowsInvalidJwtAuthenticationException_GivenRepositorySaveFails() {
            // Given
            given(jwtIssuer.issueAccessToken(subject, role)).willReturn(accessToken);

            given(jwtIssuer.issueRefreshToken(subject, role)).willReturn(refreshToken);

            willThrow(new RuntimeException("Repository save failed")).given(jwtAuthenticationRepository)
                                                                     .save(jwtAuthentication);

            // When
            var thrown = catchThrowable(() -> defaultJwtAuthenticationRefresher.refreshAuthentication(jwtAuthentication));

            // Then
            assertThat(thrown).isInstanceOf(InvalidJwtAuthenticationException.class)
                              .hasMessageContaining("Authentication could not be refreshed due to");

            then(jwtIssuer).should(times(1))
                           .issueAccessToken(subject, role);
            then(jwtIssuer).should(times(1))
                           .issueRefreshToken(subject, role);
            then(jwtAuthenticationRepository).should(times(1))
                                             .save(jwtAuthentication);
            then(jwtPairStore).should(never())
                              .delete(any(JwtPair.class));
            then(jwtPairStore).should(never())
                              .save(any(JwtPair.class));
        }

        @Test
        void ThenThrowsInvalidJwtAuthenticationException_GivenStoreDeleteFails() {
            // Given
            var oldJwtPair = jwtAuthentication.getJwtPair();

            given(jwtIssuer.issueAccessToken(subject, role)).willReturn(accessToken);

            given(jwtIssuer.issueRefreshToken(subject, role)).willReturn(refreshToken);

            given(jwtAuthenticationRepository.save(jwtAuthentication)).willReturn(jwtAuthentication);

            willThrow(new RuntimeException("Redis connection failed")).given(jwtPairStore)
                                                                      .delete(any(JwtPair.class));

            // When
            var thrown = catchThrowable(() -> defaultJwtAuthenticationRefresher.refreshAuthentication(jwtAuthentication));

            // Then
            assertThat(thrown).isInstanceOf(InvalidJwtAuthenticationException.class)
                              .hasMessageContaining("Authentication could not be refreshed due to");

            then(jwtIssuer).should(times(1))
                           .issueAccessToken(subject, role);
            then(jwtIssuer).should(times(1))
                           .issueRefreshToken(subject, role);
            then(jwtAuthenticationRepository).should(times(1))
                                             .save(jwtAuthentication);
            then(jwtPairStore).should(times(1))
                              .delete(oldJwtPair);
            then(jwtPairStore).should(never())
                              .save(any(JwtPair.class));
        }

        @Test
        void ThenThrowsInvalidJwtAuthenticationException_GivenStoreSaveFails() {
            // Given
            var oldJwtPair = jwtAuthentication.getJwtPair();

            given(jwtIssuer.issueAccessToken(subject, role)).willReturn(accessToken);

            given(jwtIssuer.issueRefreshToken(subject, role)).willReturn(refreshToken);

            given(jwtAuthenticationRepository.save(jwtAuthentication)).willReturn(jwtAuthentication);

            willThrow(new RuntimeException("Redis connection failed")).given(jwtPairStore)
                                                                      .save(any(JwtPair.class));

            // When
            var thrown = catchThrowable(() -> defaultJwtAuthenticationRefresher.refreshAuthentication(jwtAuthentication));

            // Then
            assertThat(thrown).isInstanceOf(InvalidJwtAuthenticationException.class)
                              .hasMessageContaining("Authentication could not be refreshed due to");

            then(jwtIssuer).should(times(1))
                           .issueAccessToken(subject, role);
            then(jwtIssuer).should(times(1))
                           .issueRefreshToken(subject, role);
            then(jwtAuthenticationRepository).should(times(1))
                                             .save(jwtAuthentication);
            then(jwtPairStore).should(times(1))
                              .delete(oldJwtPair);
            then(jwtPairStore).should(times(1))
                              .save(any(JwtPair.class));
        }

        @Test
        void ThenRefreshesAuthentication_GivenJwtAuthenticationIsValid() {
            // Given
            var oldJwtPair = jwtAuthentication.getJwtPair();

            given(jwtIssuer.issueAccessToken(subject, role)).willReturn(accessToken);

            given(jwtIssuer.issueRefreshToken(subject, role)).willReturn(refreshToken);

            given(jwtAuthenticationRepository.save(jwtAuthentication)).willReturn(jwtAuthentication);

            // When
            var actual = defaultJwtAuthenticationRefresher.refreshAuthentication(jwtAuthentication);

            // Then
            assertThat(actual).isEqualTo(jwtAuthentication);
            assertThat(actual.accessToken()).isEqualTo(accessToken);
            assertThat(actual.refreshToken()).isEqualTo(refreshToken);

            then(jwtIssuer).should(times(1))
                           .issueAccessToken(subject, role);
            then(jwtIssuer).should(times(1))
                           .issueRefreshToken(subject, role);
            then(jwtAuthenticationRepository).should(times(1))
                                             .save(jwtAuthentication);
            then(jwtPairStore).should(times(1))
                              .delete(oldJwtPair);
            then(jwtPairStore).should(times(1))
                              .save(actual.getJwtPair());
        }

    }

}

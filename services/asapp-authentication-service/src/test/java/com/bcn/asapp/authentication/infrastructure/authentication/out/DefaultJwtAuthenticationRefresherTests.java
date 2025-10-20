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

import static com.bcn.asapp.authentication.domain.authentication.Jwt.ACCESS_TOKEN_USE_CLAIM_VALUE;
import static com.bcn.asapp.authentication.domain.authentication.Jwt.REFRESH_TOKEN_USE_CLAIM_VALUE;
import static com.bcn.asapp.authentication.domain.authentication.Jwt.ROLE_CLAIM_NAME;
import static com.bcn.asapp.authentication.domain.authentication.Jwt.TOKEN_USE_CLAIM_NAME;
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
import com.bcn.asapp.authentication.domain.authentication.EncodedToken;
import com.bcn.asapp.authentication.domain.authentication.Expiration;
import com.bcn.asapp.authentication.domain.authentication.Issued;
import com.bcn.asapp.authentication.domain.authentication.Jwt;
import com.bcn.asapp.authentication.domain.authentication.JwtAuthentication;
import com.bcn.asapp.authentication.domain.authentication.JwtAuthenticationId;
import com.bcn.asapp.authentication.domain.authentication.JwtClaims;
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

    @InjectMocks
    private DefaultJwtAuthenticationRefresher defaultJwtAuthenticationRefresher;

    private Subject subject;

    private Role role;

    private Jwt newAccessToken;

    private Jwt newRefreshToken;

    private JwtAuthentication jwtAuthentication;

    @BeforeEach
    void beforeEach() {
        var token = EncodedToken.of("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.encoded");
        var newToken = EncodedToken.of("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.newEncoded");
        subject = Subject.of("user@asapp.com");
        role = USER;
        var accessTokenClaims = JwtClaims.of(Map.of(TOKEN_USE_CLAIM_NAME, ACCESS_TOKEN_USE_CLAIM_VALUE, ROLE_CLAIM_NAME, role.name()));
        var refreshTokenClaims = JwtClaims.of(Map.of(TOKEN_USE_CLAIM_NAME, REFRESH_TOKEN_USE_CLAIM_VALUE, ROLE_CLAIM_NAME, role.name()));
        var issued = Issued.now();
        var expiration = Expiration.of(issued, 1000L);

        newAccessToken = Jwt.of(newToken, ACCESS_TOKEN, subject, accessTokenClaims, issued, expiration);
        newRefreshToken = Jwt.of(newToken, REFRESH_TOKEN, subject, refreshTokenClaims, issued, expiration);

        var jwtAuthenticationId = JwtAuthenticationId.of(UUID.randomUUID());
        var userId = UserId.of(UUID.randomUUID());
        var accessToken = Jwt.of(token, ACCESS_TOKEN, subject, accessTokenClaims, issued, expiration);
        var refreshToken = Jwt.of(token, REFRESH_TOKEN, subject, refreshTokenClaims, issued, expiration);

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
                                             .save(any());
        }

        @Test
        void ThenThrowsInvalidJwtAuthenticationException_GivenRefreshTokenIssuanceFails() {
            // Given
            given(jwtIssuer.issueAccessToken(subject, role)).willReturn(newAccessToken);
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
                                             .save(any());
        }

        @Test
        void ThenThrowsInvalidJwtAuthenticationException_GivenRepositorySaveFails() {
            // Given
            given(jwtIssuer.issueAccessToken(subject, role)).willReturn(newAccessToken);
            given(jwtIssuer.issueRefreshToken(subject, role)).willReturn(newRefreshToken);
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
        }

        @Test
        void ThenRefreshesAuthentication_GivenJwtAuthenticationIsValid() {
            // Given
            given(jwtIssuer.issueAccessToken(subject, role)).willReturn(newAccessToken);
            given(jwtIssuer.issueRefreshToken(subject, role)).willReturn(newRefreshToken);
            given(jwtAuthenticationRepository.save(jwtAuthentication)).willReturn(jwtAuthentication);

            // When
            var actual = defaultJwtAuthenticationRefresher.refreshAuthentication(jwtAuthentication);

            // Then
            assertThat(actual).isEqualTo(jwtAuthentication);
            assertThat(actual.accessToken()).isEqualTo(newAccessToken);
            assertThat(actual.refreshToken()).isEqualTo(newRefreshToken);

            then(jwtIssuer).should(times(1))
                           .issueAccessToken(subject, role);
            then(jwtIssuer).should(times(1))
                           .issueRefreshToken(subject, role);
            then(jwtAuthenticationRepository).should(times(1))
                                             .save(jwtAuthentication);
        }

    }

}

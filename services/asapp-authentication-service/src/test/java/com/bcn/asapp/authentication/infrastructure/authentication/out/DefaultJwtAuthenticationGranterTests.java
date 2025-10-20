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
import org.springframework.security.authentication.BadCredentialsException;

import com.bcn.asapp.authentication.application.authentication.out.JwtAuthenticationRepository;
import com.bcn.asapp.authentication.domain.authentication.EncodedToken;
import com.bcn.asapp.authentication.domain.authentication.Expiration;
import com.bcn.asapp.authentication.domain.authentication.Issued;
import com.bcn.asapp.authentication.domain.authentication.Jwt;
import com.bcn.asapp.authentication.domain.authentication.JwtAuthentication;
import com.bcn.asapp.authentication.domain.authentication.JwtAuthenticationId;
import com.bcn.asapp.authentication.domain.authentication.JwtClaims;
import com.bcn.asapp.authentication.domain.authentication.Subject;
import com.bcn.asapp.authentication.domain.authentication.UserAuthentication;
import com.bcn.asapp.authentication.domain.user.UserId;
import com.bcn.asapp.authentication.domain.user.Username;
import com.bcn.asapp.authentication.infrastructure.security.JwtIssuer;

@ExtendWith(MockitoExtension.class)
class DefaultJwtAuthenticationGranterTests {

    @Mock
    private JwtIssuer jwtIssuer;

    @Mock
    private JwtAuthenticationRepository jwtAuthenticationRepository;

    @InjectMocks
    private DefaultJwtAuthenticationGranter defaultAuthenticationGranter;

    private UserId userId;

    private Jwt accessToken;

    private Jwt refreshToken;

    private UserAuthentication userAuthentication;

    @BeforeEach
    void beforeEach() {
        var token = EncodedToken.of("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.encoded");
        var subject = Subject.of("user@asapp.com");
        var accessTokenClaims = JwtClaims.of(Map.of(TOKEN_USE_CLAIM_NAME, ACCESS_TOKEN_USE_CLAIM_VALUE));
        var refreshTokenClaims = JwtClaims.of(Map.of(TOKEN_USE_CLAIM_NAME, REFRESH_TOKEN_USE_CLAIM_VALUE));
        var issued = Issued.now();
        var expiration = Expiration.of(issued, 1000L);

        userId = UserId.of(UUID.randomUUID());
        accessToken = Jwt.of(token, ACCESS_TOKEN, subject, accessTokenClaims, issued, expiration);
        refreshToken = Jwt.of(token, REFRESH_TOKEN, subject, refreshTokenClaims, issued, expiration);

        var username = Username.of("user@asapp.com");

        userAuthentication = UserAuthentication.authenticated(userId, username, USER);
    }

    @Nested
    class GrantAuthentication {

        @Test
        void ThenThrowsBadCredentialsException_GivenAccessTokenIssuanceFails() {
            // Given
            willThrow(new RuntimeException("Token issuance failed")).given(jwtIssuer)
                                                                    .issueAccessToken(userAuthentication);

            // When
            var thrown = catchThrowable(() -> defaultAuthenticationGranter.grantAuthentication(userAuthentication));

            // Then
            assertThat(thrown).isInstanceOf(BadCredentialsException.class)
                              .hasMessageContaining("Authentication could not be granted due to");

            then(jwtIssuer).should(times(1))
                           .issueAccessToken(userAuthentication);
            then(jwtIssuer).should(never())
                           .issueRefreshToken(any());
            then(jwtAuthenticationRepository).should(never())
                                             .save(any(JwtAuthentication.class));
        }

        @Test
        void ThenThrowsBadCredentialsException_GivenRefreshTokenIssuanceFails() {
            // Given
            given(jwtIssuer.issueAccessToken(userAuthentication)).willReturn(accessToken);
            willThrow(new RuntimeException("Token issuance failed")).given(jwtIssuer)
                                                                    .issueRefreshToken(userAuthentication);

            // When
            var thrown = catchThrowable(() -> defaultAuthenticationGranter.grantAuthentication(userAuthentication));

            // Then
            assertThat(thrown).isInstanceOf(BadCredentialsException.class)
                              .hasMessageContaining("Authentication could not be granted due to");

            then(jwtIssuer).should(times(1))
                           .issueAccessToken(userAuthentication);
            then(jwtIssuer).should(times(1))
                           .issueRefreshToken(userAuthentication);
            then(jwtAuthenticationRepository).should(never())
                                             .save(any(JwtAuthentication.class));
        }

        @Test
        void ThenThrowsBadCredentialsException_GivenRepositorySaveFails() {
            // Given
            given(jwtIssuer.issueAccessToken(userAuthentication)).willReturn(accessToken);
            given(jwtIssuer.issueRefreshToken(userAuthentication)).willReturn(refreshToken);
            willThrow(new RuntimeException("Repository save failed")).given(jwtAuthenticationRepository)
                                                                     .save(any(JwtAuthentication.class));

            // When
            var thrown = catchThrowable(() -> defaultAuthenticationGranter.grantAuthentication(userAuthentication));

            // Then
            assertThat(thrown).isInstanceOf(BadCredentialsException.class)
                              .hasMessageContaining("Authentication could not be granted due to");

            then(jwtIssuer).should(times(1))
                           .issueAccessToken(userAuthentication);
            then(jwtIssuer).should(times(1))
                           .issueRefreshToken(userAuthentication);
            then(jwtAuthenticationRepository).should(times(1))
                                             .save(any(JwtAuthentication.class));
        }

        @Test
        void ThenGrantsAuthentication_GivenUserAuthenticationIsValid() {
            // Given
            var jwtAuthentication = JwtAuthentication.authenticated(JwtAuthenticationId.of(UUID.randomUUID()), userId, accessToken, refreshToken);

            given(jwtIssuer.issueAccessToken(userAuthentication)).willReturn(accessToken);
            given(jwtIssuer.issueRefreshToken(userAuthentication)).willReturn(refreshToken);
            given(jwtAuthenticationRepository.save(any(JwtAuthentication.class))).willReturn(jwtAuthentication);

            // When
            var actual = defaultAuthenticationGranter.grantAuthentication(userAuthentication);

            // Then
            assertThat(actual).isEqualTo(jwtAuthentication);

            then(jwtIssuer).should(times(1))
                           .issueAccessToken(userAuthentication);
            then(jwtIssuer).should(times(1))
                           .issueRefreshToken(userAuthentication);
            then(jwtAuthenticationRepository).should(times(1))
                                             .save(any(JwtAuthentication.class));
        }

    }

}

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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.util.Map;
import java.util.Optional;
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
import com.bcn.asapp.authentication.domain.user.UserId;
import com.bcn.asapp.authentication.infrastructure.security.InvalidJwtException;
import com.bcn.asapp.authentication.infrastructure.security.JwtAuthenticationNotFoundException;
import com.bcn.asapp.authentication.infrastructure.security.JwtDecoder;
import com.bcn.asapp.authentication.infrastructure.security.UnexpectedJwtTypeException;

@ExtendWith(MockitoExtension.class)
class DefaultJwtVerifierTests {

    @Mock
    private JwtDecoder jwtDecoder;

    @Mock
    private JwtAuthenticationRepository jwtAuthenticationRepository;

    @InjectMocks
    private DefaultJwtVerifier defaultJwtVerifier;

    private EncodedToken encodedAccessToken;

    private EncodedToken encodedRefreshToken;

    private Jwt accessToken;

    private Jwt refreshToken;

    private JwtAuthentication jwtAuthentication;

    @BeforeEach
    void beforeEach() {
        encodedAccessToken = EncodedToken.of("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.accessToken");
        encodedRefreshToken = EncodedToken.of("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.refreshToken");

        var subject = Subject.of("user@asapp.com");
        var accessTokenClaims = JwtClaims.of(Map.of(TOKEN_USE_CLAIM_NAME, ACCESS_TOKEN_USE_CLAIM_VALUE));
        var refreshTokenClaims = JwtClaims.of(Map.of(TOKEN_USE_CLAIM_NAME, REFRESH_TOKEN_USE_CLAIM_VALUE));
        var issued = Issued.now();
        var expiration = Expiration.of(issued, 1000L);

        accessToken = Jwt.of(encodedAccessToken, ACCESS_TOKEN, subject, accessTokenClaims, issued, expiration);
        refreshToken = Jwt.of(encodedRefreshToken, REFRESH_TOKEN, subject, refreshTokenClaims, issued, expiration);

        var jwtAuthenticationId = JwtAuthenticationId.of(UUID.randomUUID());
        var userId = UserId.of(UUID.randomUUID());
        jwtAuthentication = JwtAuthentication.authenticated(jwtAuthenticationId, userId, accessToken, refreshToken);
    }

    @Nested
    class VerifyAccessToken {

        @Test
        void ThenThrowsInvalidJwtException_GivenDecoderFails() {
            // Given
            willThrow(new RuntimeException("Decoder failed")).given(jwtDecoder)
                                                             .decode(encodedAccessToken);

            // When
            var thrown = catchThrowable(() -> defaultJwtVerifier.verifyAccessToken(encodedAccessToken));

            // Then
            assertThat(thrown).isInstanceOf(InvalidJwtException.class)
                              .hasMessageContaining("Access token is not valid");

            then(jwtDecoder).should(times(1))
                            .decode(encodedAccessToken);
            then(jwtAuthenticationRepository).should(never())
                                             .findByAccessToken(any(EncodedToken.class));
        }

        @Test
        void ThenThrowsInvalidJwtException_GivenTokenIsNotAccessToken() {
            // Given
            given(jwtDecoder.decode(encodedAccessToken)).willReturn(refreshToken);

            // When
            var thrown = catchThrowable(() -> defaultJwtVerifier.verifyAccessToken(encodedAccessToken));

            // Then
            assertThat(thrown).isInstanceOf(InvalidJwtException.class)
                              .hasMessageContaining("Access token is not valid")
                              .hasCauseInstanceOf(UnexpectedJwtTypeException.class);

            then(jwtDecoder).should(times(1))
                            .decode(encodedAccessToken);
            then(jwtAuthenticationRepository).should(never())
                                             .findByAccessToken(any(EncodedToken.class));
        }

        @Test
        void ThenThrowsInvalidJwtException_GivenAuthenticationNotFoundInRepository() {
            // Given
            given(jwtDecoder.decode(encodedAccessToken)).willReturn(accessToken);
            given(jwtAuthenticationRepository.findByAccessToken(encodedAccessToken)).willReturn(Optional.empty());

            // When
            var thrown = catchThrowable(() -> defaultJwtVerifier.verifyAccessToken(encodedAccessToken));

            // Then
            assertThat(thrown).isInstanceOf(InvalidJwtException.class)
                              .hasMessageContaining("Access token is not valid")
                              .hasCauseInstanceOf(JwtAuthenticationNotFoundException.class);

            then(jwtDecoder).should(times(1))
                            .decode(encodedAccessToken);
            then(jwtAuthenticationRepository).should(times(1))
                                             .findByAccessToken(encodedAccessToken);
        }

        @Test
        void ThenVerifiesAccessToken_GivenAccessTokenIsValid() {
            // Given
            given(jwtDecoder.decode(encodedAccessToken)).willReturn(accessToken);
            given(jwtAuthenticationRepository.findByAccessToken(encodedAccessToken)).willReturn(Optional.of(jwtAuthentication));

            // When
            var result = defaultJwtVerifier.verifyAccessToken(encodedAccessToken);

            // Then
            assertThat(result).isEqualTo(jwtAuthentication);

            then(jwtDecoder).should(times(1))
                            .decode(encodedAccessToken);
            then(jwtAuthenticationRepository).should(times(1))
                                             .findByAccessToken(encodedAccessToken);
        }

    }

    @Nested
    class VerifyRefreshToken {

        @Test
        void ThenThrowsInvalidJwtException_GivenDecoderFails() {
            // Given
            willThrow(new RuntimeException("Decoder failed")).given(jwtDecoder)
                                                             .decode(encodedRefreshToken);

            // When
            var thrown = catchThrowable(() -> defaultJwtVerifier.verifyRefreshToken(encodedRefreshToken));

            // Then
            assertThat(thrown).isInstanceOf(InvalidJwtException.class)
                              .hasMessageContaining("Refresh token is not valid");

            then(jwtDecoder).should(times(1))
                            .decode(encodedRefreshToken);
            then(jwtAuthenticationRepository).should(never())
                                             .findByRefreshToken(any(EncodedToken.class));
        }

        @Test
        void ThenThrowsInvalidJwtException_GivenTokenIsNotRefreshToken() {
            // Given
            given(jwtDecoder.decode(encodedRefreshToken)).willReturn(accessToken);

            // When
            var thrown = catchThrowable(() -> defaultJwtVerifier.verifyRefreshToken(encodedRefreshToken));

            // Then
            assertThat(thrown).isInstanceOf(InvalidJwtException.class)
                              .hasMessageContaining("Refresh token is not valid")
                              .hasCauseInstanceOf(UnexpectedJwtTypeException.class);

            then(jwtDecoder).should(times(1))
                            .decode(encodedRefreshToken);
            then(jwtAuthenticationRepository).should(never())
                                             .findByRefreshToken(any(EncodedToken.class));
        }

        @Test
        void ThenThrowsInvalidJwtException_GivenAuthenticationNotFoundInRepository() {
            // Given
            given(jwtDecoder.decode(encodedRefreshToken)).willReturn(refreshToken);
            given(jwtAuthenticationRepository.findByRefreshToken(encodedRefreshToken)).willReturn(Optional.empty());

            // When
            var thrown = catchThrowable(() -> defaultJwtVerifier.verifyRefreshToken(encodedRefreshToken));

            // Then
            assertThat(thrown).isInstanceOf(InvalidJwtException.class)
                              .hasMessageContaining("Refresh token is not valid")
                              .hasCauseInstanceOf(JwtAuthenticationNotFoundException.class);

            then(jwtDecoder).should(times(1))
                            .decode(encodedRefreshToken);
            then(jwtAuthenticationRepository).should(times(1))
                                             .findByRefreshToken(encodedRefreshToken);
        }

        @Test
        void ThenVerifiesRefreshToken_GivenRefreshTokenIsValid() {
            // Given
            given(jwtDecoder.decode(encodedRefreshToken)).willReturn(refreshToken);
            given(jwtAuthenticationRepository.findByRefreshToken(encodedRefreshToken)).willReturn(Optional.of(jwtAuthentication));

            // When
            var result = defaultJwtVerifier.verifyRefreshToken(encodedRefreshToken);

            // Then
            assertThat(result).isEqualTo(jwtAuthentication);

            then(jwtDecoder).should(times(1))
                            .decode(encodedRefreshToken);
            then(jwtAuthenticationRepository).should(times(1))
                                             .findByRefreshToken(encodedRefreshToken);
        }

    }

}

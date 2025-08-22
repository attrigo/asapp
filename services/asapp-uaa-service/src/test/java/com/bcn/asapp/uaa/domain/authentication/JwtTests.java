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

package com.bcn.asapp.uaa.domain.authentication;

import static com.bcn.asapp.uaa.domain.authentication.Jwt.ACCESS_TOKEN_USE_CLAIM_VALUE;
import static com.bcn.asapp.uaa.domain.authentication.Jwt.REFRESH_TOKEN_USE_CLAIM_VALUE;
import static com.bcn.asapp.uaa.domain.authentication.Jwt.ROLE_CLAIM_NAME;
import static com.bcn.asapp.uaa.domain.authentication.Jwt.TOKEN_USE_CLAIM_NAME;
import static com.bcn.asapp.uaa.domain.authentication.JwtType.ACCESS_TOKEN;
import static com.bcn.asapp.uaa.domain.authentication.JwtType.REFRESH_TOKEN;
import static com.bcn.asapp.uaa.domain.user.Role.ADMIN;
import static com.bcn.asapp.uaa.domain.user.Role.USER;
import static java.time.temporal.ChronoUnit.DAYS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class JwtTests {

    private final EncodedToken encodedToken = EncodedToken.of("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.encoded");

    private final Subject subject = Subject.of("user");

    private final JwtClaims accessTokenClaims = JwtClaims.of(Map.of(TOKEN_USE_CLAIM_NAME, ACCESS_TOKEN_USE_CLAIM_VALUE, ROLE_CLAIM_NAME, ADMIN.name()));

    private final JwtClaims refreshTokenClaims = JwtClaims.of(Map.of(TOKEN_USE_CLAIM_NAME, REFRESH_TOKEN_USE_CLAIM_VALUE, ROLE_CLAIM_NAME, USER.name()));

    private final Issued issued = Issued.of(Instant.parse("2024-01-15T10:00:00Z"));

    private final Expiration expiration = Expiration.of(issued, 1000L);

    @Nested
    class CreateJwtWithConstructor {

        @Test
        void ThenThrowsIllegalArgumentException_GivenEncodedTokenIsNull() {
            // When
            var thrown = catchThrowable(() -> new Jwt(null, ACCESS_TOKEN, subject, accessTokenClaims, issued, expiration));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Encoded token must not be null");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenTypeIsNull() {
            // When
            var thrown = catchThrowable(() -> new Jwt(encodedToken, null, subject, accessTokenClaims, issued, expiration));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Type must not be null");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenSubjectIsNull() {
            // When
            var thrown = catchThrowable(() -> new Jwt(encodedToken, ACCESS_TOKEN, null, accessTokenClaims, issued, expiration));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Subject must not be null");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenClaimsIsNull() {
            // When
            var thrown = catchThrowable(() -> new Jwt(encodedToken, ACCESS_TOKEN, subject, null, issued, expiration));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Claims must not be null");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenClaimsMissingTokenUseClaim() {
            // Given
            var invalidClaims = JwtClaims.of(Map.of(ROLE_CLAIM_NAME, ADMIN.name()));

            // When
            var thrown = catchThrowable(() -> new Jwt(encodedToken, ACCESS_TOKEN, subject, invalidClaims, issued, expiration));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Claims must contain the mandatory token use claim");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenClaimsHasInvalidTokenUseClaim() {
            // Given
            var invalidClaims = JwtClaims.of(Map.of(TOKEN_USE_CLAIM_NAME, "invalid", ROLE_CLAIM_NAME, ADMIN.name()));

            // When
            var thrown = catchThrowable(() -> new Jwt(encodedToken, ACCESS_TOKEN, subject, invalidClaims, issued, expiration));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessageContaining("Invalid JWT token use claim")
                              .hasMessageContaining("access")
                              .hasMessageContaining("refresh")
                              .hasMessageContaining("invalid");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenAccessTokenTypeWithRefreshTokenUseClaim() {
            // When
            var thrown = catchThrowable(() -> new Jwt(encodedToken, ACCESS_TOKEN, subject, refreshTokenClaims, issued, expiration));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessageContaining("Token type")
                              .hasMessageContaining("token_use claim")
                              .hasMessageContaining("do not match");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenRefreshTokenTypeWithAccessTokenUseClaim() {
            // When
            var thrown = catchThrowable(() -> new Jwt(encodedToken, REFRESH_TOKEN, subject, accessTokenClaims, issued, expiration));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessageContaining("Token type")
                              .hasMessageContaining("token_use claim")
                              .hasMessageContaining("do not match");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenExpirationBeforeIssued() {
            // Given
            var invalidExpiration = new Expiration(issued.value()
                                                         .minus(1, DAYS));

            // When
            var thrown = catchThrowable(() -> new Jwt(encodedToken, ACCESS_TOKEN, subject, accessTokenClaims, issued, invalidExpiration));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Issued date must be before expiration date");
        }

        @Test
        void ThenReturnsJwt_GivenParametersAreValidOnAccessToken() {
            // When
            var actual = new Jwt(encodedToken, ACCESS_TOKEN, subject, accessTokenClaims, issued, expiration);

            // Then
            assertThat(actual).isNotNull();
            assertThat(actual.encodedToken()).isEqualTo(encodedToken);
            assertThat(actual.type()).isEqualTo(ACCESS_TOKEN);
            assertThat(actual.subject()).isEqualTo(subject);
            assertThat(actual.claims()).isEqualTo(accessTokenClaims);
            assertThat(actual.issued()).isEqualTo(issued);
            assertThat(actual.expiration()).isEqualTo(expiration);
        }

        @Test
        void ThenReturnsJwt_GivenParametersAreValidOnRefreshToken() {
            // When
            var actual = new Jwt(encodedToken, REFRESH_TOKEN, subject, refreshTokenClaims, issued, expiration);

            // Then
            assertThat(actual).isNotNull();
            assertThat(actual.encodedToken()).isEqualTo(encodedToken);
            assertThat(actual.type()).isEqualTo(REFRESH_TOKEN);
            assertThat(actual.subject()).isEqualTo(subject);
            assertThat(actual.claims()).isEqualTo(refreshTokenClaims);
            assertThat(actual.issued()).isEqualTo(issued);
            assertThat(actual.expiration()).isEqualTo(expiration);
        }

    }

    @Nested
    class CreateJwtWithFactoryMethod {

        @Test
        void ThenThrowsIllegalArgumentException_GivenEncodedTokenIsNull() {
            // When
            var thrown = catchThrowable(() -> Jwt.of(null, ACCESS_TOKEN, subject, accessTokenClaims, issued, expiration));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Encoded token must not be null");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenTypeIsNull() {
            // When
            var thrown = catchThrowable(() -> Jwt.of(encodedToken, null, subject, accessTokenClaims, issued, expiration));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Type must not be null");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenSubjectIsNull() {
            // When
            var thrown = catchThrowable(() -> Jwt.of(encodedToken, ACCESS_TOKEN, null, accessTokenClaims, issued, expiration));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Subject must not be null");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenClaimsIsNull() {
            // When
            var thrown = catchThrowable(() -> Jwt.of(encodedToken, ACCESS_TOKEN, subject, null, issued, expiration));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Claims must not be null");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenClaimsMissingTokenUseClaim() {
            // Given
            var invalidClaims = JwtClaims.of(Map.of(ROLE_CLAIM_NAME, ADMIN.name()));

            // When
            var thrown = catchThrowable(() -> Jwt.of(encodedToken, ACCESS_TOKEN, subject, invalidClaims, issued, expiration));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Claims must contain the mandatory token use claim");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenClaimsHasInvalidTokenUseClaim() {
            // Given
            var invalidClaims = JwtClaims.of(Map.of(TOKEN_USE_CLAIM_NAME, "invalid", ROLE_CLAIM_NAME, ADMIN.name()));

            // When
            var thrown = catchThrowable(() -> Jwt.of(encodedToken, ACCESS_TOKEN, subject, invalidClaims, issued, expiration));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessageContaining("Invalid JWT token use claim")
                              .hasMessageContaining("access")
                              .hasMessageContaining("refresh")
                              .hasMessageContaining("invalid");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenAccessTokenTypeWithRefreshTokenUseClaim() {
            // When
            var thrown = catchThrowable(() -> Jwt.of(encodedToken, ACCESS_TOKEN, subject, refreshTokenClaims, issued, expiration));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessageContaining("Token type")
                              .hasMessageContaining("token_use claim")
                              .hasMessageContaining("do not match");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenRefreshTokenTypeWithAccessTokenUseClaim() {
            // When
            var thrown = catchThrowable(() -> Jwt.of(encodedToken, REFRESH_TOKEN, subject, accessTokenClaims, issued, expiration));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessageContaining("Token type")
                              .hasMessageContaining("token_use claim")
                              .hasMessageContaining("do not match");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenExpirationBeforeIssued() {
            // Given
            var invalidExpiration = new Expiration(issued.value()
                                                         .minus(1, DAYS));

            // When
            var thrown = catchThrowable(() -> Jwt.of(encodedToken, ACCESS_TOKEN, subject, accessTokenClaims, issued, invalidExpiration));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Issued date must be before expiration date");
        }

        @Test
        void ThenReturnsJwt_GivenParametersAreValidOnAccessToken() {
            // When
            var actual = Jwt.of(encodedToken, ACCESS_TOKEN, subject, accessTokenClaims, issued, expiration);

            // Then
            assertThat(actual).isNotNull();
            assertThat(actual.encodedToken()).isEqualTo(encodedToken);
            assertThat(actual.type()).isEqualTo(ACCESS_TOKEN);
            assertThat(actual.subject()).isEqualTo(subject);
            assertThat(actual.claims()).isEqualTo(accessTokenClaims);
            assertThat(actual.issued()).isEqualTo(issued);
            assertThat(actual.expiration()).isEqualTo(expiration);
        }

        @Test
        void ThenReturnsJwt_GivenParametersAreValidOnRefreshToken() {
            // When
            var actual = Jwt.of(encodedToken, REFRESH_TOKEN, subject, refreshTokenClaims, issued, expiration);

            // Then
            assertThat(actual).isNotNull();
            assertThat(actual.encodedToken()).isEqualTo(encodedToken);
            assertThat(actual.type()).isEqualTo(REFRESH_TOKEN);
            assertThat(actual.subject()).isEqualTo(subject);
            assertThat(actual.claims()).isEqualTo(refreshTokenClaims);
            assertThat(actual.issued()).isEqualTo(issued);
            assertThat(actual.expiration()).isEqualTo(expiration);
        }

    }

    @Nested
    class CheckIsAccessToken {

        @Test
        void ThenReturnsTrue_GivenJwtIsAccessToken() {
            // Given
            var jwt = Jwt.of(encodedToken, ACCESS_TOKEN, subject, accessTokenClaims, issued, expiration);

            // When
            var actual = jwt.isAccessToken();

            // Then
            assertThat(actual).isTrue();
        }

        @Test
        void ThenReturnsFalse_GivenJwtIsRefreshToken() {
            // Given
            var jwt = Jwt.of(encodedToken, REFRESH_TOKEN, subject, refreshTokenClaims, issued, expiration);

            // When
            var actual = jwt.isAccessToken();

            // Then
            assertThat(actual).isFalse();
        }

    }

    @Nested
    class CheckIsRefreshToken {

        @Test
        void ThenReturnsTrue_GivenJwtIsRefreshToken() {
            // Given
            var jwt = Jwt.of(encodedToken, REFRESH_TOKEN, subject, refreshTokenClaims, issued, expiration);

            // When
            var actual = jwt.isRefreshToken();

            // Then
            assertThat(actual).isTrue();
        }

        @Test
        void ThenReturnsFalse_GivenJwtIsAccessToken() {
            // Given
            var jwt = Jwt.of(encodedToken, ACCESS_TOKEN, subject, accessTokenClaims, issued, expiration);

            // When
            var actual = jwt.isRefreshToken();

            // Then
            assertThat(actual).isFalse();
        }

    }

    @Nested
    class GetEncodedTokenValue {

        @Test
        void ThenReturnsEncodedTokenValue_GivenJwtIsValid() {
            // Given
            var jwt = Jwt.of(encodedToken, ACCESS_TOKEN, subject, accessTokenClaims, issued, expiration);

            // When
            var actual = jwt.encodedTokenValue();

            // Then
            assertThat(actual).isEqualTo(encodedToken.value());
        }

    }

    @Nested
    class GetRoleClaim {

        @Test
        void ThenReturnsRole_GivenRoleClaimExists() {
            // Given
            var jwt = Jwt.of(encodedToken, ACCESS_TOKEN, subject, accessTokenClaims, issued, expiration);

            // When
            var actual = jwt.roleClaim();

            // Then
            assertThat(actual).isEqualTo(ADMIN);
        }

        @Test
        void ThenReturnsNull_GivenRoleClaimNotExist() {
            // Given
            var claims = JwtClaims.of(Map.of(TOKEN_USE_CLAIM_NAME, ACCESS_TOKEN_USE_CLAIM_VALUE));
            var jwt = Jwt.of(encodedToken, ACCESS_TOKEN, subject, claims, issued, expiration);

            // When
            var actual = jwt.roleClaim();

            // Then
            assertThat(actual).isNull();
        }

    }

}

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

package com.bcn.asapp.authentication.domain.authentication;

import static com.bcn.asapp.authentication.domain.authentication.Jwt.ACCESS_TOKEN_USE_CLAIM_VALUE;
import static com.bcn.asapp.authentication.domain.authentication.Jwt.REFRESH_TOKEN_USE_CLAIM_VALUE;
import static com.bcn.asapp.authentication.domain.authentication.Jwt.ROLE_CLAIM_NAME;
import static com.bcn.asapp.authentication.domain.authentication.Jwt.TOKEN_USE_CLAIM_NAME;
import static com.bcn.asapp.authentication.domain.authentication.JwtType.ACCESS_TOKEN;
import static com.bcn.asapp.authentication.domain.authentication.JwtType.REFRESH_TOKEN;
import static com.bcn.asapp.authentication.domain.user.Role.USER;
import static java.time.temporal.ChronoUnit.DAYS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class JwtTests {

    private final EncodedToken encodedToken = EncodedToken.of("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0In0.c2lnbmF0dXJl");

    private final JwtType accessTokenType = ACCESS_TOKEN;

    private final JwtType refreshTokenType = REFRESH_TOKEN;

    private final Subject subject = Subject.of("user@asapp.com");

    private final JwtClaims accessTokenClaims = JwtClaims.of(Map.of(TOKEN_USE_CLAIM_NAME, ACCESS_TOKEN_USE_CLAIM_VALUE, ROLE_CLAIM_NAME, USER.name()));

    private final JwtClaims refreshTokenClaims = JwtClaims.of(Map.of(TOKEN_USE_CLAIM_NAME, REFRESH_TOKEN_USE_CLAIM_VALUE, ROLE_CLAIM_NAME, USER.name()));

    private final Issued issued = Issued.of(Instant.parse("2025-01-01T10:00:00Z"));

    private final Expiration expiration = Expiration.of(issued, 30000L);

    @Nested
    class CreateJwtWithConstructor {

        @Test
        void ThenThrowsIllegalArgumentException_GivenEncodedTokenIsNull() {
            // When
            var thrown = catchThrowable(() -> new Jwt(null, accessTokenType, subject, accessTokenClaims, issued, expiration));

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
            var thrown = catchThrowable(() -> new Jwt(encodedToken, accessTokenType, null, accessTokenClaims, issued, expiration));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Subject must not be null");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenClaimsIsNull() {
            // When
            var thrown = catchThrowable(() -> new Jwt(encodedToken, accessTokenType, subject, null, issued, expiration));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Claims must not be null");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenClaimsMissingTokenUseClaim() {
            // Given
            var invalidClaims = JwtClaims.of(Map.of(ROLE_CLAIM_NAME, USER.name()));

            // When
            var thrown = catchThrowable(() -> new Jwt(encodedToken, accessTokenType, subject, invalidClaims, issued, expiration));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Claims must contain the mandatory token use claim");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenClaimsHasInvalidTokenUseClaim() {
            // Given
            var invalidClaims = JwtClaims.of(Map.of(TOKEN_USE_CLAIM_NAME, "invalid_token_claim", ROLE_CLAIM_NAME, USER.name()));

            // When
            var thrown = catchThrowable(() -> new Jwt(encodedToken, accessTokenType, subject, invalidClaims, issued, expiration));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessageContaining("Invalid JWT use claim")
                              .hasMessageContaining("access")
                              .hasMessageContaining("refresh")
                              .hasMessageContaining("invalid_token_claim");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenAccessTokenTypeWithRefreshTokenUseClaim() {
            // When
            var thrown = catchThrowable(() -> new Jwt(encodedToken, accessTokenType, subject, refreshTokenClaims, issued, expiration));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessageContaining("Token type")
                              .hasMessageContaining("token_use claim")
                              .hasMessageContaining("do not match");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenRefreshTokenTypeWithAccessTokenUseClaim() {
            // When
            var thrown = catchThrowable(() -> new Jwt(encodedToken, refreshTokenType, subject, accessTokenClaims, issued, expiration));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessageContaining("Token type")
                              .hasMessageContaining("token_use claim")
                              .hasMessageContaining("do not match");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenExpirationBeforeIssued() {
            // Given
            var expirationValue = issued.value()
                                        .minus(1, DAYS);
            var invalidExpiration = new Expiration(expirationValue);

            // When
            var thrown = catchThrowable(() -> new Jwt(encodedToken, accessTokenType, subject, accessTokenClaims, issued, invalidExpiration));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Issued date must be before expiration date");
        }

        @Test
        void ThenReturnsJwt_GivenParametersAreValidOnAccessToken() {
            // When
            var actual = new Jwt(encodedToken, accessTokenType, subject, accessTokenClaims, issued, expiration);

            // Then
            assertThat(actual).isNotNull();
            assertThat(actual.encodedToken()).isEqualTo(encodedToken);
            assertThat(actual.type()).isEqualTo(accessTokenType);
            assertThat(actual.subject()).isEqualTo(subject);
            assertThat(actual.claims()).isEqualTo(accessTokenClaims);
            assertThat(actual.issued()).isEqualTo(issued);
            assertThat(actual.expiration()).isEqualTo(expiration);
        }

        @Test
        void ThenReturnsJwt_GivenParametersAreValidOnRefreshToken() {
            // When
            var actual = new Jwt(encodedToken, refreshTokenType, subject, refreshTokenClaims, issued, expiration);

            // Then
            assertThat(actual).isNotNull();
            assertThat(actual.encodedToken()).isEqualTo(encodedToken);
            assertThat(actual.type()).isEqualTo(refreshTokenType);
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
            var thrown = catchThrowable(() -> Jwt.of(null, accessTokenType, subject, accessTokenClaims, issued, expiration));

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
            var thrown = catchThrowable(() -> Jwt.of(encodedToken, accessTokenType, null, accessTokenClaims, issued, expiration));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Subject must not be null");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenClaimsIsNull() {
            // When
            var thrown = catchThrowable(() -> Jwt.of(encodedToken, accessTokenType, subject, null, issued, expiration));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Claims must not be null");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenClaimsMissingTokenUseClaim() {
            // Given
            var invalidClaims = JwtClaims.of(Map.of(ROLE_CLAIM_NAME, USER.name()));

            // When
            var thrown = catchThrowable(() -> Jwt.of(encodedToken, accessTokenType, subject, invalidClaims, issued, expiration));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Claims must contain the mandatory token use claim");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenClaimsHasInvalidTokenUseClaim() {
            // Given
            var invalidClaims = JwtClaims.of(Map.of(TOKEN_USE_CLAIM_NAME, "invalid_token_claim", ROLE_CLAIM_NAME, USER.name()));

            // When
            var thrown = catchThrowable(() -> Jwt.of(encodedToken, accessTokenType, subject, invalidClaims, issued, expiration));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessageContaining("Invalid JWT use claim")
                              .hasMessageContaining("access")
                              .hasMessageContaining("refresh")
                              .hasMessageContaining("invalid_token_claim");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenAccessTokenTypeWithRefreshTokenUseClaim() {
            // When
            var thrown = catchThrowable(() -> Jwt.of(encodedToken, accessTokenType, subject, refreshTokenClaims, issued, expiration));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessageContaining("Token type")
                              .hasMessageContaining("token_use claim")
                              .hasMessageContaining("do not match");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenRefreshTokenTypeWithAccessTokenUseClaim() {
            // When
            var thrown = catchThrowable(() -> Jwt.of(encodedToken, refreshTokenType, subject, accessTokenClaims, issued, expiration));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessageContaining("Token type")
                              .hasMessageContaining("token_use claim")
                              .hasMessageContaining("do not match");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenExpirationBeforeIssued() {
            // Given
            var expirationValue = issued.value()
                                        .minus(1, DAYS);
            var invalidExpiration = new Expiration(expirationValue);

            // When
            var thrown = catchThrowable(() -> Jwt.of(encodedToken, accessTokenType, subject, accessTokenClaims, issued, invalidExpiration));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Issued date must be before expiration date");
        }

        @Test
        void ThenReturnsJwt_GivenParametersAreValidOnAccessToken() {
            // When
            var actual = Jwt.of(encodedToken, accessTokenType, subject, accessTokenClaims, issued, expiration);

            // Then
            assertThat(actual).isNotNull();
            assertThat(actual.encodedToken()).isEqualTo(encodedToken);
            assertThat(actual.type()).isEqualTo(accessTokenType);
            assertThat(actual.subject()).isEqualTo(subject);
            assertThat(actual.claims()).isEqualTo(accessTokenClaims);
            assertThat(actual.issued()).isEqualTo(issued);
            assertThat(actual.expiration()).isEqualTo(expiration);
        }

        @Test
        void ThenReturnsJwt_GivenParametersAreValidOnRefreshToken() {
            // When
            var actual = Jwt.of(encodedToken, refreshTokenType, subject, refreshTokenClaims, issued, expiration);

            // Then
            assertThat(actual).isNotNull();
            assertThat(actual.encodedToken()).isEqualTo(encodedToken);
            assertThat(actual.type()).isEqualTo(refreshTokenType);
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
            var jwt = Jwt.of(encodedToken, accessTokenType, subject, accessTokenClaims, issued, expiration);

            // When
            var actual = jwt.isAccessToken();

            // Then
            assertThat(actual).isTrue();
        }

        @Test
        void ThenReturnsFalse_GivenJwtIsRefreshToken() {
            // Given
            var jwt = Jwt.of(encodedToken, refreshTokenType, subject, refreshTokenClaims, issued, expiration);

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
            var jwt = Jwt.of(encodedToken, refreshTokenType, subject, refreshTokenClaims, issued, expiration);

            // When
            var actual = jwt.isRefreshToken();

            // Then
            assertThat(actual).isTrue();
        }

        @Test
        void ThenReturnsFalse_GivenJwtIsAccessToken() {
            // Given
            var jwt = Jwt.of(encodedToken, accessTokenType, subject, accessTokenClaims, issued, expiration);

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
            var jwt = Jwt.of(encodedToken, accessTokenType, subject, accessTokenClaims, issued, expiration);

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
            var jwt = Jwt.of(encodedToken, accessTokenType, subject, accessTokenClaims, issued, expiration);

            // When
            var actual = jwt.roleClaim();

            // Then
            assertThat(actual).isEqualTo(USER);
        }

        @Test
        void ThenReturnsNull_GivenRoleClaimNotExist() {
            // Given
            var claims = JwtClaims.of(Map.of(TOKEN_USE_CLAIM_NAME, ACCESS_TOKEN_USE_CLAIM_VALUE));
            var jwt = Jwt.of(encodedToken, accessTokenType, subject, claims, issued, expiration);

            // When
            var actual = jwt.roleClaim();

            // Then
            assertThat(actual).isNull();
        }

    }

}

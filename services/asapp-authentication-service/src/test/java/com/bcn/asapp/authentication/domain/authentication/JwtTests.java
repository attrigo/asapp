/**
* Copyright 2023 the original author or authors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     https://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.bcn.asapp.authentication.domain.authentication;

import static com.bcn.asapp.authentication.domain.authentication.JwtClaimNames.ACCESS_TOKEN_USE;
import static com.bcn.asapp.authentication.domain.authentication.JwtClaimNames.REFRESH_TOKEN_USE;
import static com.bcn.asapp.authentication.domain.authentication.JwtClaimNames.ROLE;
import static com.bcn.asapp.authentication.domain.authentication.JwtClaimNames.TOKEN_USE;
import static com.bcn.asapp.authentication.domain.authentication.JwtType.ACCESS_TOKEN;
import static com.bcn.asapp.authentication.domain.authentication.JwtType.REFRESH_TOKEN;
import static com.bcn.asapp.authentication.domain.user.Role.USER;
import static com.bcn.asapp.authentication.testutil.fixture.JwtFactory.aJwtBuilder;
import static com.bcn.asapp.authentication.testutil.fixture.JwtFactory.aRefreshToken;
import static com.bcn.asapp.authentication.testutil.fixture.JwtFactory.anAccessToken;
import static java.time.temporal.ChronoUnit.DAYS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link Jwt} claim consistency, type matching, and temporal validation.
 * <p>
 * Coverage:
 * <li>Rejects null encoded token, type, subject, or claims</li>
 * <li>Validates claims contain mandatory token_use claim</li>
 * <li>Validates token_use claim value is "access" or "refresh"</li>
 * <li>Validates token type matches token_use claim value</li>
 * <li>Validates issued timestamp is before expiration timestamp</li>
 * <li>Creates valid access and refresh tokens with all components</li>
 * <li>Provides type checking methods (isAccessToken, isRefreshToken)</li>
 * <li>Extracts role claim from token claims</li>
 * <li>Provides access to encoded token value</li>
 */
class JwtTests {

    @Nested
    class CreateJwtWithConstructor {

        @Test
        void ReturnsJwt_ValidParametersOnAccessToken() {
            // Given
            var encodedToken = EncodedToken.of("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0In0.c2lnbmF0dXJl");
            var subject = Subject.of("user@asapp.com");
            var accessTokenClaims = JwtClaims.of(Map.of(TOKEN_USE, ACCESS_TOKEN_USE, ROLE, USER.name()));
            var issued = Issued.of(Instant.parse("2025-01-01T10:00:00Z"));
            var expiration = Expiration.of(issued, 30000L);

            // When
            var actual = new Jwt(encodedToken, ACCESS_TOKEN, subject, accessTokenClaims, issued, expiration);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual).as("JWT").isNotNull();
                softly.assertThat(actual.encodedToken()).as("encoded token").isEqualTo(encodedToken);
                softly.assertThat(actual.type()).as("type").isEqualTo(ACCESS_TOKEN);
                softly.assertThat(actual.subject()).as("subject").isEqualTo(subject);
                softly.assertThat(actual.claims()).as("claims").isEqualTo(accessTokenClaims);
                softly.assertThat(actual.issued()).as("issued").isEqualTo(issued);
                softly.assertThat(actual.expiration()).as("expiration").isEqualTo(expiration);
                // @formatter:on
            });
        }

        @Test
        void ReturnsJwt_ValidParametersOnRefreshToken() {
            // Given
            var encodedToken = EncodedToken.of("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0In0.c2lnbmF0dXJl");
            var subject = Subject.of("user@asapp.com");
            var refreshTokenClaims = JwtClaims.of(Map.of(TOKEN_USE, REFRESH_TOKEN_USE, ROLE, USER.name()));
            var issued = Issued.of(Instant.parse("2025-01-01T10:00:00Z"));
            var expiration = Expiration.of(issued, 30000L);

            // When
            var actual = new Jwt(encodedToken, REFRESH_TOKEN, subject, refreshTokenClaims, issued, expiration);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual).as("JWT").isNotNull();
                softly.assertThat(actual.encodedToken()).as("encoded token").isEqualTo(encodedToken);
                softly.assertThat(actual.type()).as("type").isEqualTo(REFRESH_TOKEN);
                softly.assertThat(actual.subject()).as("subject").isEqualTo(subject);
                softly.assertThat(actual.claims()).as("claims").isEqualTo(refreshTokenClaims);
                softly.assertThat(actual.issued()).as("issued").isEqualTo(issued);
                softly.assertThat(actual.expiration()).as("expiration").isEqualTo(expiration);
                // @formatter:on
            });
        }

        @Test
        void ThrowsIllegalArgumentException_NullEncodedToken() {
            // Given
            var subject = Subject.of("user@asapp.com");
            var accessTokenClaims = JwtClaims.of(Map.of(TOKEN_USE, ACCESS_TOKEN_USE, ROLE, USER.name()));
            var issued = Issued.of(Instant.parse("2025-01-01T10:00:00Z"));
            var expiration = Expiration.of(issued, 30000L);

            // When
            var actual = catchThrowable(() -> new Jwt(null, ACCESS_TOKEN, subject, accessTokenClaims, issued, expiration));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Encoded token must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullType() {
            // Given
            var encodedToken = EncodedToken.of("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0In0.c2lnbmF0dXJl");
            var subject = Subject.of("user@asapp.com");
            var accessTokenClaims = JwtClaims.of(Map.of(TOKEN_USE, ACCESS_TOKEN_USE, ROLE, USER.name()));
            var issued = Issued.of(Instant.parse("2025-01-01T10:00:00Z"));
            var expiration = Expiration.of(issued, 30000L);

            // When
            var actual = catchThrowable(() -> new Jwt(encodedToken, null, subject, accessTokenClaims, issued, expiration));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Type must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullSubject() {
            // Given
            var encodedToken = EncodedToken.of("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0In0.c2lnbmF0dXJl");
            var accessTokenClaims = JwtClaims.of(Map.of(TOKEN_USE, ACCESS_TOKEN_USE, ROLE, USER.name()));
            var issued = Issued.of(Instant.parse("2025-01-01T10:00:00Z"));
            var expiration = Expiration.of(issued, 30000L);

            // When
            var actual = catchThrowable(() -> new Jwt(encodedToken, ACCESS_TOKEN, null, accessTokenClaims, issued, expiration));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Subject must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullClaims() {
            // Given
            var encodedToken = EncodedToken.of("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0In0.c2lnbmF0dXJl");
            var subject = Subject.of("user@asapp.com");
            var issued = Issued.of(Instant.parse("2025-01-01T10:00:00Z"));
            var expiration = Expiration.of(issued, 30000L);

            // When
            var actual = catchThrowable(() -> new Jwt(encodedToken, ACCESS_TOKEN, subject, null, issued, expiration));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Claims must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_MissingTokenUseClaim() {
            // Given
            var encodedToken = EncodedToken.of("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0In0.c2lnbmF0dXJl");
            var subject = Subject.of("user@asapp.com");
            var issued = Issued.of(Instant.parse("2025-01-01T10:00:00Z"));
            var expiration = Expiration.of(issued, 30000L);
            var invalidClaims = JwtClaims.of(Map.of(ROLE, USER.name()));

            // When
            var actual = catchThrowable(() -> new Jwt(encodedToken, ACCESS_TOKEN, subject, invalidClaims, issued, expiration));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Claims must contain the mandatory token use claim");
        }

        @Test
        void ThrowsIllegalArgumentException_InvalidTokenUseClaim() {
            // Given
            var encodedToken = EncodedToken.of("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0In0.c2lnbmF0dXJl");
            var subject = Subject.of("user@asapp.com");
            var issued = Issued.of(Instant.parse("2025-01-01T10:00:00Z"));
            var expiration = Expiration.of(issued, 30000L);
            var invalidClaims = JwtClaims.of(Map.of(TOKEN_USE, "invalid_token_claim", ROLE, USER.name()));

            // When
            var actual = catchThrowable(() -> new Jwt(encodedToken, ACCESS_TOKEN, subject, invalidClaims, issued, expiration));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessageContaining("Invalid JWT use claim")
                              .hasMessageContaining("access")
                              .hasMessageContaining("refresh")
                              .hasMessageContaining("invalid_token_claim");
        }

        @Test
        void ThrowsIllegalArgumentException_AccessTokenTypeWithRefreshTokenUseClaim() {
            // Given
            var encodedToken = EncodedToken.of("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0In0.c2lnbmF0dXJl");
            var subject = Subject.of("user@asapp.com");
            var refreshTokenClaims = JwtClaims.of(Map.of(TOKEN_USE, REFRESH_TOKEN_USE, ROLE, USER.name()));
            var issued = Issued.of(Instant.parse("2025-01-01T10:00:00Z"));
            var expiration = Expiration.of(issued, 30000L);

            // When
            var actual = catchThrowable(() -> new Jwt(encodedToken, ACCESS_TOKEN, subject, refreshTokenClaims, issued, expiration));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessageContaining("Token type")
                              .hasMessageContaining("token_use claim")
                              .hasMessageContaining("do not match");
        }

        @Test
        void ThrowsIllegalArgumentException_RefreshTokenTypeWithAccessTokenUseClaim() {
            // Given
            var encodedToken = EncodedToken.of("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0In0.c2lnbmF0dXJl");
            var subject = Subject.of("user@asapp.com");
            var accessTokenClaims = JwtClaims.of(Map.of(TOKEN_USE, ACCESS_TOKEN_USE, ROLE, USER.name()));
            var issued = Issued.of(Instant.parse("2025-01-01T10:00:00Z"));
            var expiration = Expiration.of(issued, 30000L);

            // When
            var actual = catchThrowable(() -> new Jwt(encodedToken, REFRESH_TOKEN, subject, accessTokenClaims, issued, expiration));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessageContaining("Token type")
                              .hasMessageContaining("token_use claim")
                              .hasMessageContaining("do not match");
        }

        @Test
        void ThrowsIllegalArgumentException_ExpirationBeforeIssued() {
            // Given
            var encodedToken = EncodedToken.of("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0In0.c2lnbmF0dXJl");
            var subject = Subject.of("user@asapp.com");
            var accessTokenClaims = JwtClaims.of(Map.of(TOKEN_USE, ACCESS_TOKEN_USE, ROLE, USER.name()));
            var issued = Issued.of(Instant.parse("2025-01-01T10:00:00Z"));
            var expiration = issued.value()
                                   .minus(1, DAYS);
            var invalidExpiration = new Expiration(expiration);

            // When
            var actual = catchThrowable(() -> new Jwt(encodedToken, ACCESS_TOKEN, subject, accessTokenClaims, issued, invalidExpiration));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Issued date must be before expiration date");
        }

    }

    @Nested
    class CreateJwtWithFactoryMethod {

        @Test
        void ReturnsJwt_ValidParametersOnAccessToken() {
            // Given
            var encodedToken = EncodedToken.of("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0In0.c2lnbmF0dXJl");
            var subject = Subject.of("user@asapp.com");
            var accessTokenClaims = JwtClaims.of(Map.of(TOKEN_USE, ACCESS_TOKEN_USE, ROLE, USER.name()));
            var issued = Issued.of(Instant.parse("2025-01-01T10:00:00Z"));
            var expiration = Expiration.of(issued, 30000L);

            // When
            var actual = Jwt.of(encodedToken, ACCESS_TOKEN, subject, accessTokenClaims, issued, expiration);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual).as("JWT").isNotNull();
                softly.assertThat(actual.encodedToken()).as("encoded token").isEqualTo(encodedToken);
                softly.assertThat(actual.type()).as("type").isEqualTo(ACCESS_TOKEN);
                softly.assertThat(actual.subject()).as("subject").isEqualTo(subject);
                softly.assertThat(actual.claims()).as("claims").isEqualTo(accessTokenClaims);
                softly.assertThat(actual.issued()).as("issued").isEqualTo(issued);
                softly.assertThat(actual.expiration()).as("expiration").isEqualTo(expiration);
                // @formatter:on
            });
        }

        @Test
        void ReturnsJwt_ValidParametersOnRefreshToken() {
            // Given
            var encodedToken = EncodedToken.of("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0In0.c2lnbmF0dXJl");
            var subject = Subject.of("user@asapp.com");
            var refreshTokenClaims = JwtClaims.of(Map.of(TOKEN_USE, REFRESH_TOKEN_USE, ROLE, USER.name()));
            var issued = Issued.of(Instant.parse("2025-01-01T10:00:00Z"));
            var expiration = Expiration.of(issued, 30000L);

            // When
            var actual = Jwt.of(encodedToken, REFRESH_TOKEN, subject, refreshTokenClaims, issued, expiration);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual).as("JWT").isNotNull();
                softly.assertThat(actual.encodedToken()).as("encoded token").isEqualTo(encodedToken);
                softly.assertThat(actual.type()).as("type").isEqualTo(REFRESH_TOKEN);
                softly.assertThat(actual.subject()).as("subject").isEqualTo(subject);
                softly.assertThat(actual.claims()).as("claims").isEqualTo(refreshTokenClaims);
                softly.assertThat(actual.issued()).as("issued").isEqualTo(issued);
                softly.assertThat(actual.expiration()).as("expiration").isEqualTo(expiration);
                // @formatter:on
            });
        }

        @Test
        void ThrowsIllegalArgumentException_NullEncodedToken() {
            // Given
            var subject = Subject.of("user@asapp.com");
            var accessTokenClaims = JwtClaims.of(Map.of(TOKEN_USE, ACCESS_TOKEN_USE, ROLE, USER.name()));
            var issued = Issued.of(Instant.parse("2025-01-01T10:00:00Z"));
            var expiration = Expiration.of(issued, 30000L);

            // When
            var actual = catchThrowable(() -> Jwt.of(null, ACCESS_TOKEN, subject, accessTokenClaims, issued, expiration));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Encoded token must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullType() {
            // Given
            var encodedToken = EncodedToken.of("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0In0.c2lnbmF0dXJl");
            var subject = Subject.of("user@asapp.com");
            var accessTokenClaims = JwtClaims.of(Map.of(TOKEN_USE, ACCESS_TOKEN_USE, ROLE, USER.name()));
            var issued = Issued.of(Instant.parse("2025-01-01T10:00:00Z"));
            var expiration = Expiration.of(issued, 30000L);

            // When
            var actual = catchThrowable(() -> Jwt.of(encodedToken, null, subject, accessTokenClaims, issued, expiration));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Type must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullSubject() {
            // Given
            var encodedToken = EncodedToken.of("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0In0.c2lnbmF0dXJl");
            var accessTokenClaims = JwtClaims.of(Map.of(TOKEN_USE, ACCESS_TOKEN_USE, ROLE, USER.name()));
            var issued = Issued.of(Instant.parse("2025-01-01T10:00:00Z"));
            var expiration = Expiration.of(issued, 30000L);

            // When
            var actual = catchThrowable(() -> Jwt.of(encodedToken, ACCESS_TOKEN, null, accessTokenClaims, issued, expiration));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Subject must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullClaims() {
            // Given
            var encodedToken = EncodedToken.of("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0In0.c2lnbmF0dXJl");
            var subject = Subject.of("user@asapp.com");
            var issued = Issued.of(Instant.parse("2025-01-01T10:00:00Z"));
            var expiration = Expiration.of(issued, 30000L);

            // When
            var actual = catchThrowable(() -> Jwt.of(encodedToken, ACCESS_TOKEN, subject, null, issued, expiration));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Claims must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_MissingTokenUseClaim() {
            // Given
            var encodedToken = EncodedToken.of("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0In0.c2lnbmF0dXJl");
            var subject = Subject.of("user@asapp.com");
            var issued = Issued.of(Instant.parse("2025-01-01T10:00:00Z"));
            var expiration = Expiration.of(issued, 30000L);
            var invalidClaims = JwtClaims.of(Map.of(ROLE, USER.name()));

            // When
            var actual = catchThrowable(() -> Jwt.of(encodedToken, ACCESS_TOKEN, subject, invalidClaims, issued, expiration));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Claims must contain the mandatory token use claim");
        }

        @Test
        void ThrowsIllegalArgumentException_InvalidTokenUseClaim() {
            // Given
            var encodedToken = EncodedToken.of("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0In0.c2lnbmF0dXJl");
            var subject = Subject.of("user@asapp.com");
            var issued = Issued.of(Instant.parse("2025-01-01T10:00:00Z"));
            var expiration = Expiration.of(issued, 30000L);
            var invalidClaims = JwtClaims.of(Map.of(TOKEN_USE, "invalid_token_claim", ROLE, USER.name()));

            // When
            var actual = catchThrowable(() -> Jwt.of(encodedToken, ACCESS_TOKEN, subject, invalidClaims, issued, expiration));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessageContaining("Invalid JWT use claim")
                              .hasMessageContaining("access")
                              .hasMessageContaining("refresh")
                              .hasMessageContaining("invalid_token_claim");
        }

        @Test
        void ThrowsIllegalArgumentException_AccessTokenTypeWithRefreshTokenUseClaim() {
            // Given
            var encodedToken = EncodedToken.of("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0In0.c2lnbmF0dXJl");
            var subject = Subject.of("user@asapp.com");
            var refreshTokenClaims = JwtClaims.of(Map.of(TOKEN_USE, REFRESH_TOKEN_USE, ROLE, USER.name()));
            var issued = Issued.of(Instant.parse("2025-01-01T10:00:00Z"));
            var expiration = Expiration.of(issued, 30000L);

            // When
            var actual = catchThrowable(() -> Jwt.of(encodedToken, ACCESS_TOKEN, subject, refreshTokenClaims, issued, expiration));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessageContaining("Token type")
                              .hasMessageContaining("token_use claim")
                              .hasMessageContaining("do not match");
        }

        @Test
        void ThrowsIllegalArgumentException_RefreshTokenTypeWithAccessTokenUseClaim() {
            // Given
            var encodedToken = EncodedToken.of("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0In0.c2lnbmF0dXJl");
            var subject = Subject.of("user@asapp.com");
            var accessTokenClaims = JwtClaims.of(Map.of(TOKEN_USE, ACCESS_TOKEN_USE, ROLE, USER.name()));
            var issued = Issued.of(Instant.parse("2025-01-01T10:00:00Z"));
            var expiration = Expiration.of(issued, 30000L);

            // When
            var actual = catchThrowable(() -> Jwt.of(encodedToken, REFRESH_TOKEN, subject, accessTokenClaims, issued, expiration));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessageContaining("Token type")
                              .hasMessageContaining("token_use claim")
                              .hasMessageContaining("do not match");
        }

        @Test
        void ThrowsIllegalArgumentException_ExpirationBeforeIssued() {
            // Given
            var encodedToken = EncodedToken.of("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0In0.c2lnbmF0dXJl");
            var subject = Subject.of("user@asapp.com");
            var accessTokenClaims = JwtClaims.of(Map.of(TOKEN_USE, ACCESS_TOKEN_USE, ROLE, USER.name()));
            var issued = Issued.of(Instant.parse("2025-01-01T10:00:00Z"));
            var expiration = issued.value()
                                   .minus(1, DAYS);
            var invalidExpiration = new Expiration(expiration);

            // When
            var actual = catchThrowable(() -> Jwt.of(encodedToken, ACCESS_TOKEN, subject, accessTokenClaims, issued, invalidExpiration));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Issued date must be before expiration date");
        }

    }

    @Nested
    class CheckIsAccessToken {

        @Test
        void ReturnsTrue_AccessTokenJwt() {
            // Given
            var jwt = anAccessToken();

            // When
            var actual = jwt.isAccessToken();

            // Then
            assertThat(actual).isTrue();
        }

        @Test
        void ReturnsFalse_RefreshTokenJwt() {
            // Given
            var jwt = aRefreshToken();

            // When
            var actual = jwt.isAccessToken();

            // Then
            assertThat(actual).isFalse();
        }

    }

    @Nested
    class CheckIsRefreshToken {

        @Test
        void ReturnsTrue_RefreshTokenJwt() {
            // Given
            var jwt = aRefreshToken();

            // When
            var actual = jwt.isRefreshToken();

            // Then
            assertThat(actual).isTrue();
        }

        @Test
        void ReturnsFalse_AccessTokenJwt() {
            // Given
            var jwt = anAccessToken();

            // When
            var actual = jwt.isRefreshToken();

            // Then
            assertThat(actual).isFalse();
        }

    }

    @Nested
    class GetEncodedTokenValue {

        @Test
        void ReturnsEncodedTokenValue_ValidJwt() {
            // Given
            var jwt = anAccessToken();

            // When
            var actual = jwt.encodedTokenValue();

            // Then
            assertThat(actual).isEqualTo(jwt.encodedToken()
                                            .value());
        }

    }

    @Nested
    class GetRoleClaim {

        @Test
        void ReturnsRole_RoleClaimExists() {
            // Given
            var jwt = anAccessToken();

            // When
            var actual = jwt.roleClaim();

            // Then
            assertThat(actual).isEqualTo(USER);
        }

        @Test
        void ReturnsNull_RoleClaimNotExists() {
            // Given
            var jwt = aJwtBuilder().accessToken()
                                   .withClaims(Map.of(TOKEN_USE, ACCESS_TOKEN_USE))
                                   .build();

            // When
            var actual = jwt.roleClaim();

            // Then
            assertThat(actual).isNull();
        }

    }

}

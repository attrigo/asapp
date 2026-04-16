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

package com.bcn.asapp.authentication.infrastructure.security;

import static com.bcn.asapp.authentication.domain.authentication.JwtClaimNames.ACCESS_TOKEN_USE;
import static com.bcn.asapp.authentication.domain.authentication.JwtClaimNames.REFRESH_TOKEN_USE;
import static com.bcn.asapp.authentication.domain.authentication.JwtClaimNames.ROLE;
import static com.bcn.asapp.authentication.domain.authentication.JwtClaimNames.TOKEN_USE;
import static com.bcn.asapp.authentication.domain.authentication.JwtTypeNames.ACCESS_TOKEN_TYPE;
import static com.bcn.asapp.authentication.domain.authentication.JwtTypeNames.REFRESH_TOKEN_TYPE;
import static com.bcn.asapp.authentication.testutil.fixture.EncodedTokenMother.encodedAccessToken;
import static com.bcn.asapp.authentication.testutil.fixture.EncodedTokenMother.encodedRefreshToken;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

/**
 * Tests {@link DecodedJwt} token metadata access, type identification, and role extraction.
 * <p>
 * Coverage:
 * <li>Validates encoded token, type, subject, and claims required at construction</li>
 * <li>Determines access token identity by matching type and token_use claim</li>
 * <li>Extracts role claim with null handling for missing or invalid values</li>
 * <li>Provides immutable access to all token metadata</li>
 */
class DecodedJwtTests {

    @Nested
    class CreateDecodedJwt {

        @Test
        void ReturnsDecodedJwt_ValidParameters() {
            // Given
            var encodedAccessToken = encodedAccessToken();
            var subject = "user@asapp.com";
            var claims = Map.<String, Object>of(TOKEN_USE, ACCESS_TOKEN_USE, ROLE, "USER");

            // When
            var actual = new DecodedJwt(encodedAccessToken, ACCESS_TOKEN_TYPE, subject, claims);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual).as("decoded JWT").isNotNull();
                softly.assertThat(actual.encodedToken()).as("encoded token").isEqualTo(encodedAccessToken);
                softly.assertThat(actual.type()).as("type").isEqualTo(ACCESS_TOKEN_TYPE);
                softly.assertThat(actual.subject()).as("subject").isEqualTo(subject);
                softly.assertThat(actual.claims()).as("claims").isEqualTo(claims);
                // @formatter:on
            });
        }

        @ParameterizedTest
        @NullAndEmptySource
        void ThrowsIllegalArgumentException_NullOrEmptyEncodedToken(String encodedToken) {
            // Given
            var subject = "user@asapp.com";
            var claims = Map.<String, Object>of(TOKEN_USE, ACCESS_TOKEN_USE, ROLE, "USER");

            // When
            var actual = catchThrowable(() -> new DecodedJwt(encodedToken, ACCESS_TOKEN_TYPE, subject, claims));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessageContaining("Encoded token must not be blank");
        }

        @ParameterizedTest
        @NullAndEmptySource
        void ThrowsIllegalArgumentException_NullOrEmptyType(String type) {
            // Given
            var encodedAccessToken = encodedAccessToken();
            var subject = "user@asapp.com";
            var claims = Map.<String, Object>of(TOKEN_USE, ACCESS_TOKEN_USE, ROLE, "USER");

            // When
            var actual = catchThrowable(() -> new DecodedJwt(encodedAccessToken, type, subject, claims));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessageContaining("Type must not be blank");
        }

        @ParameterizedTest
        @NullAndEmptySource
        void ThrowsIllegalArgumentException_NullOrEmptySubject(String subject) {
            // Given
            var encodedAccessToken = encodedAccessToken();
            var claims = Map.<String, Object>of(TOKEN_USE, ACCESS_TOKEN_USE, ROLE, "USER");

            // When
            var actual = catchThrowable(() -> new DecodedJwt(encodedAccessToken, ACCESS_TOKEN_TYPE, subject, claims));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessageContaining("Subject must not be blank");
        }

        @ParameterizedTest
        @NullAndEmptySource
        void ThrowsIllegalArgumentException_NullOrEmptyClaims(Map<String, Object> claims) {
            // Given
            var encodedAccessToken = encodedAccessToken();
            var subject = "user@asapp.com";

            // When
            var actual = catchThrowable(() -> new DecodedJwt(encodedAccessToken, ACCESS_TOKEN_TYPE, subject, claims));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessageContaining("Claims must not be empty");
        }

    }

    @Nested
    class CheckIsAccessToken {

        @Test
        void ReturnsTrue_AccessTokenTypeAndClaims() {
            // Given
            var encodedAccessToken = encodedAccessToken();
            var subject = "user@asapp.com";
            var claims = Map.<String, Object>of(TOKEN_USE, ACCESS_TOKEN_USE, ROLE, "USER");
            var decodedJwt = new DecodedJwt(encodedAccessToken, ACCESS_TOKEN_TYPE, subject, claims);

            // When
            var actual = decodedJwt.isAccessToken();

            // Then
            assertThat(actual).isTrue();
        }

        @Test
        void ReturnsFalse_TokenTypeNotAccess() {
            // Given
            var encodedAccessToken = encodedAccessToken();
            var subject = "user@asapp.com";
            var claims = Map.<String, Object>of(TOKEN_USE, ACCESS_TOKEN_USE, ROLE, "USER");
            var decodedJwt = new DecodedJwt(encodedAccessToken, REFRESH_TOKEN_TYPE, subject, claims);

            // When
            var actual = decodedJwt.isAccessToken();

            // Then
            assertThat(actual).isFalse();
        }

        @Test
        void ReturnsFalse_TokenUseClaimNotAccess() {
            // Given
            var encodedAccessToken = encodedAccessToken();
            var subject = "user@asapp.com";
            var claims = Map.<String, Object>of(TOKEN_USE, REFRESH_TOKEN_USE, ROLE, "USER");
            var decodedJwt = new DecodedJwt(encodedAccessToken, ACCESS_TOKEN_TYPE, subject, claims);

            // When
            var actual = decodedJwt.isAccessToken();

            // Then
            assertThat(actual).isFalse();
        }

        @Test
        void ReturnsFalse_TokenTypeNotAccessAndTokenUseClaimNotAccess() {
            // Given
            var encodedAccessToken = encodedAccessToken();
            var subject = "user@asapp.com";
            var claims = Map.<String, Object>of(TOKEN_USE, REFRESH_TOKEN_USE, ROLE, "USER");
            var decodedJwt = new DecodedJwt(encodedAccessToken, REFRESH_TOKEN_TYPE, subject, claims);

            // When
            var actual = decodedJwt.isAccessToken();

            // Then
            assertThat(actual).isFalse();
        }

    }

    @Nested
    class CheckIsRefreshToken {

        @Test
        void ReturnsTrue_RefreshTokenTypeAndClaims() {
            // Given
            var encodedRefreshToken = encodedRefreshToken();
            var subject = "user@asapp.com";
            var claims = Map.<String, Object>of(TOKEN_USE, REFRESH_TOKEN_USE, ROLE, "USER");
            var decodedJwt = new DecodedJwt(encodedRefreshToken, REFRESH_TOKEN_TYPE, subject, claims);

            // When
            var actual = decodedJwt.isRefreshToken();

            // Then
            assertThat(actual).isTrue();
        }

        @Test
        void ReturnsFalse_TokenTypeNotRefresh() {
            // Given
            var encodedRefreshToken = encodedRefreshToken();
            var subject = "user@asapp.com";
            var claims = Map.<String, Object>of(TOKEN_USE, ACCESS_TOKEN_USE, ROLE, "USER");
            var decodedJwt = new DecodedJwt(encodedRefreshToken, ACCESS_TOKEN_TYPE, subject, claims);

            // When
            var actual = decodedJwt.isRefreshToken();

            // Then
            assertThat(actual).isFalse();
        }

        @Test
        void ReturnsFalse_TokenUseClaimNotRefresh() {
            // Given
            var encodedRefreshToken = encodedRefreshToken();
            var subject = "user@asapp.com";
            var claims = Map.<String, Object>of(TOKEN_USE, ACCESS_TOKEN_USE, ROLE, "USER");
            var decodedJwt = new DecodedJwt(encodedRefreshToken, REFRESH_TOKEN_TYPE, subject, claims);

            // When
            var actual = decodedJwt.isRefreshToken();

            // Then
            assertThat(actual).isFalse();
        }

        @Test
        void ReturnsFalse_TokenTypeNotRefreshAndTokenUseClaimNotRefresh() {
            // Given
            var encodedRefreshToken = encodedRefreshToken();
            var subject = "user@asapp.com";
            var claims = Map.<String, Object>of(TOKEN_USE, ACCESS_TOKEN_USE, ROLE, "USER");
            var decodedJwt = new DecodedJwt(encodedRefreshToken, ACCESS_TOKEN_TYPE, subject, claims);

            // When
            var actual = decodedJwt.isRefreshToken();

            // Then
            assertThat(actual).isFalse();
        }

    }

    @Nested
    class GetRoleClaim {

        @Test
        void ReturnsRoleClaim_PresentRoleClaim() {
            // Given
            var encodedAccessToken = encodedAccessToken();
            var subject = "user@asapp.com";
            var roleClaim = "USER";
            var claims = Map.<String, Object>of(TOKEN_USE, ACCESS_TOKEN_USE, ROLE, roleClaim);
            var decodedJwt = new DecodedJwt(encodedAccessToken, ACCESS_TOKEN_TYPE, subject, claims);

            // When
            var actual = decodedJwt.roleClaim();

            // Then
            assertThat(actual).isEqualTo(roleClaim);
        }

        @Test
        void ReturnsNull_MissingRoleClaim() {
            // Given
            var encodedAccessToken = encodedAccessToken();
            var subject = "user@asapp.com";
            var claims = Map.<String, Object>of(TOKEN_USE, ACCESS_TOKEN_USE);
            var decodedJwt = new DecodedJwt(encodedAccessToken, ACCESS_TOKEN_TYPE, subject, claims);

            // When
            var actual = decodedJwt.roleClaim();

            // Then
            assertThat(actual).isNull();
        }

        @Test
        void ReturnsNull_NonStringRoleClaim() {
            // Given
            var encodedAccessToken = encodedAccessToken();
            var subject = "user@asapp.com";
            var claims = Map.<String, Object>of(TOKEN_USE, ACCESS_TOKEN_USE, ROLE, 123);
            var decodedJwt = new DecodedJwt(encodedAccessToken, ACCESS_TOKEN_TYPE, subject, claims);

            // When
            var actual = decodedJwt.roleClaim();

            // Then
            assertThat(actual).isNull();
        }

    }

}

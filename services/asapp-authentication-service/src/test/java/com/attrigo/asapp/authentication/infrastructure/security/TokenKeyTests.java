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

package com.attrigo.asapp.authentication.infrastructure.security;

import static com.attrigo.asapp.authentication.domain.authentication.JwtType.ACCESS_TOKEN;
import static com.attrigo.asapp.authentication.domain.authentication.JwtType.REFRESH_TOKEN;
import static com.attrigo.asapp.authentication.infrastructure.security.TokenKey.ACCESS_TOKEN_PREFIX;
import static com.attrigo.asapp.authentication.infrastructure.security.TokenKey.REFRESH_TOKEN_PREFIX;
import static com.attrigo.asapp.authentication.testutil.fixture.JwtMother.aRefreshToken;
import static com.attrigo.asapp.authentication.testutil.fixture.JwtMother.anAccessToken;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.attrigo.asapp.authentication.domain.authentication.EncodedToken;

/**
 * Tests {@link TokenKey} construction, validation and key resolution.
 * <p>
 * Coverage:
 * <li>Validates token type and token required at construction</li>
 * <li>Takes the token type from the JWT it is created from</li>
 * <li>Creates a key for either token type from an encoded token</li>
 * <li>Resolves the namespaced key each token type is stored under</li>
 */
class TokenKeyTests {

    private static final String TOKEN_VALUE = "access.token.value";

    @Nested
    class CreateTokenKeyWithConstructor {

        @Test
        void ReturnsTokenKey_ValidParameters() {
            // Given
            var token = EncodedToken.of(TOKEN_VALUE);

            // When
            var actual = new TokenKey(ACCESS_TOKEN, token);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual).as("token key").isNotNull();
                softly.assertThat(actual.type()).as("token type").isEqualTo(ACCESS_TOKEN);
                softly.assertThat(actual.token()).as("token").isEqualTo(token);
                // @formatter:on
            });
        }

        @Test
        void ThrowsIllegalArgumentException_NullType() {
            // When
            var actual = catchThrowable(() -> new TokenKey(null, EncodedToken.of(TOKEN_VALUE)));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Token type must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullToken() {
            // When
            var actual = catchThrowable(() -> new TokenKey(ACCESS_TOKEN, null));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Token must not be null");
        }

    }

    @Nested
    class CreateTokenKeyWithFactoryMethod {

        @Test
        void ReturnsTokenKey_JwtAccessType() {
            // Given
            var jwt = anAccessToken();

            // When
            var actual = TokenKey.of(jwt);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual).as("token key").isNotNull();
                softly.assertThat(actual.type()).as("token type").isEqualTo(ACCESS_TOKEN);
                softly.assertThat(actual.token()).as("token").isEqualTo(jwt.encodedToken());
                // @formatter:on
            });
        }

        @Test
        void ReturnsTokenKey_JwtRefreshType() {
            // Given
            var jwt = aRefreshToken();

            // When
            var actual = TokenKey.of(jwt);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual).as("token key").isNotNull();
                softly.assertThat(actual.type()).as("token type").isEqualTo(REFRESH_TOKEN);
                softly.assertThat(actual.token()).as("token").isEqualTo(jwt.encodedToken());
                // @formatter:on
            });
        }

        @Test
        void ThrowsIllegalArgumentException_NullJwt() {
            // When
            var actual = catchThrowable(() -> TokenKey.of(null));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("JWT must not be null");
        }

    }

    @Nested
    class CreateAccessTokenKey {

        @Test
        void ReturnsTokenKey_ValidToken() {
            // Given
            var token = EncodedToken.of(TOKEN_VALUE);

            // When
            var actual = TokenKey.ofAccessToken(token);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual).as("token key").isNotNull();
                softly.assertThat(actual.type()).as("token type").isEqualTo(ACCESS_TOKEN);
                softly.assertThat(actual.token()).as("token").isEqualTo(token);
                // @formatter:on
            });
        }

    }

    @Nested
    class CreateRefreshTokenKey {

        @Test
        void ReturnsTokenKey_ValidToken() {
            // Given
            var token = EncodedToken.of(TOKEN_VALUE);

            // When
            var actual = TokenKey.ofRefreshToken(token);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual).as("token key").isNotNull();
                softly.assertThat(actual.type()).as("token type").isEqualTo(REFRESH_TOKEN);
                softly.assertThat(actual.token()).as("token").isEqualTo(token);
                // @formatter:on
            });
        }

    }

    @Nested
    class GetValue {

        @Test
        void ReturnsAccessTokenPrefixedKeyValue_TokenAccessType() {
            // Given
            var tokenKey = new TokenKey(ACCESS_TOKEN, EncodedToken.of(TOKEN_VALUE));

            // When
            var actual = tokenKey.value();

            // Then
            assertThat(actual).isEqualTo(ACCESS_TOKEN_PREFIX + TOKEN_VALUE);
        }

        @Test
        void ReturnsRefreshTokenPrefixedKeyValue_TokenRefreshType() {
            // Given
            var tokenKey = new TokenKey(REFRESH_TOKEN, EncodedToken.of(TOKEN_VALUE));

            // When
            var actual = tokenKey.value();

            // Then
            assertThat(actual).isEqualTo(REFRESH_TOKEN_PREFIX + TOKEN_VALUE);
        }

    }

}

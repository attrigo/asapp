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
import static com.bcn.asapp.authentication.domain.authentication.Jwt.TOKEN_USE_CLAIM_NAME;
import static com.bcn.asapp.authentication.domain.authentication.JwtType.ACCESS_TOKEN;
import static com.bcn.asapp.authentication.domain.authentication.JwtType.REFRESH_TOKEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.bcn.asapp.authentication.domain.user.UserId;

class JwtAuthenticationTests {

    private JwtAuthenticationId id;

    private UserId userId;

    private Jwt accessToken;

    private Jwt refreshToken;

    @BeforeEach
    void beforeEach() {
        var token = EncodedToken.of("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.encoded");
        var subject = Subject.of("user");
        var accessTokenClaims = JwtClaims.of(Map.of(TOKEN_USE_CLAIM_NAME, ACCESS_TOKEN_USE_CLAIM_VALUE));
        var refreshTokenClaims = JwtClaims.of(Map.of(TOKEN_USE_CLAIM_NAME, REFRESH_TOKEN_USE_CLAIM_VALUE));
        var issued = Issued.now();
        var expiration = Expiration.of(issued, 1000L);

        id = JwtAuthenticationId.of(UUID.randomUUID());
        userId = UserId.of(UUID.randomUUID());
        accessToken = Jwt.of(token, ACCESS_TOKEN, subject, accessTokenClaims, issued, expiration);
        refreshToken = Jwt.of(token, REFRESH_TOKEN, subject, refreshTokenClaims, issued, expiration);
    }

    @Nested
    class CreateJwtUnAuthenticated {

        @Test
        void ThenThrowsIllegalArgumentException_GivenUserIdIsNull() {
            // When
            var thrown = catchThrowable(() -> JwtAuthentication.unAuthenticated(null, accessToken, refreshToken));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("User ID must not be null");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenAccessTokenIsNull() {
            // When
            var thrown = catchThrowable(() -> JwtAuthentication.unAuthenticated(userId, null, refreshToken));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Access token must not be null");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenRefreshTokenIsNull() {
            // When
            var thrown = catchThrowable(() -> JwtAuthentication.unAuthenticated(userId, accessToken, null));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Refresh token must not be null");
        }

        @Test
        void ThenReturnsInactiveUser_GivenParametersAreValid() {
            // When
            var actual = JwtAuthentication.unAuthenticated(userId, accessToken, refreshToken);

            // Then
            assertThat(actual.getId()).isNull();
            assertThat(actual.getUserId()).isEqualTo(userId);
            assertThat(actual.accessToken()).isEqualTo(accessToken);
            assertThat(actual.refreshToken()).isEqualTo(refreshToken);
        }

    }

    @Nested
    class CreateJwtAuthenticated {

        @Test
        void ThenThrowsIllegalArgumentException_GivenIdIsNull() {
            // When
            var thrown = catchThrowable(() -> JwtAuthentication.authenticated(null, userId, accessToken, refreshToken));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("ID must not be null");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenUserIdIsNull() {
            // When
            var thrown = catchThrowable(() -> JwtAuthentication.authenticated(id, null, accessToken, refreshToken));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("User ID must not be null");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenAccessTokenIsNull() {
            // When
            var thrown = catchThrowable(() -> JwtAuthentication.authenticated(id, userId, null, refreshToken));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Access token must not be null");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenRefreshTokenIsNull() {
            // When
            var thrown = catchThrowable(() -> JwtAuthentication.authenticated(id, userId, accessToken, null));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Refresh token must not be null");
        }

        @Test
        void ThenReturnsInactiveUser_GivenParametersAreValid() {
            // When
            var actual = JwtAuthentication.authenticated(id, userId, accessToken, refreshToken);

            // Then
            assertThat(actual.getId()).isEqualTo(id);
            assertThat(actual.getUserId()).isEqualTo(userId);
            assertThat(actual.accessToken()).isEqualTo(accessToken);
            assertThat(actual.refreshToken()).isEqualTo(refreshToken);
        }

    }

    @Nested
    class GetAccessToken {

        @Test
        void ThenReturnsAccessToken_GivenJwtUnAuthenticated() {
            // Given
            var jwtAuthentication = JwtAuthentication.unAuthenticated(userId, accessToken, refreshToken);

            // When
            var actual = jwtAuthentication.accessToken();

            // Then
            assertThat(actual).isEqualTo(accessToken);
        }

        @Test
        void ThenReturnsAccessToken_GivenJwtAuthenticated() {
            // Given
            var jwtAuthentication = JwtAuthentication.authenticated(id, userId, accessToken, refreshToken);

            // When
            var actual = jwtAuthentication.accessToken();

            // Then
            assertThat(actual).isEqualTo(accessToken);
        }

    }

    @Nested
    class GetRefreshToken {

        @Test
        void ThenReturnsAccessToken_GivenJwtUnAuthenticated() {
            // Given
            var jwtAuthentication = JwtAuthentication.unAuthenticated(userId, accessToken, refreshToken);

            // When
            var actual = jwtAuthentication.refreshToken();

            // Then
            assertThat(actual).isEqualTo(refreshToken);
        }

        @Test
        void ThenReturnsAccessToken_GivenJwtAuthenticated() {
            // Given
            var jwtAuthentication = JwtAuthentication.authenticated(id, userId, accessToken, refreshToken);

            // When
            var actual = jwtAuthentication.refreshToken();

            // Then
            assertThat(actual).isEqualTo(refreshToken);
        }

    }

    @Nested
    class UpdateTokens {

        @Test
        void ThenThrowsIllegalArgumentException_GivenAccessTokenIsNullOnJwtUnAuthenticated() {
            // Given
            var jwtAuthentication = JwtAuthentication.unAuthenticated(userId, accessToken, refreshToken);

            // When
            var thrown = catchThrowable(() -> jwtAuthentication.updateTokens(null, refreshToken));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Access token must not be null");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenRefreshTokenIsNullOnJwtUnAuthenticated() {
            // Given
            var jwtAuthentication = JwtAuthentication.unAuthenticated(userId, accessToken, refreshToken);

            // When
            var thrown = catchThrowable(() -> jwtAuthentication.updateTokens(accessToken, null));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Refresh token must not be null");
        }

        @Test
        void ThenUpdatesTokens_GivenAccessTokenAndRefreshTokenAreValidOnJwtUnAuthenticated() {
            // Given
            var jwtAuthentication = JwtAuthentication.unAuthenticated(userId, accessToken, refreshToken);

            var token = EncodedToken.of("new_token");
            var subject = Subject.of("new_subject");
            var accessTokenClaims = JwtClaims.of(Map.of(TOKEN_USE_CLAIM_NAME, ACCESS_TOKEN_USE_CLAIM_VALUE));
            var refreshTokenClaims = JwtClaims.of(Map.of(TOKEN_USE_CLAIM_NAME, REFRESH_TOKEN_USE_CLAIM_VALUE));
            var issued = Issued.now();
            var expiration = Expiration.of(issued, 1000L);
            var newAccessToken = Jwt.of(token, ACCESS_TOKEN, subject, accessTokenClaims, issued, expiration);
            var newRefreshToken = Jwt.of(token, REFRESH_TOKEN, subject, refreshTokenClaims, issued, expiration);

            // When
            jwtAuthentication.updateTokens(newAccessToken, newRefreshToken);

            // Then
            assertThat(jwtAuthentication.accessToken()).isEqualTo(newAccessToken);
            assertThat(jwtAuthentication.refreshToken()).isEqualTo(newRefreshToken);
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenAccessTokenIsNullOnJwtAuthenticated() {
            // Given
            var jwtAuthentication = JwtAuthentication.authenticated(id, userId, accessToken, refreshToken);

            // When
            var thrown = catchThrowable(() -> jwtAuthentication.updateTokens(null, refreshToken));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Access token must not be null");
        }

        @Test
        void ThenThrowsIllegalArgumentException_GivenRefreshTokenIsNullOnJwtAuthenticated() {
            // Given
            var jwtAuthentication = JwtAuthentication.authenticated(id, userId, accessToken, refreshToken);

            // When
            var thrown = catchThrowable(() -> jwtAuthentication.updateTokens(accessToken, null));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("Refresh token must not be null");
        }

        @Test
        void ThenUpdatesTokens_GivenAccessTokenAndRefreshTokenAreValidOnJwtAuthenticated() {
            // Given
            var jwtAuthentication = JwtAuthentication.authenticated(id, userId, accessToken, refreshToken);

            var token = EncodedToken.of("new_token");
            var subject = Subject.of("new_subject");
            var accessTokenClaims = JwtClaims.of(Map.of(TOKEN_USE_CLAIM_NAME, ACCESS_TOKEN_USE_CLAIM_VALUE));
            var refreshTokenClaims = JwtClaims.of(Map.of(TOKEN_USE_CLAIM_NAME, REFRESH_TOKEN_USE_CLAIM_VALUE));
            var issued = Issued.now();
            var expiration = Expiration.of(issued, 1000L);
            var newAccessToken = Jwt.of(token, ACCESS_TOKEN, subject, accessTokenClaims, issued, expiration);
            var newRefreshToken = Jwt.of(token, REFRESH_TOKEN, subject, refreshTokenClaims, issued, expiration);

            // When
            jwtAuthentication.updateTokens(newAccessToken, newRefreshToken);

            // Then
            assertThat(jwtAuthentication.accessToken()).isEqualTo(newAccessToken);
            assertThat(jwtAuthentication.refreshToken()).isEqualTo(newRefreshToken);
        }

    }

    @Nested
    class CheckEquality {

        @Test
        void ThenReturnsFalse_GivenOtherJwtAuthenticationIsNull() {
            // Given
            var jwtAuthentication = JwtAuthentication.authenticated(id, userId, accessToken, refreshToken);

            // When
            var actual = jwtAuthentication.equals(null);

            // Then
            assertThat(actual).isFalse();
        }

        @Test
        void ThenReturnsFalse_GivenOtherClassIsNotJwtAuthentication() {
            // Given
            var jwtAuthentication = JwtAuthentication.authenticated(id, userId, accessToken, refreshToken);
            var other = "other";

            // When
            var actual = jwtAuthentication.equals(other);

            // Then
            assertThat(actual).isFalse();
        }

        @Test
        void ThenReturnsTrue_GivenOtherJwtAuthenticationIsSameObject() {
            // Given
            var jwtAuthentication = JwtAuthentication.authenticated(id, userId, accessToken, refreshToken);

            // When
            var actual = jwtAuthentication.equals(jwtAuthentication);

            // Then
            assertThat(actual).isTrue();
        }

        @Test
        void ThenReturnsFalse_GivenJwtUnAuthenticatedAndJwtAuthenticated() {
            // Given
            var jwtAuthentication1 = JwtAuthentication.unAuthenticated(userId, accessToken, refreshToken);
            var jwtAuthentication2 = JwtAuthentication.authenticated(id, userId, accessToken, refreshToken);

            // When
            var actual1 = jwtAuthentication1.equals(jwtAuthentication2);
            var actual2 = jwtAuthentication2.equals(jwtAuthentication1);

            // Then
            assertThat(actual1).isFalse();
            assertThat(actual2).isFalse();
        }

        @Test
        void ThenReturnsTrue_GivenThreeJwtAuthenticationWithSameId() {
            // Given
            var jwtAuthentication1 = JwtAuthentication.authenticated(id, userId, accessToken, refreshToken);
            var jwtAuthentication2 = JwtAuthentication.authenticated(id, userId, accessToken, refreshToken);
            var jwtAuthentication3 = JwtAuthentication.authenticated(id, userId, accessToken, refreshToken);

            // When
            var actual1 = jwtAuthentication1.equals(jwtAuthentication2);
            var actual2 = jwtAuthentication2.equals(jwtAuthentication3);
            var actual3 = jwtAuthentication1.equals(jwtAuthentication3);

            // Then
            assertThat(actual1).isTrue();
            assertThat(actual2).isTrue();
            assertThat(actual3).isTrue();
        }

        @Test
        void ThenReturnsFalse_GivenThreeJwtAuthenticationWithDifferentId() {
            // Given
            var jwtAuthentication1 = JwtAuthentication.authenticated(JwtAuthenticationId.of(UUID.randomUUID()), userId, accessToken, refreshToken);
            var jwtAuthentication2 = JwtAuthentication.authenticated(JwtAuthenticationId.of(UUID.randomUUID()), userId, accessToken, refreshToken);
            var jwtAuthentication3 = JwtAuthentication.authenticated(JwtAuthenticationId.of(UUID.randomUUID()), userId, accessToken, refreshToken);

            // When
            var actual1 = jwtAuthentication1.equals(jwtAuthentication2);
            var actual2 = jwtAuthentication2.equals(jwtAuthentication3);
            var actual3 = jwtAuthentication1.equals(jwtAuthentication3);

            // Then
            assertThat(actual1).isFalse();
            assertThat(actual2).isFalse();
            assertThat(actual3).isFalse();
        }

    }

    @Nested
    class HashCode {

        @Test
        void ThenReturnsDifferentHashCode_GivenTwoJwtUnAuthenticated() {
            // Given
            var jwtAuthentication1 = JwtAuthentication.unAuthenticated(userId, accessToken, refreshToken);
            var jwtAuthentication2 = JwtAuthentication.unAuthenticated(userId, accessToken, refreshToken);

            // When
            var actual1 = jwtAuthentication1.hashCode();
            var actual2 = jwtAuthentication2.hashCode();

            // Then
            assertThat(actual1).isNotEqualTo(actual2);
        }

        @Test
        void ThenReturnsDifferentHashCode_GivenJwtUnAuthenticatedAndJwtAuthenticated() {
            // Given
            var jwtAuthentication1 = JwtAuthentication.unAuthenticated(userId, accessToken, refreshToken);
            var jwtAuthentication2 = JwtAuthentication.authenticated(id, userId, accessToken, refreshToken);

            // When
            var actual1 = jwtAuthentication1.hashCode();
            var actual2 = jwtAuthentication2.hashCode();

            // Then
            assertThat(actual1).isNotEqualTo(actual2);
        }

        @Test
        void ThenReturnsSameHashCode_GivenTwoJwtAuthenticatedWithSameId() {
            // Given
            var jwtAuthentication1 = JwtAuthentication.authenticated(id, userId, accessToken, refreshToken);
            var jwtAuthentication2 = JwtAuthentication.authenticated(id, userId, accessToken, refreshToken);

            // When
            var actual1 = jwtAuthentication1.hashCode();
            var actual2 = jwtAuthentication2.hashCode();

            // Then
            assertThat(actual1).isEqualTo(actual2);
        }

        @Test
        void ThenReturnsDifferentHashCode_GivenTwoJwtAuthenticatedWithDifferentId() {
            // Given
            var jwtAuthentication1 = JwtAuthentication.authenticated(JwtAuthenticationId.of(UUID.randomUUID()), userId, accessToken, refreshToken);
            var jwtAuthentication2 = JwtAuthentication.authenticated(JwtAuthenticationId.of(UUID.randomUUID()), userId, accessToken, refreshToken);

            // When
            var actual1 = jwtAuthentication1.hashCode();
            var actual2 = jwtAuthentication2.hashCode();

            // Then
            assertThat(actual1).isNotEqualTo(actual2);
        }

    }

    @Nested
    class GetId {

        @Test
        void ThenReturnsNull_GivenJwtUnAuthenticated() {
            // Given
            var jwtAuthentication = JwtAuthentication.unAuthenticated(userId, accessToken, refreshToken);

            // When
            var actual = jwtAuthentication.getId();

            // Then
            assertThat(actual).isNull();
        }

        @Test
        void ThenReturnsId_GivenJwtAuthenticated() {
            // Given
            var jwtAuthentication = JwtAuthentication.authenticated(id, userId, accessToken, refreshToken);

            // When
            var actual = jwtAuthentication.getId();

            // Then
            assertThat(actual).isEqualTo(id);
        }

    }

    @Nested
    class GetUserId {

        @Test
        void ThenReturnsUserId_GivenJwtUnAuthenticated() {
            // Given
            var jwtAuthentication = JwtAuthentication.unAuthenticated(userId, accessToken, refreshToken);

            // When
            var actual = jwtAuthentication.getUserId();

            // Then
            assertThat(actual).isEqualTo(userId);
        }

        @Test
        void ThenReturnsUserId_GivenJwtAuthenticated() {
            // Given
            var jwtAuthentication = JwtAuthentication.authenticated(id, userId, accessToken, refreshToken);

            // When
            var actual = jwtAuthentication.getUserId();

            // Then
            assertThat(actual).isEqualTo(userId);
        }

    }

    @Nested
    class GetJwtPair {

        @Test
        void ThenReturnsJwtPair_GivenJwtUnAuthenticated() {
            // Given
            var jwtAuthentication = JwtAuthentication.unAuthenticated(userId, accessToken, refreshToken);

            // When
            var actual = jwtAuthentication.getJwtPair();

            // Then
            assertThat(actual.accessToken()).isEqualTo(accessToken);
            assertThat(actual.refreshToken()).isEqualTo(refreshToken);
        }

        @Test
        void ThenReturnsJwtPair_GivenJwtAuthenticated() {
            // Given
            var jwtAuthentication = JwtAuthentication.authenticated(id, userId, accessToken, refreshToken);

            // When
            var actual = jwtAuthentication.getJwtPair();

            // Then
            assertThat(actual.accessToken()).isEqualTo(accessToken);
            assertThat(actual.refreshToken()).isEqualTo(refreshToken);
        }

    }

}

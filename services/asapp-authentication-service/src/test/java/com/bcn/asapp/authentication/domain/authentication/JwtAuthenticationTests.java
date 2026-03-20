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

import static com.bcn.asapp.authentication.domain.authentication.JwtClaimNames.ACCESS_TOKEN_USE;
import static com.bcn.asapp.authentication.domain.authentication.JwtClaimNames.REFRESH_TOKEN_USE;
import static com.bcn.asapp.authentication.domain.authentication.JwtClaimNames.ROLE;
import static com.bcn.asapp.authentication.domain.authentication.JwtClaimNames.TOKEN_USE;
import static com.bcn.asapp.authentication.domain.user.Role.ADMIN;
import static com.bcn.asapp.authentication.testutil.fixture.JwtFactory.aJwtBuilder;
import static com.bcn.asapp.authentication.testutil.fixture.JwtFactory.aRefreshToken;
import static com.bcn.asapp.authentication.testutil.fixture.JwtFactory.anAccessToken;
import static com.bcn.asapp.authentication.testutil.fixture.JwtPairFactory.aJwtPair;
import static com.bcn.asapp.authentication.testutil.fixture.JwtPairFactory.aJwtPairBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.bcn.asapp.authentication.domain.user.UserId;

/**
 * Tests {@link JwtAuthentication} lifecycle states, token refresh, and identity equality.
 * <p>
 * Coverage:
 * <li>Creates unauthenticated JWT with user ID and token pair, null authentication ID</li>
 * <li>Creates authenticated JWT with authentication ID, user ID, and token pair</li>
 * <li>Refreshes JWT pair with new tokens while maintaining authentication ID and user ID</li>
 * <li>Validates user ID and JWT pair required for both states</li>
 * <li>Validates authentication ID required for authenticated state</li>
 * <li>Implements identity-based equality using authentication ID for authenticated, user ID for unauthenticated</li>
 */
class JwtAuthenticationTests {

    @Nested
    class CreateJwtUnAuthenticated {

        @Test
        void ReturnsUnauthenticatedJwt_ValidParameters() {
            // Given
            var userId = UserId.of(UUID.fromString("3d4e5f67-8901-4234-b567-890abcdef123"));
            var accessToken = anAccessToken();
            var refreshToken = aRefreshToken();
            var jwtPair = aJwtPairBuilder().withTokens(accessToken, refreshToken)
                                           .build();

            // When
            var actual = JwtAuthentication.unAuthenticated(userId, jwtPair);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual.getId()).as("ID").isNull();
                softly.assertThat(actual.getUserId()).as("user ID").isEqualTo(userId);
                softly.assertThat(actual.accessToken()).as("access token").isEqualTo(accessToken);
                softly.assertThat(actual.refreshToken()).as("refresh token").isEqualTo(refreshToken);
                // @formatter:on
            });
        }

        @Test
        void ThrowsIllegalArgumentException_NullUserId() {
            // Given
            var jwtPair = aJwtPair();

            // When
            var actual = catchThrowable(() -> JwtAuthentication.unAuthenticated(null, jwtPair));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("User ID must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullJwtPair() {
            // Given
            var userId = UserId.of(UUID.fromString("3d4e5f67-8901-4234-b567-890abcdef123"));

            // When
            var actual = catchThrowable(() -> JwtAuthentication.unAuthenticated(userId, null));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("JWT pair must not be null");
        }

    }

    @Nested
    class CreateJwtAuthenticated {

        @Test
        void ReturnsAuthenticatedJwt_ValidParameters() {
            // Given
            var jwtAuthenticationId = JwtAuthenticationId.of(UUID.fromString("2c3d4e5f-6789-4012-a345-6789abcdef12"));
            var userId = UserId.of(UUID.fromString("3d4e5f67-8901-4234-b567-890abcdef123"));
            var accessToken = anAccessToken();
            var refreshToken = aRefreshToken();
            var jwtPair = aJwtPairBuilder().withTokens(accessToken, refreshToken)
                                           .build();

            // When
            var actual = JwtAuthentication.authenticated(jwtAuthenticationId, userId, jwtPair);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual.getId()).as("ID").isEqualTo(jwtAuthenticationId);
                softly.assertThat(actual.getUserId()).as("user ID").isEqualTo(userId);
                softly.assertThat(actual.accessToken()).as("access token").isEqualTo(accessToken);
                softly.assertThat(actual.refreshToken()).as("refresh token").isEqualTo(refreshToken);
                // @formatter:on
            });
        }

        @Test
        void ThrowsIllegalArgumentException_NullId() {
            // Given
            var userId = UserId.of(UUID.fromString("3d4e5f67-8901-4234-b567-890abcdef123"));
            var jwtPair = aJwtPair();

            // When
            var actual = catchThrowable(() -> JwtAuthentication.authenticated(null, userId, jwtPair));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("ID must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullUserId() {
            // Given
            var jwtAuthenticationId = JwtAuthenticationId.of(UUID.fromString("2c3d4e5f-6789-4012-a345-6789abcdef12"));
            var jwtPair = aJwtPair();

            // When
            var actual = catchThrowable(() -> JwtAuthentication.authenticated(jwtAuthenticationId, null, jwtPair));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("User ID must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullJwtPair() {
            // Given
            var jwtAuthenticationId = JwtAuthenticationId.of(UUID.fromString("2c3d4e5f-6789-4012-a345-6789abcdef12"));
            var userId = UserId.of(UUID.fromString("3d4e5f67-8901-4234-b567-890abcdef123"));

            // When
            var actual = catchThrowable(() -> JwtAuthentication.authenticated(jwtAuthenticationId, userId, null));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("JWT pair must not be null");
        }

    }

    @Nested
    class GetAccessToken {

        @Test
        void ReturnsAccessToken_JwtUnauthenticated() {
            // Given
            var userId = UserId.of(UUID.fromString("3d4e5f67-8901-4234-b567-890abcdef123"));
            var accessToken = anAccessToken();
            var jwtPair = aJwtPairBuilder().withAccessToken(accessToken)
                                           .build();
            var jwtAuthentication = JwtAuthentication.unAuthenticated(userId, jwtPair);

            // When
            var actual = jwtAuthentication.accessToken();

            // Then
            assertThat(actual).isEqualTo(accessToken);
        }

        @Test
        void ReturnsAccessToken_JwtAuthenticated() {
            // Given
            var jwtAuthenticationId = JwtAuthenticationId.of(UUID.fromString("2c3d4e5f-6789-4012-a345-6789abcdef12"));
            var userId = UserId.of(UUID.fromString("3d4e5f67-8901-4234-b567-890abcdef123"));
            var accessToken = anAccessToken();
            var jwtPair = aJwtPairBuilder().withAccessToken(accessToken)
                                           .build();
            var jwtAuthentication = JwtAuthentication.authenticated(jwtAuthenticationId, userId, jwtPair);

            // When
            var actual = jwtAuthentication.accessToken();

            // Then
            assertThat(actual).isEqualTo(accessToken);
        }

    }

    @Nested
    class GetRefreshToken {

        @Test
        void ReturnsRefreshToken_JwtUnauthenticated() {
            // Given
            var userId = UserId.of(UUID.fromString("3d4e5f67-8901-4234-b567-890abcdef123"));
            var refreshToken = aRefreshToken();
            var jwtPair = aJwtPairBuilder().withRefreshToken(refreshToken)
                                           .build();
            var jwtAuthentication = JwtAuthentication.unAuthenticated(userId, jwtPair);

            // When
            var actual = jwtAuthentication.refreshToken();

            // Then
            assertThat(actual).isEqualTo(refreshToken);
        }

        @Test
        void ReturnsRefreshToken_JwtAuthenticated() {
            // Given
            var jwtAuthenticationId = JwtAuthenticationId.of(UUID.fromString("2c3d4e5f-6789-4012-a345-6789abcdef12"));
            var userId = UserId.of(UUID.fromString("3d4e5f67-8901-4234-b567-890abcdef123"));
            var refreshToken = aRefreshToken();
            var jwtPair = aJwtPairBuilder().withRefreshToken(refreshToken)
                                           .build();
            var jwtAuthentication = JwtAuthentication.authenticated(jwtAuthenticationId, userId, jwtPair);

            // When
            var actual = jwtAuthentication.refreshToken();

            // Then
            assertThat(actual).isEqualTo(refreshToken);
        }

    }

    @Nested
    class RefreshTokens {

        @Test
        void RefreshesTokens_ValidJwtPairOnJwtUnauthenticated() {
            // Given
            var userId = UserId.of(UUID.fromString("3d4e5f67-8901-4234-b567-890abcdef123"));
            var jwtPair = aJwtPair();
            var jwtAuthentication = JwtAuthentication.unAuthenticated(userId, jwtPair);

            var newAccessToken = aJwtBuilder().accessToken()
                                              .withSubject("new_user@asapp.com")
                                              .withClaims(Map.of(TOKEN_USE, ACCESS_TOKEN_USE, ROLE, ADMIN.name()))
                                              .build();
            var newRefreshToken = aJwtBuilder().refreshToken()
                                               .withSubject("new_user@asapp.com")
                                               .withClaims(Map.of(TOKEN_USE, REFRESH_TOKEN_USE, ROLE, ADMIN.name()))
                                               .build();
            var newJwtPair = aJwtPairBuilder().withTokens(newAccessToken, newRefreshToken)
                                              .build();

            // When
            jwtAuthentication.refreshTokens(newJwtPair);

            // Then
            assertThat(jwtAuthentication.accessToken()).isEqualTo(newAccessToken);
            assertThat(jwtAuthentication.refreshToken()).isEqualTo(newRefreshToken);
        }

        @Test
        void RefreshesTokens_ValidJwtPairOnJwtAuthenticated() {
            // Given
            var jwtAuthenticationId = JwtAuthenticationId.of(UUID.fromString("2c3d4e5f-6789-4012-a345-6789abcdef12"));
            var userId = UserId.of(UUID.fromString("3d4e5f67-8901-4234-b567-890abcdef123"));
            var jwtPair = aJwtPair();
            var jwtAuthentication = JwtAuthentication.authenticated(jwtAuthenticationId, userId, jwtPair);

            var newAccessToken = aJwtBuilder().accessToken()
                                              .withSubject("new_user@asapp.com")
                                              .withClaims(Map.of(TOKEN_USE, ACCESS_TOKEN_USE, ROLE, ADMIN.name()))
                                              .build();
            var newRefreshToken = aJwtBuilder().refreshToken()
                                               .withSubject("new_user@asapp.com")
                                               .withClaims(Map.of(TOKEN_USE, REFRESH_TOKEN_USE, ROLE, ADMIN.name()))
                                               .build();
            var newJwtPair = aJwtPairBuilder().withTokens(newAccessToken, newRefreshToken)
                                              .build();

            // When
            jwtAuthentication.refreshTokens(newJwtPair);

            // Then
            assertThat(jwtAuthentication.accessToken()).isEqualTo(newAccessToken);
            assertThat(jwtAuthentication.refreshToken()).isEqualTo(newRefreshToken);
        }

        @Test
        void ThrowsIllegalArgumentException_NullJwtPairOnJwtUnAuthenticated() {
            // Given
            var userId = UserId.of(UUID.fromString("3d4e5f67-8901-4234-b567-890abcdef123"));
            var jwtPair = aJwtPair();
            var jwtAuthentication = JwtAuthentication.unAuthenticated(userId, jwtPair);

            // When
            var actual = catchThrowable(() -> jwtAuthentication.refreshTokens(null));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("JWT pair must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullJwtPairOnJwtAuthenticated() {
            // Given
            var jwtAuthenticationId = JwtAuthenticationId.of(UUID.fromString("2c3d4e5f-6789-4012-a345-6789abcdef12"));
            var userId = UserId.of(UUID.fromString("3d4e5f67-8901-4234-b567-890abcdef123"));
            var jwtPair = aJwtPair();
            var jwtAuthentication = JwtAuthentication.authenticated(jwtAuthenticationId, userId, jwtPair);

            // When
            var actual = catchThrowable(() -> jwtAuthentication.refreshTokens(null));

            // Then
            assertThat(actual).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("JWT pair must not be null");
        }

    }

    @Nested
    class CheckEquality {

        @Test
        void ReturnsTrue_SameObjectOtherJwtAuthentication() {
            // Given
            var jwtAuthenticationId = JwtAuthenticationId.of(UUID.fromString("2c3d4e5f-6789-4012-a345-6789abcdef12"));
            var userId = UserId.of(UUID.fromString("3d4e5f67-8901-4234-b567-890abcdef123"));
            var jwtPair = aJwtPair();
            var jwtAuthentication = JwtAuthentication.authenticated(jwtAuthenticationId, userId, jwtPair);

            // When
            var actual = jwtAuthentication.equals(jwtAuthentication);

            // Then
            assertThat(actual).isTrue();
        }

        @Test
        void ReturnsTrue_ThreeJwtAuthenticationsSameId() {
            // Given
            var jwtAuthenticationId = JwtAuthenticationId.of(UUID.fromString("2c3d4e5f-6789-4012-a345-6789abcdef12"));
            var userId = UserId.of(UUID.fromString("3d4e5f67-8901-4234-b567-890abcdef123"));
            var jwtPair = aJwtPair();
            var jwtAuthentication1 = JwtAuthentication.authenticated(jwtAuthenticationId, userId, jwtPair);
            var jwtAuthentication2 = JwtAuthentication.authenticated(jwtAuthenticationId, userId, jwtPair);
            var jwtAuthentication3 = JwtAuthentication.authenticated(jwtAuthenticationId, userId, jwtPair);

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
        void ReturnsFalse_NullOtherJwtAuthentication() {
            // Given
            var jwtAuthenticationId = JwtAuthenticationId.of(UUID.fromString("2c3d4e5f-6789-4012-a345-6789abcdef12"));
            var userId = UserId.of(UUID.fromString("3d4e5f67-8901-4234-b567-890abcdef123"));
            var jwtPair = aJwtPair();
            var jwtAuthentication = JwtAuthentication.authenticated(jwtAuthenticationId, userId, jwtPair);

            // When
            var actual = jwtAuthentication.equals(null);

            // Then
            assertThat(actual).isFalse();
        }

        @Test
        void ReturnsFalse_OtherClassNotJwtAuthentication() {
            // Given
            var jwtAuthenticationId = JwtAuthenticationId.of(UUID.fromString("2c3d4e5f-6789-4012-a345-6789abcdef12"));
            var userId = UserId.of(UUID.fromString("3d4e5f67-8901-4234-b567-890abcdef123"));
            var jwtPair = aJwtPair();
            var jwtAuthentication = JwtAuthentication.authenticated(jwtAuthenticationId, userId, jwtPair);
            var other = "not a jwt authentication";

            // When
            var actual = jwtAuthentication.equals(other);

            // Then
            assertThat(actual).isFalse();
        }

        @Test
        void ReturnsFalse_JwtUnauthenticatedAndJwtAuthenticated() {
            // Given
            var jwtAuthenticationId = JwtAuthenticationId.of(UUID.fromString("2c3d4e5f-6789-4012-a345-6789abcdef12"));
            var userId = UserId.of(UUID.fromString("3d4e5f67-8901-4234-b567-890abcdef123"));
            var jwtPair = aJwtPair();
            var jwtAuthentication1 = JwtAuthentication.unAuthenticated(userId, jwtPair);
            var jwtAuthentication2 = JwtAuthentication.authenticated(jwtAuthenticationId, userId, jwtPair);

            // When
            var actual1 = jwtAuthentication1.equals(jwtAuthentication2);
            var actual2 = jwtAuthentication2.equals(jwtAuthentication1);

            // Then
            assertThat(actual1).isFalse();
            assertThat(actual2).isFalse();
        }

        @Test
        void ReturnsFalse_ThreeJwtAuthenticationsDifferentId() {
            // Given
            var userId = UserId.of(UUID.fromString("3d4e5f67-8901-4234-b567-890abcdef123"));
            var jwtPair = aJwtPair();
            var jwtAuthenticationId1 = JwtAuthenticationId.of(UUID.fromString("4e5f6789-0123-4456-c789-0abcdef12345"));
            var jwtAuthenticationId2 = JwtAuthenticationId.of(UUID.fromString("5f678901-2345-4678-d90a-bcdef1234567"));
            var jwtAuthenticationId3 = JwtAuthenticationId.of(UUID.fromString("67890123-4567-4890-a123-bcdef1234567"));
            var jwtAuthentication1 = JwtAuthentication.authenticated(jwtAuthenticationId1, userId, jwtPair);
            var jwtAuthentication2 = JwtAuthentication.authenticated(jwtAuthenticationId2, userId, jwtPair);
            var jwtAuthentication3 = JwtAuthentication.authenticated(jwtAuthenticationId3, userId, jwtPair);

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
        void ReturnsSameHashCode_TwoJwtAuthenticatedSameId() {
            // Given
            var jwtAuthenticationId = JwtAuthenticationId.of(UUID.fromString("2c3d4e5f-6789-4012-a345-6789abcdef12"));
            var userId = UserId.of(UUID.fromString("3d4e5f67-8901-4234-b567-890abcdef123"));
            var jwtPair = aJwtPair();
            var jwtAuthentication1 = JwtAuthentication.authenticated(jwtAuthenticationId, userId, jwtPair);
            var jwtAuthentication2 = JwtAuthentication.authenticated(jwtAuthenticationId, userId, jwtPair);

            // When
            var actual1 = jwtAuthentication1.hashCode();
            var actual2 = jwtAuthentication2.hashCode();

            // Then
            assertThat(actual1).isEqualTo(actual2);
        }

        @Test
        void ReturnsDifferentHashCode_TwoJwtUnauthenticated() {
            // Given
            var userId = UserId.of(UUID.fromString("3d4e5f67-8901-4234-b567-890abcdef123"));
            var jwtPair = aJwtPair();
            var jwtAuthentication1 = JwtAuthentication.unAuthenticated(userId, jwtPair);
            var jwtAuthentication2 = JwtAuthentication.unAuthenticated(userId, jwtPair);

            // When
            var actual1 = jwtAuthentication1.hashCode();
            var actual2 = jwtAuthentication2.hashCode();

            // Then
            assertThat(actual1).isNotEqualTo(actual2);
        }

        @Test
        void ReturnsDifferentHashCode_JwtUnauthenticatedAndJwtAuthenticated() {
            // Given
            var jwtAuthenticationId = JwtAuthenticationId.of(UUID.fromString("2c3d4e5f-6789-4012-a345-6789abcdef12"));
            var userId = UserId.of(UUID.fromString("3d4e5f67-8901-4234-b567-890abcdef123"));
            var jwtPair = aJwtPair();
            var jwtAuthentication1 = JwtAuthentication.unAuthenticated(userId, jwtPair);
            var jwtAuthentication2 = JwtAuthentication.authenticated(jwtAuthenticationId, userId, jwtPair);

            // When
            var actual1 = jwtAuthentication1.hashCode();
            var actual2 = jwtAuthentication2.hashCode();

            // Then
            assertThat(actual1).isNotEqualTo(actual2);
        }

        @Test
        void ReturnsDifferentHashCode_TwoJwtAuthenticatedDifferentId() {
            // Given
            var userId = UserId.of(UUID.fromString("3d4e5f67-8901-4234-b567-890abcdef123"));
            var jwtPair = aJwtPair();
            var jwtAuthenticationId1 = JwtAuthenticationId.of(UUID.fromString("78901234-5678-4abc-b234-def123456789"));
            var jwtAuthenticationId2 = JwtAuthenticationId.of(UUID.fromString("89012345-6789-4bcd-c345-ef1234567890"));
            var jwtAuthentication1 = JwtAuthentication.authenticated(jwtAuthenticationId1, userId, jwtPair);
            var jwtAuthentication2 = JwtAuthentication.authenticated(jwtAuthenticationId2, userId, jwtPair);

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
        void ReturnsId_JwtAuthenticated() {
            // Given
            var jwtAuthenticationId = JwtAuthenticationId.of(UUID.fromString("2c3d4e5f-6789-4012-a345-6789abcdef12"));
            var userId = UserId.of(UUID.fromString("3d4e5f67-8901-4234-b567-890abcdef123"));
            var jwtPair = aJwtPair();
            var jwtAuthentication = JwtAuthentication.authenticated(jwtAuthenticationId, userId, jwtPair);

            // When
            var actual = jwtAuthentication.getId();

            // Then
            assertThat(actual).isEqualTo(jwtAuthenticationId);
        }

        @Test
        void ReturnsNull_JwtUnauthenticated() {
            // Given
            var userId = UserId.of(UUID.fromString("3d4e5f67-8901-4234-b567-890abcdef123"));
            var jwtPair = aJwtPair();
            var jwtAuthentication = JwtAuthentication.unAuthenticated(userId, jwtPair);

            // When
            var actual = jwtAuthentication.getId();

            // Then
            assertThat(actual).isNull();
        }

    }

    @Nested
    class GetUserId {

        @Test
        void ReturnsUserId_JwtUnauthenticated() {
            // Given
            var userId = UserId.of(UUID.fromString("3d4e5f67-8901-4234-b567-890abcdef123"));
            var jwtPair = aJwtPair();
            var jwtAuthentication = JwtAuthentication.unAuthenticated(userId, jwtPair);

            // When
            var actual = jwtAuthentication.getUserId();

            // Then
            assertThat(actual).isEqualTo(userId);
        }

        @Test
        void ReturnsUserId_JwtAuthenticated() {
            // Given
            var jwtAuthenticationId = JwtAuthenticationId.of(UUID.fromString("2c3d4e5f-6789-4012-a345-6789abcdef12"));
            var userId = UserId.of(UUID.fromString("3d4e5f67-8901-4234-b567-890abcdef123"));
            var jwtPair = aJwtPair();
            var jwtAuthentication = JwtAuthentication.authenticated(jwtAuthenticationId, userId, jwtPair);

            // When
            var actual = jwtAuthentication.getUserId();

            // Then
            assertThat(actual).isEqualTo(userId);
        }

    }

    @Nested
    class GetJwtPair {

        @Test
        void ReturnsJwtPair_JwtUnauthenticated() {
            // Given
            var userId = UserId.of(UUID.fromString("3d4e5f67-8901-4234-b567-890abcdef123"));
            var accessToken = anAccessToken();
            var refreshToken = aRefreshToken();
            var jwtPair = aJwtPairBuilder().withTokens(accessToken, refreshToken)
                                           .build();
            var jwtAuthentication = JwtAuthentication.unAuthenticated(userId, jwtPair);

            // When
            var actual = jwtAuthentication.getJwtPair();

            // Then
            assertThat(actual.accessToken()).isEqualTo(accessToken);
            assertThat(actual.refreshToken()).isEqualTo(refreshToken);
        }

        @Test
        void ReturnsJwtPair_JwtAuthenticated() {
            // Given
            var jwtAuthenticationId = JwtAuthenticationId.of(UUID.fromString("2c3d4e5f-6789-4012-a345-6789abcdef12"));
            var userId = UserId.of(UUID.fromString("3d4e5f67-8901-4234-b567-890abcdef123"));
            var accessToken = anAccessToken();
            var refreshToken = aRefreshToken();
            var jwtPair = aJwtPairBuilder().withTokens(accessToken, refreshToken)
                                           .build();
            var jwtAuthentication = JwtAuthentication.authenticated(jwtAuthenticationId, userId, jwtPair);

            // When
            var actual = jwtAuthentication.getJwtPair();

            // Then
            assertThat(actual.accessToken()).isEqualTo(accessToken);
            assertThat(actual.refreshToken()).isEqualTo(refreshToken);
        }

    }

}

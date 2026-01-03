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
import static com.bcn.asapp.authentication.domain.authentication.JwtType.ACCESS_TOKEN;
import static com.bcn.asapp.authentication.domain.authentication.JwtType.REFRESH_TOKEN;
import static com.bcn.asapp.authentication.domain.user.Role.ADMIN;
import static com.bcn.asapp.authentication.domain.user.Role.USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;

import java.time.Instant;
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
        var token = EncodedToken.of("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyIn0.aGFzaFZhbHVl");
        var subject = Subject.of("user@asapp.com");
        var accessTokenClaims = JwtClaims.of(Map.of(TOKEN_USE, ACCESS_TOKEN_USE, ROLE, USER.name()));
        var refreshTokenClaims = JwtClaims.of(Map.of(TOKEN_USE, REFRESH_TOKEN_USE, ROLE, USER.name()));
        var issued = Issued.of(Instant.parse("2025-01-01T10:00:00Z"));
        var expiration = Expiration.of(issued, 30000L);

        id = JwtAuthenticationId.of(UUID.fromString("2c3d4e5f-6789-4012-a345-6789abcdef12"));
        userId = UserId.of(UUID.fromString("3d4e5f67-8901-4234-b567-890abcdef123"));
        accessToken = Jwt.of(token, ACCESS_TOKEN, subject, accessTokenClaims, issued, expiration);
        refreshToken = Jwt.of(token, REFRESH_TOKEN, subject, refreshTokenClaims, issued, expiration);
    }

    @Nested
    class CreateJwtUnAuthenticated {

        @Test
        void ThrowsIllegalArgumentException_NullUserId() {
            // Given
            var jwtPair = JwtPair.of(accessToken, refreshToken);

            // When
            var thrown = catchThrowable(() -> JwtAuthentication.unAuthenticated(null, jwtPair));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("User ID must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullJwtPair() {
            // When
            var thrown = catchThrowable(() -> JwtAuthentication.unAuthenticated(userId, null));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("JWT pair must not be null");
        }

        @Test
        void ReturnsUnauthenticatedJwt_ValidParameters() {
            // Given
            var jwtPair = JwtPair.of(accessToken, refreshToken);

            // When
            var actual = JwtAuthentication.unAuthenticated(userId, jwtPair);

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
        void ThrowsIllegalArgumentException_NullId() {
            // Given
            var jwtPair = JwtPair.of(accessToken, refreshToken);

            // When
            var thrown = catchThrowable(() -> JwtAuthentication.authenticated(null, userId, jwtPair));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("ID must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullUserId() {
            // Given
            var jwtPair = JwtPair.of(accessToken, refreshToken);

            // When
            var thrown = catchThrowable(() -> JwtAuthentication.authenticated(id, null, jwtPair));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("User ID must not be null");
        }

        @Test
        void ThrowsIllegalArgumentException_NullJwtPair() {
            // When
            var thrown = catchThrowable(() -> JwtAuthentication.authenticated(id, userId, null));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("JWT pair must not be null");
        }

        @Test
        void ReturnsUnauthenticatedJwt_ValidParameters() {
            // Given
            var jwtPair = JwtPair.of(accessToken, refreshToken);

            // When
            var actual = JwtAuthentication.authenticated(id, userId, jwtPair);

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
        void ReturnsAccessToken_JwtUnauthenticated() {
            // Given
            var jwtPair = JwtPair.of(accessToken, refreshToken);
            var jwtAuthentication = JwtAuthentication.unAuthenticated(userId, jwtPair);

            // When
            var actual = jwtAuthentication.accessToken();

            // Then
            assertThat(actual).isEqualTo(accessToken);
        }

        @Test
        void ReturnsAccessToken_JwtAuthenticated() {
            // Given
            var jwtPair = JwtPair.of(accessToken, refreshToken);
            var jwtAuthentication = JwtAuthentication.authenticated(id, userId, jwtPair);

            // When
            var actual = jwtAuthentication.accessToken();

            // Then
            assertThat(actual).isEqualTo(accessToken);
        }

    }

    @Nested
    class GetRefreshToken {

        @Test
        void ReturnsAccessToken_JwtUnauthenticated() {
            // Given
            var jwtPair = JwtPair.of(accessToken, refreshToken);
            var jwtAuthentication = JwtAuthentication.unAuthenticated(userId, jwtPair);

            // When
            var actual = jwtAuthentication.refreshToken();

            // Then
            assertThat(actual).isEqualTo(refreshToken);
        }

        @Test
        void ReturnsAccessToken_JwtAuthenticated() {
            // Given
            var jwtPair = JwtPair.of(accessToken, refreshToken);
            var jwtAuthentication = JwtAuthentication.authenticated(id, userId, jwtPair);

            // When
            var actual = jwtAuthentication.refreshToken();

            // Then
            assertThat(actual).isEqualTo(refreshToken);
        }

    }

    @Nested
    class RefreshTokens {

        @Test
        void ThrowsIllegalArgumentException_NullJwtPairOnJwtUnAuthenticated() {
            // Given
            var jwtPair = JwtPair.of(accessToken, refreshToken);
            var jwtAuthentication = JwtAuthentication.unAuthenticated(userId, jwtPair);

            // When
            var thrown = catchThrowable(() -> jwtAuthentication.refreshTokens(null));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("JWT pair must not be null");
        }

        @Test
        void RefreshesTokens_ValidJwtPairOnJwtUnauthenticated() {
            // Given
            var jwtPair = JwtPair.of(accessToken, refreshToken);
            var jwtAuthentication = JwtAuthentication.unAuthenticated(userId, jwtPair);

            var newEncodedToken = EncodedToken.of("eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyQGFzYXBwLmNvbSJ9.dGVzdFNpZ25hdHVyZQ");
            var newSubject = Subject.of("new_user@asapp.com");
            var newAccessTokenClaims = JwtClaims.of(Map.of(TOKEN_USE, ACCESS_TOKEN_USE, ROLE, ADMIN.name()));
            var newRefreshTokenClaims = JwtClaims.of(Map.of(TOKEN_USE, REFRESH_TOKEN_USE, ROLE, ADMIN.name()));
            var newIssued = Issued.of(Instant.parse("2025-02-02T12:00:00Z"));
            var newEexpiration = Expiration.of(newIssued, 30000L);
            var newAccessToken = Jwt.of(newEncodedToken, ACCESS_TOKEN, newSubject, newAccessTokenClaims, newIssued, newEexpiration);
            var newRefreshToken = Jwt.of(newEncodedToken, REFRESH_TOKEN, newSubject, newRefreshTokenClaims, newIssued, newEexpiration);
            var newJwtPair = JwtPair.of(newAccessToken, newRefreshToken);

            // When
            jwtAuthentication.refreshTokens(newJwtPair);

            // Then
            assertThat(jwtAuthentication.accessToken()).isEqualTo(newAccessToken);
            assertThat(jwtAuthentication.refreshToken()).isEqualTo(newRefreshToken);
        }

        @Test
        void ThrowsIllegalArgumentException_NullJwtPairOnJwtAuthenticated() {
            // Given
            var jwtPair = JwtPair.of(accessToken, refreshToken);
            var jwtAuthentication = JwtAuthentication.authenticated(id, userId, jwtPair);

            // When
            var thrown = catchThrowable(() -> jwtAuthentication.refreshTokens(null));

            // Then
            assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                              .hasMessage("JWT pair must not be null");
        }

        @Test
        void RefreshesTokens_ValidJwtPairOnJwtAuthenticated() {
            // Given
            var jwtPair = JwtPair.of(accessToken, refreshToken);
            var jwtAuthentication = JwtAuthentication.authenticated(id, userId, jwtPair);

            var newEncodedToken = EncodedToken.of("eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyQGFzYXBwLmNvbSJ9.dGVzdFNpZ25hdHVyZQ");
            var newSubject = Subject.of("new_user@asapp.com");
            var newAccessTokenClaims = JwtClaims.of(Map.of(TOKEN_USE, ACCESS_TOKEN_USE, ROLE, ADMIN.name()));
            var newRefreshTokenClaims = JwtClaims.of(Map.of(TOKEN_USE, REFRESH_TOKEN_USE, ROLE, ADMIN.name()));
            var newIssued = Issued.of(Instant.parse("2025-02-02T12:00:00Z"));
            var newExpiration = Expiration.of(newIssued, 30000L);
            var newAccessToken = Jwt.of(newEncodedToken, ACCESS_TOKEN, newSubject, newAccessTokenClaims, newIssued, newExpiration);
            var newRefreshToken = Jwt.of(newEncodedToken, REFRESH_TOKEN, newSubject, newRefreshTokenClaims, newIssued, newExpiration);
            var newJwtPair = JwtPair.of(newAccessToken, newRefreshToken);

            // When
            jwtAuthentication.refreshTokens(newJwtPair);

            // Then
            assertThat(jwtAuthentication.accessToken()).isEqualTo(newAccessToken);
            assertThat(jwtAuthentication.refreshToken()).isEqualTo(newRefreshToken);
        }

    }

    @Nested
    class CheckEquality {

        @Test
        void ReturnsFalse_NullOtherJwtAuthentication() {
            // Given
            var jwtPair = JwtPair.of(accessToken, refreshToken);
            var jwtAuthentication = JwtAuthentication.authenticated(id, userId, jwtPair);

            // When
            var actual = jwtAuthentication.equals(null);

            // Then
            assertThat(actual).isFalse();
        }

        @Test
        void ReturnsFalse_OtherClassNotJwtAuthentication() {
            // Given
            var jwtPair = JwtPair.of(accessToken, refreshToken);
            var jwtAuthentication = JwtAuthentication.authenticated(id, userId, jwtPair);
            var other = "not a jwt authentication";

            // When
            var actual = jwtAuthentication.equals(other);

            // Then
            assertThat(actual).isFalse();
        }

        @Test
        void ReturnsTrue_SameObjectOtherJwtAuthentication() {
            // Given
            var jwtPair = JwtPair.of(accessToken, refreshToken);
            var jwtAuthentication = JwtAuthentication.authenticated(id, userId, jwtPair);

            // When
            var actual = jwtAuthentication.equals(jwtAuthentication);

            // Then
            assertThat(actual).isTrue();
        }

        @Test
        void ReturnsFalse_JwtUnauthenticatedAndJwtAuthenticated() {
            // Given
            var jwtPair = JwtPair.of(accessToken, refreshToken);
            var jwtAuthentication1 = JwtAuthentication.unAuthenticated(userId, jwtPair);
            var jwtAuthentication2 = JwtAuthentication.authenticated(id, userId, jwtPair);

            // When
            var actual1 = jwtAuthentication1.equals(jwtAuthentication2);
            var actual2 = jwtAuthentication2.equals(jwtAuthentication1);

            // Then
            assertThat(actual1).isFalse();
            assertThat(actual2).isFalse();
        }

        @Test
        void ReturnsTrue_ThreeJwtAuthenticationsSameId() {
            // Given
            var jwtPair = JwtPair.of(accessToken, refreshToken);
            var jwtAuthentication1 = JwtAuthentication.authenticated(id, userId, jwtPair);
            var jwtAuthentication2 = JwtAuthentication.authenticated(id, userId, jwtPair);
            var jwtAuthentication3 = JwtAuthentication.authenticated(id, userId, jwtPair);

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
        void ReturnsFalse_ThreeJwtAuthenticationsDifferentId() {
            // Given
            var jwtPair = JwtPair.of(accessToken, refreshToken);
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
        void ReturnsDifferentHashCode_TwoJwtUnauthenticated() {
            // Given
            var jwtPair = JwtPair.of(accessToken, refreshToken);
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
            var jwtPair = JwtPair.of(accessToken, refreshToken);
            var jwtAuthentication1 = JwtAuthentication.unAuthenticated(userId, jwtPair);
            var jwtAuthentication2 = JwtAuthentication.authenticated(id, userId, jwtPair);

            // When
            var actual1 = jwtAuthentication1.hashCode();
            var actual2 = jwtAuthentication2.hashCode();

            // Then
            assertThat(actual1).isNotEqualTo(actual2);
        }

        @Test
        void ReturnsSameHashCode_TwoJwtAuthenticatedSameId() {
            // Given
            var jwtPair = JwtPair.of(accessToken, refreshToken);
            var jwtAuthentication1 = JwtAuthentication.authenticated(id, userId, jwtPair);
            var jwtAuthentication2 = JwtAuthentication.authenticated(id, userId, jwtPair);

            // When
            var actual1 = jwtAuthentication1.hashCode();
            var actual2 = jwtAuthentication2.hashCode();

            // Then
            assertThat(actual1).isEqualTo(actual2);
        }

        @Test
        void ReturnsDifferentHashCode_TwoJwtAuthenticatedDifferentId() {
            // Given
            var jwtPair = JwtPair.of(accessToken, refreshToken);
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
        void ReturnsNull_JwtUnauthenticated() {
            // Given
            var jwtPair = JwtPair.of(accessToken, refreshToken);
            var jwtAuthentication = JwtAuthentication.unAuthenticated(userId, jwtPair);

            // When
            var actual = jwtAuthentication.getId();

            // Then
            assertThat(actual).isNull();
        }

        @Test
        void ReturnsId_JwtAuthenticated() {
            // Given
            var jwtPair = JwtPair.of(accessToken, refreshToken);
            var jwtAuthentication = JwtAuthentication.authenticated(id, userId, jwtPair);

            // When
            var actual = jwtAuthentication.getId();

            // Then
            assertThat(actual).isEqualTo(id);
        }

    }

    @Nested
    class GetUserId {

        @Test
        void ReturnsUserId_JwtUnauthenticated() {
            // Given
            var jwtPair = JwtPair.of(accessToken, refreshToken);
            var jwtAuthentication = JwtAuthentication.unAuthenticated(userId, jwtPair);

            // When
            var actual = jwtAuthentication.getUserId();

            // Then
            assertThat(actual).isEqualTo(userId);
        }

        @Test
        void ReturnsUserId_JwtAuthenticated() {
            // Given
            var jwtPair = JwtPair.of(accessToken, refreshToken);
            var jwtAuthentication = JwtAuthentication.authenticated(id, userId, jwtPair);

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
            var jwtPair = JwtPair.of(accessToken, refreshToken);
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
            var jwtPair = JwtPair.of(accessToken, refreshToken);
            var jwtAuthentication = JwtAuthentication.authenticated(id, userId, jwtPair);

            // When
            var actual = jwtAuthentication.getJwtPair();

            // Then
            assertThat(actual.accessToken()).isEqualTo(accessToken);
            assertThat(actual.refreshToken()).isEqualTo(refreshToken);
        }

    }

}

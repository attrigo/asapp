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

package com.bcn.asapp.authentication.infrastructure.security;

import static com.bcn.asapp.authentication.domain.authentication.JwtClaimNames.ACCESS_TOKEN_USE;
import static com.bcn.asapp.authentication.domain.authentication.JwtClaimNames.REFRESH_TOKEN_USE;
import static com.bcn.asapp.authentication.domain.authentication.JwtClaimNames.ROLE;
import static com.bcn.asapp.authentication.domain.authentication.JwtClaimNames.TOKEN_USE;
import static com.bcn.asapp.authentication.domain.authentication.JwtType.ACCESS_TOKEN;
import static com.bcn.asapp.authentication.domain.authentication.JwtType.REFRESH_TOKEN;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import io.jsonwebtoken.security.Keys;

import com.bcn.asapp.authentication.domain.authentication.JwtClaims;
import com.bcn.asapp.authentication.domain.authentication.Subject;
import com.bcn.asapp.authentication.domain.authentication.UserAuthentication;
import com.bcn.asapp.authentication.domain.user.Role;
import com.bcn.asapp.authentication.domain.user.UserId;
import com.bcn.asapp.authentication.domain.user.Username;

class JwtIssuerTests {

    private final UUID userIdValue = UUID.fromString("184c3f38-0783-4b4e-a570-709416bd856b");

    private final String usernameValue = "user@asapp.com";

    private JwtIssuer jwtIssuer;

    @BeforeEach
    void beforeEach() {
        var secretKey = Keys.hmacShaKeyFor(new byte[32]);
        var jwtSecret = Base64.getEncoder()
                              .encodeToString(secretKey.getEncoded());
        var accessTokenExpirationTime = 900000L; // 15 minutes
        var refreshTokenExpirationTime = 604800000L; // 7 days
        jwtIssuer = new JwtIssuer(jwtSecret, accessTokenExpirationTime, refreshTokenExpirationTime);
    }

    @Nested
    class IssueAccessTokenFromUserAuthentication {

        @ParameterizedTest
        @EnumSource(value = Role.class)
        void ThenReturnsAccessToken_GivenUserAuthentication(Role role) {
            // Given
            var userAuthentication = UserAuthentication.authenticated(UserId.of(userIdValue), Username.of(usernameValue), role);

            // When
            var actual = jwtIssuer.issueAccessToken(userAuthentication);

            // Then
            assertThat(actual).isNotNull();
            assertThat(actual.encodedToken()).isNotNull();
            assertThat(actual.type()).isEqualTo(ACCESS_TOKEN);
            assertThat(actual.subject()).extracting(Subject::value)
                                        .isEqualTo(usernameValue);
            assertThat(actual.claims()).extracting(JwtClaims::value)
                                       .isEqualTo(Map.of(TOKEN_USE, ACCESS_TOKEN_USE, ROLE, role.name()));
            assertThat(actual.issued()
                             .value()).isBefore(actual.expiration()
                                                      .value());
            assertThat(actual.isAccessToken()).isTrue();
            assertThat(actual.isRefreshToken()).isFalse();
        }

    }

    @Nested
    class IssueAccessTokenFromSubjectAndRole {

        @ParameterizedTest
        @EnumSource(value = Role.class)
        void ThenReturnsAccessToken_GivenSubjectAndRole(Role role) {
            // Given
            var subject = Subject.of(usernameValue);

            // When
            var actual = jwtIssuer.issueAccessToken(subject, role);

            // Then
            assertThat(actual).isNotNull();
            assertThat(actual.encodedToken()).isNotNull();
            assertThat(actual.type()).isEqualTo(ACCESS_TOKEN);
            assertThat(actual.subject()).extracting(Subject::value)
                                        .isEqualTo(usernameValue);
            assertThat(actual.claims()).extracting(JwtClaims::value)
                                       .isEqualTo(Map.of(TOKEN_USE, ACCESS_TOKEN_USE, ROLE, role.name()));
            assertThat(actual.issued()
                             .value()).isBefore(actual.expiration()
                                                      .value());
            assertThat(actual.isAccessToken()).isTrue();
            assertThat(actual.isRefreshToken()).isFalse();
        }

    }

    @Nested
    class IssueRefreshTokenFromUserAuthentication {

        @ParameterizedTest
        @EnumSource(value = Role.class)
        void ThenReturnsRefreshToken_GivenUserAuthentication(Role role) {
            // Given
            var userAuthentication = UserAuthentication.authenticated(UserId.of(userIdValue), Username.of(usernameValue), role);

            // When
            var actual = jwtIssuer.issueRefreshToken(userAuthentication);

            // Then
            assertThat(actual).isNotNull();
            assertThat(actual.encodedToken()).isNotNull();
            assertThat(actual.type()).isEqualTo(REFRESH_TOKEN);
            assertThat(actual.subject()).extracting(Subject::value)
                                        .isEqualTo(usernameValue);
            assertThat(actual.claims()).extracting(JwtClaims::value)
                                       .isEqualTo(Map.of(TOKEN_USE, REFRESH_TOKEN_USE, ROLE, role.name()));
            assertThat(actual.issued()
                             .value()).isBefore(actual.expiration()
                                                      .value());
            assertThat(actual.isAccessToken()).isFalse();
            assertThat(actual.isRefreshToken()).isTrue();
        }

    }

    @Nested
    class IssueRefreshTokenFromSubjectAndRole {

        @ParameterizedTest
        @EnumSource(value = Role.class)
        void ThenReturnsRefreshToken_GivenSubjectAndRole(Role role) {
            // Given
            var subject = Subject.of(usernameValue);

            // When
            var actual = jwtIssuer.issueRefreshToken(subject, role);

            // Then
            assertThat(actual).isNotNull();
            assertThat(actual.encodedToken()).isNotNull();
            assertThat(actual.type()).isEqualTo(REFRESH_TOKEN);
            assertThat(actual.subject()).extracting(Subject::value)
                                        .isEqualTo(usernameValue);
            assertThat(actual.claims()).extracting(JwtClaims::value)
                                       .isEqualTo(Map.of(TOKEN_USE, REFRESH_TOKEN_USE, ROLE, role.name()));
            assertThat(actual.issued()
                             .value()).isBefore(actual.expiration()
                                                      .value());
            assertThat(actual.isAccessToken()).isFalse();
            assertThat(actual.isRefreshToken()).isTrue();
        }

    }

}

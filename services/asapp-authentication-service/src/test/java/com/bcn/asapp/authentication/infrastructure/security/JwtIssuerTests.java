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
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.text.ParseException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.KeyLengthException;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.SignedJWT;

import com.bcn.asapp.authentication.domain.authentication.JwtClaims;
import com.bcn.asapp.authentication.domain.authentication.Subject;
import com.bcn.asapp.authentication.domain.authentication.UserAuthentication;
import com.bcn.asapp.authentication.domain.user.Role;
import com.bcn.asapp.authentication.domain.user.UserId;
import com.bcn.asapp.authentication.domain.user.Username;

/**
 * Tests {@link JwtIssuer} token generation, claim population, and cryptographic signing.
 * <p>
 * Coverage:
 * <li>Generates access and refresh tokens with distinct type headers and expiration times</li>
 * <li>Includes mandatory claims (subject, role, token_use) in both token types</li>
 * <li>Signs tokens with configured secret key for cryptographic verification</li>
 * <li>Supports all role types in claim generation</li>
 * <li>Selects HMAC algorithm (HS256/HS384/HS512) based on key bit-length</li>
 * <li>Throws JwtIssuanceException when cryptographic signing fails</li>
 */
class JwtIssuerTests {

    private static final long ACCESS_TOKEN_EXPIRATION_TIME = 900000L; // 15 minutes

    private static final long REFRESH_TOKEN_EXPIRATION_TIME = 604800000L; // 7 days

    private JwtIssuer jwtIssuer;

    @BeforeEach
    void beforeEach() throws KeyLengthException {
        var macSigner = new MACSigner(new byte[32]);
        jwtIssuer = new JwtIssuer(macSigner, ACCESS_TOKEN_EXPIRATION_TIME, REFRESH_TOKEN_EXPIRATION_TIME);
    }

    @Nested
    class IssueTokenPairFromUserAuthentication {

        @ParameterizedTest
        @EnumSource(value = Role.class)
        void ReturnsJwtPair_ValidUserAuthentication(Role role) {
            // Given
            var userIdValue = UUID.fromString("184c3f38-0783-4b4e-a570-709416bd856b");
            var userId = UserId.of(userIdValue);
            var usernameValue = "user@asapp.com";
            var username = Username.of(usernameValue);
            var userAuthentication = UserAuthentication.authenticated(userId, username, role);

            // When
            var actual = jwtIssuer.issueTokenPair(userAuthentication);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual).as("JWT pair").isNotNull();
                softly.assertThat(actual.accessToken()).as("access token").isNotNull();
                softly.assertThat(actual.refreshToken()).as("refresh token").isNotNull();
                softly.assertThat(actual.accessToken().type()).as("access token type").isEqualTo(ACCESS_TOKEN);
                softly.assertThat(actual.accessToken().subject()).as("access token subject").extracting(Subject::value).isEqualTo(usernameValue);
                softly.assertThat(actual.accessToken().claims()).as("access token claims").extracting(JwtClaims::value).isEqualTo(Map.of(TOKEN_USE, ACCESS_TOKEN_USE, ROLE, role.name()));
                softly.assertThat(actual.accessToken().isAccessToken()).as("is access token").isTrue();
                softly.assertThat(actual.refreshToken().type()).as("refresh token type").isEqualTo(REFRESH_TOKEN);
                softly.assertThat(actual.refreshToken().subject()).as("refresh token subject").extracting(Subject::value).isEqualTo(usernameValue);
                softly.assertThat(actual.refreshToken().claims()).as("refresh token claims").extracting(JwtClaims::value).isEqualTo(Map.of(TOKEN_USE, REFRESH_TOKEN_USE, ROLE, role.name()));
                softly.assertThat(actual.refreshToken().isRefreshToken()).as("is refresh token").isTrue();
                // @formatter:on
            });
        }

        @ParameterizedTest
        @MethodSource("com.bcn.asapp.authentication.infrastructure.security.JwtIssuerTests#keySizeToAlgorithm")
        void ReturnsTokenSignedWithMatchingAlgorithm_KeyBitLength(int keySize, JWSAlgorithm algorithm) throws KeyLengthException {
            // Given
            var issuer = new JwtIssuer(new MACSigner(new byte[keySize]), ACCESS_TOKEN_EXPIRATION_TIME, REFRESH_TOKEN_EXPIRATION_TIME);
            var userId = UserId.of(UUID.fromString("184c3f38-0783-4b4e-a570-709416bd856b"));
            var username = Username.of("user@asapp.com");
            var userAuthentication = UserAuthentication.authenticated(userId, username, Role.USER);

            // When
            var actual = issuer.issueTokenPair(userAuthentication);

            // Then
            assertThat(algorithmOf(actual.accessToken()
                                         .encodedTokenValue())).isEqualTo(algorithm);
        }

        @Test
        void ThrowsJwtIssuanceException_SigningFails() throws JOSEException {
            // Given
            var macSigner = mock(MACSigner.class);
            var issuer = new JwtIssuer(macSigner, ACCESS_TOKEN_EXPIRATION_TIME, REFRESH_TOKEN_EXPIRATION_TIME);
            var userId = UserId.of(UUID.fromString("184c3f38-0783-4b4e-a570-709416bd856b"));
            var username = Username.of("user@asapp.com");
            var userAuthentication = UserAuthentication.authenticated(userId, username, Role.USER);

            given(macSigner.getSecret()).willReturn(new byte[32]);
            given(macSigner.supportedJWSAlgorithms()).willReturn(Set.of(JWSAlgorithm.HS256));
            given(macSigner.sign(any(), any())).willThrow(new JOSEException("signing failed"));

            // When
            var actual = catchThrowable(() -> issuer.issueTokenPair(userAuthentication));

            // Then
            assertThat(actual).isInstanceOf(JwtIssuanceException.class)
                              .hasMessageContaining("JWT signing failed for type");
        }

    }

    @Nested
    class IssueTokenPairFromSubjectAndRole {

        @ParameterizedTest
        @EnumSource(value = Role.class)
        void ReturnsJwtPair_ValidSubjectAndRole(Role role) {
            // Given
            var subjectValue = "user@asapp.com";
            var subject = Subject.of(subjectValue);

            // When
            var actual = jwtIssuer.issueTokenPair(subject, role);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual).as("JWT pair").isNotNull();
                softly.assertThat(actual.accessToken()).as("access token").isNotNull();
                softly.assertThat(actual.refreshToken()).as("refresh token").isNotNull();
                softly.assertThat(actual.accessToken().type()).as("access token type").isEqualTo(ACCESS_TOKEN);
                softly.assertThat(actual.accessToken().subject()).as("access token subject").extracting(Subject::value).isEqualTo(subjectValue);
                softly.assertThat(actual.accessToken().claims()).as("access token claims").extracting(JwtClaims::value).isEqualTo(Map.of(TOKEN_USE, ACCESS_TOKEN_USE, ROLE, role.name()));
                softly.assertThat(actual.accessToken().isAccessToken()).as("is access token").isTrue();
                softly.assertThat(actual.refreshToken().type()).as("refresh token type").isEqualTo(REFRESH_TOKEN);
                softly.assertThat(actual.refreshToken().subject()).as("refresh token subject").extracting(Subject::value).isEqualTo(subjectValue);
                softly.assertThat(actual.refreshToken().claims()).as("refresh token claims").extracting(JwtClaims::value).isEqualTo(Map.of(TOKEN_USE, REFRESH_TOKEN_USE, ROLE, role.name()));
                softly.assertThat(actual.refreshToken().isRefreshToken()).as("is refresh token").isTrue();
                // @formatter:on
            });
        }

        @ParameterizedTest
        @MethodSource("com.bcn.asapp.authentication.infrastructure.security.JwtIssuerTests#keySizeToAlgorithm")
        void ReturnsTokenSignedWithMatchingAlgorithm_KeyBitLength(int keySize, JWSAlgorithm algorithm) throws KeyLengthException {
            // Given
            var issuer = new JwtIssuer(new MACSigner(new byte[keySize]), ACCESS_TOKEN_EXPIRATION_TIME, REFRESH_TOKEN_EXPIRATION_TIME);
            var subject = Subject.of("user@asapp.com");

            // When
            var actual = issuer.issueTokenPair(subject, Role.USER);

            // Then
            assertThat(algorithmOf(actual.accessToken()
                                         .encodedTokenValue())).isEqualTo(algorithm);
        }

        @Test
        void ThrowsJwtIssuanceException_SigningFails() throws JOSEException {
            // Given
            var macSigner = mock(MACSigner.class);
            var issuer = new JwtIssuer(macSigner, ACCESS_TOKEN_EXPIRATION_TIME, REFRESH_TOKEN_EXPIRATION_TIME);
            var subject = Subject.of("user@asapp.com");

            given(macSigner.getSecret()).willReturn(new byte[32]);
            given(macSigner.supportedJWSAlgorithms()).willReturn(Set.of(JWSAlgorithm.HS256));
            given(macSigner.sign(any(), any())).willThrow(new JOSEException("signing failed"));

            // When
            var actual = catchThrowable(() -> issuer.issueTokenPair(subject, Role.USER));

            // Then
            assertThat(actual).isInstanceOf(JwtIssuanceException.class)
                              .hasMessageContaining("JWT signing failed for type");
        }

    }

    static Stream<Arguments> keySizeToAlgorithm() {
        return Stream.of(Arguments.of(32, JWSAlgorithm.HS256), Arguments.of(48, JWSAlgorithm.HS384), Arguments.of(64, JWSAlgorithm.HS512));
    }

    static JWSAlgorithm algorithmOf(String encodedToken) {
        try {
            return SignedJWT.parse(encodedToken)
                            .getHeader()
                            .getAlgorithm();
        } catch (ParseException e) {
            throw new AssertionError("Failed to parse JWT", e);
        }
    }

}

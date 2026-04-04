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

package com.bcn.asapp.authentication.testutil;

import static com.bcn.asapp.authentication.domain.authentication.JwtClaimNames.ACCESS_TOKEN_USE;
import static com.bcn.asapp.authentication.domain.authentication.JwtClaimNames.REFRESH_TOKEN_USE;
import static com.bcn.asapp.authentication.domain.authentication.JwtClaimNames.TOKEN_USE;
import static com.bcn.asapp.authentication.domain.authentication.JwtTypeNames.ACCESS_TOKEN_TYPE;
import static com.bcn.asapp.authentication.domain.authentication.JwtTypeNames.REFRESH_TOKEN_TYPE;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Base64;
import java.util.Properties;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.SoftAssertions;
import org.springframework.util.Assert;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.SignedJWT;

/**
 * Provides custom AssertJ assertions for JWT token validation with fluent API.
 *
 * @since 0.2.0
 */
public class JwtAssertions extends AbstractAssert<JwtAssertions, SignedJWT> {

    private static final String JWT_SECRET;

    static {
        try (InputStream input = JwtAssertions.class.getClassLoader()
                                                    .getResourceAsStream("application.properties")) {
            if (input == null) {
                throw new IllegalStateException("application.properties not found in classpath");
            }
            Properties props = new Properties();
            props.load(input);
            JWT_SECRET = props.getProperty("asapp.security.jwt-secret");
            Assert.hasText(JWT_SECRET, "asapp.security.jwt-secret not found or empty in application.properties");
        } catch (IOException e) {
            throw new IllegalStateException("Could not load JWT secret from properties", e);
        }
    }

    JwtAssertions(SignedJWT actual) {
        super(actual, JwtAssertions.class);
    }

    public static JwtAssertions assertThatJwt(String actualJwt) {
        try {
            var secretBytes = Base64.getDecoder().decode(JWT_SECRET);
            var signedJwt = SignedJWT.parse(actualJwt);
            if (!signedJwt.verify(new MACVerifier(secretBytes))) {
                throw new AssertionError("JWT signature verification failed for token: " + actualJwt);
            }
            return new JwtAssertions(signedJwt);
        } catch (ParseException | JOSEException e) {
            throw new AssertionError("Failed to parse or verify JWT: " + actualJwt, e);
        }
    }

    public JwtAssertions isAccessToken() {
        isNotNull();
        try {
            var actualHeaderType = actual.getHeader().getType().getType();
            var actualTokenUseClaim = actual.getJWTClaimsSet().getClaim(TOKEN_USE);
            SoftAssertions.assertSoftly(softAssertions -> {
                Assertions.assertThat(actualHeaderType)
                          .isNotNull()
                          .describedAs("type")
                          .isEqualTo(ACCESS_TOKEN_TYPE);
                Assertions.assertThat(actualTokenUseClaim)
                          .isNotNull()
                          .describedAs("token use claim")
                          .isEqualTo(ACCESS_TOKEN_USE);
            });
        } catch (ParseException e) {
            throw new AssertionError("Failed to extract JWT claims", e);
        }
        return myself;
    }

    public JwtAssertions isRefreshToken() {
        isNotNull();
        try {
            var actualHeaderType = actual.getHeader().getType().getType();
            var actualTokenUseClaim = actual.getJWTClaimsSet().getClaim(TOKEN_USE);
            SoftAssertions.assertSoftly(softAssertions -> {
                Assertions.assertThat(actualHeaderType)
                          .isNotNull()
                          .describedAs("type")
                          .isEqualTo(REFRESH_TOKEN_TYPE);
                Assertions.assertThat(actualTokenUseClaim)
                          .isNotNull()
                          .describedAs("token use claim")
                          .isEqualTo(REFRESH_TOKEN_USE);
            });
        } catch (ParseException e) {
            throw new AssertionError("Failed to extract JWT claims", e);
        }
        return myself;
    }

    public JwtAssertions hasSubject(String expectedSubject) {
        isNotNull();
        try {
            var actualSubject = actual.getJWTClaimsSet().getSubject();
            Assertions.assertThat(actualSubject)
                      .isNotNull()
                      .describedAs("subject")
                      .isEqualTo(expectedSubject);
        } catch (ParseException e) {
            throw new AssertionError("Failed to extract JWT claims", e);
        }
        return myself;
    }

    public JwtAssertions hasClaim(String expectedClaimName, Object expectedClaimValue, Class<?> expectedClaimValueType) {
        isNotNull();
        try {
            var actualClaimValue = actual.getJWTClaimsSet().getClaim(expectedClaimName);
            Assertions.assertThat(actualClaimValue)
                      .isNotNull()
                      .describedAs("claim")
                      .asInstanceOf(InstanceOfAssertFactories.type(expectedClaimValueType))
                      .isEqualTo(expectedClaimValue);
        } catch (ParseException e) {
            throw new AssertionError("Failed to extract JWT claims", e);
        }
        return myself;
    }

    public JwtAssertions hasIssuedAt() {
        isNotNull();
        try {
            var actualIssuedAt = actual.getJWTClaimsSet().getIssueTime();
            Assertions.assertThat(actualIssuedAt)
                      .describedAs("issued")
                      .isNotNull();
        } catch (ParseException e) {
            throw new AssertionError("Failed to extract JWT claims", e);
        }
        return myself;
    }

    public JwtAssertions hasExpiration() {
        isNotNull();
        try {
            var actualExpiration = actual.getJWTClaimsSet().getExpirationTime();
            Assertions.assertThat(actualExpiration)
                      .describedAs("expiration")
                      .isNotNull();
        } catch (ParseException e) {
            throw new AssertionError("Failed to extract JWT claims", e);
        }
        return this;
    }

}

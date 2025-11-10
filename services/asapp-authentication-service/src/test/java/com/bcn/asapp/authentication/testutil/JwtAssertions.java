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
import java.util.Properties;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.SoftAssertions;
import org.springframework.util.Assert;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

public class JwtAssertions extends AbstractAssert<JwtAssertions, Jws<Claims>> {

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

    JwtAssertions(Jws<Claims> actual) {
        super(actual, JwtAssertions.class);
    }

    public static JwtAssertions assertThatJwt(String actualJwt) {
        var jws = Jwts.parser()
                      .verifyWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(JWT_SECRET)))
                      .build()
                      .parseSignedClaims(actualJwt);

        return new JwtAssertions(jws);
    }

    public JwtAssertions isAccessToken() {
        isNotNull();
        hasHeader();
        hasPayload();

        var actualHeaderType = actual.getHeader()
                                     .getType();
        var actualTokenUseClaim = actual.getPayload()
                                        .get(TOKEN_USE);
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

        return myself;
    }

    public JwtAssertions isRefreshToken() {
        isNotNull();
        hasHeader();
        hasPayload();

        var actualHeaderType = actual.getHeader()
                                     .getType();
        var actualTokenUseClaim = actual.getPayload()
                                        .get(TOKEN_USE);
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
        return myself;
    }

    public JwtAssertions hasSubject(String expectedSubject) {
        isNotNull();
        hasPayload();

        var actualSubject = actual.getPayload()
                                  .getSubject();
        Assertions.assertThat(actualSubject)
                  .isNotNull()
                  .describedAs("subject")
                  .isEqualTo(expectedSubject);

        return myself;
    }

    public JwtAssertions hasClaim(String expectedClaimName, Object expectedClaimValue, Class<?> expectedClaimValueType) {
        isNotNull();
        hasPayload();

        var actualClaimValue = actual.getPayload()
                                     .get(expectedClaimName, expectedClaimValueType);
        Assertions.assertThat(actualClaimValue)
                  .isNotNull()
                  .describedAs("claim")
                  .asInstanceOf(InstanceOfAssertFactories.type(expectedClaimValueType))
                  .isEqualTo(expectedClaimValue);

        return myself;
    }

    public JwtAssertions hasIssuedAt() {
        isNotNull();
        hasPayload();

        var actualIssuedAt = actual.getPayload()
                                   .getIssuedAt();
        Assertions.assertThat(actualIssuedAt)
                  .describedAs("issued")
                  .isNotNull();

        return myself;
    }

    public JwtAssertions hasExpiration() {
        isNotNull();
        hasPayload();

        var actualExpiration = actual.getPayload()
                                     .getExpiration();
        Assertions.assertThat(actualExpiration)
                  .describedAs("expiration")
                  .isNotNull();

        return this;
    }

    private void hasHeader() {
        var actualHeader = actual.getHeader();
        Assertions.assertThat(actualHeader)
                  .describedAs("header")
                  .isNotNull();
    }

    private void hasPayload() {
        var actualPayload = actual.getPayload();
        Assertions.assertThat(actualPayload)
                  .describedAs("payload")
                  .isNotNull();
    }

}

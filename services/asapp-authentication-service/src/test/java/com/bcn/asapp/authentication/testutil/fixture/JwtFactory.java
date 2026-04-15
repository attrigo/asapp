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

package com.bcn.asapp.authentication.testutil.fixture;

import static com.bcn.asapp.authentication.domain.authentication.JwtType.ACCESS_TOKEN;
import static com.bcn.asapp.authentication.domain.authentication.JwtType.REFRESH_TOKEN;
import static com.bcn.asapp.authentication.testutil.fixture.TestFactoryConstants.DEFAULT_ACCESS_TOKEN_CLAIMS;
import static com.bcn.asapp.authentication.testutil.fixture.TestFactoryConstants.DEFAULT_REFRESH_TOKEN_CLAIMS;
import static com.bcn.asapp.authentication.testutil.fixture.TestFactoryConstants.DEFAULT_SUBJECT;
import static com.bcn.asapp.authentication.testutil.fixture.TestFactoryConstants.EXPIRATION_TIME_MILLIS;
import static com.bcn.asapp.authentication.testutil.fixture.TestFactoryConstants.TOKEN_EXPIRED_OFFSET_MILLIS;
import static com.bcn.asapp.authentication.testutil.fixture.TestFactoryConstants.generateRandomIssueAt;

import java.time.Instant;
import java.util.Map;

import com.bcn.asapp.authentication.domain.authentication.EncodedToken;
import com.bcn.asapp.authentication.domain.authentication.Expiration;
import com.bcn.asapp.authentication.domain.authentication.Issued;
import com.bcn.asapp.authentication.domain.authentication.Jwt;
import com.bcn.asapp.authentication.domain.authentication.JwtClaims;
import com.bcn.asapp.authentication.domain.authentication.JwtType;
import com.bcn.asapp.authentication.domain.authentication.Subject;

/**
 * Provides test data builders for Jwt domain value objects with fluent API.
 * <p>
 * This factory simplifies creation of {@link Jwt} instances in tests, avoiding verbose inline construction. Supports both default pre-configured tokens and
 * customizable builders.
 *
 * @since 0.2.0
 */
public final class JwtFactory {

    private JwtFactory() {}

    public static Jwt anAccessToken() {
        return aJwtBuilder().accessToken()
                            .build();
    }

    public static Jwt aRefreshToken() {
        return aJwtBuilder().refreshToken()
                            .build();
    }

    public static Builder aJwtBuilder() {
        return new Builder();
    }

    public static class Builder {

        private String encodedToken;

        private JwtType type;

        private String subject;

        private Map<String, Object> claims;

        private Instant issued;

        private Instant expiration;

        Builder() {
            this.subject = DEFAULT_SUBJECT;
            this.issued = generateRandomIssueAt();
            this.expiration = issued.plusMillis(EXPIRATION_TIME_MILLIS);
        }

        public Builder accessToken() {
            this.type = ACCESS_TOKEN;
            this.claims = DEFAULT_ACCESS_TOKEN_CLAIMS;
            return this;
        }

        public Builder refreshToken() {
            this.type = REFRESH_TOKEN;
            this.claims = DEFAULT_REFRESH_TOKEN_CLAIMS;
            return this;
        }

        public Builder withEncodedToken(String encodedToken) {
            this.encodedToken = encodedToken;
            return this;
        }

        public Builder withType(JwtType type) {
            this.type = type;
            return this;
        }

        public Builder withSubject(String subject) {
            this.subject = subject;
            return this;
        }

        public Builder withClaims(Map<String, Object> claims) {
            this.claims = claims;
            return this;
        }

        public Builder withIssued(Instant issued) {
            this.issued = issued;
            return this;
        }

        public Builder withExpiration(Instant expiration) {
            this.expiration = expiration;
            return this;
        }

        public Builder expired() {
            var now = Instant.now();
            this.issued = now.minusMillis(EXPIRATION_TIME_MILLIS + TOKEN_EXPIRED_OFFSET_MILLIS);
            this.expiration = now.minusMillis(TOKEN_EXPIRED_OFFSET_MILLIS);
            return this;
        }

        public Jwt build() {
            if (encodedToken == null) {
                encodedToken = EncodedTokenFactory.anEncodedTokenBuilder()
                                                  .withType(type.type())
                                                  .withSubject(subject)
                                                  .withClaims(claims)
                                                  .withIssuedAt(issued)
                                                  .withExpiration(expiration)
                                                  .build();
            }

            var encodedTokenVO = EncodedToken.of(encodedToken);
            var subjectVO = Subject.of(subject);
            var claimsVO = JwtClaims.of(claims);
            var issuedVO = Issued.of(issued);
            var expirationVO = new Expiration(expiration);

            return Jwt.of(encodedTokenVO, type, subjectVO, claimsVO, issuedVO, expirationVO);
        }

    }

}

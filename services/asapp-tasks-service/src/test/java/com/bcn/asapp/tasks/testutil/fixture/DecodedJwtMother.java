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

package com.bcn.asapp.tasks.testutil.fixture;

import static com.bcn.asapp.tasks.infrastructure.security.JwtTypeNames.ACCESS_TOKEN_TYPE;
import static com.bcn.asapp.tasks.infrastructure.security.JwtTypeNames.REFRESH_TOKEN_TYPE;
import static com.bcn.asapp.tasks.testutil.fixture.EncodedTokenMother.anEncodedTokenBuilder;
import static com.bcn.asapp.tasks.testutil.fixture.TestFactoryConstants.DEFAULT_ACCESS_TOKEN_CLAIMS;
import static com.bcn.asapp.tasks.testutil.fixture.TestFactoryConstants.DEFAULT_REFRESH_TOKEN_CLAIMS;
import static com.bcn.asapp.tasks.testutil.fixture.TestFactoryConstants.DEFAULT_SUBJECT;

import java.util.Map;

import com.bcn.asapp.tasks.infrastructure.security.DecodedJwt;

/**
 * Provides test data builders for {@link DecodedJwt} instances with fluent API.
 *
 * @since 0.3.0
 */
public final class DecodedJwtMother {

    private DecodedJwtMother() {}

    public static DecodedJwt decodedAccessToken() {
        return aDecodedJwtBuilder().accessToken()
                                   .build();
    }

    public static DecodedJwt decodedRefreshToken() {
        return aDecodedJwtBuilder().refreshToken()
                                   .build();
    }

    public static Builder aDecodedJwtBuilder() {
        return new Builder();
    }

    public static class Builder {

        private String type;

        private String subject;

        private Map<String, Object> claims;

        Builder() {
            this.type = ACCESS_TOKEN_TYPE;
            this.subject = DEFAULT_SUBJECT;
            this.claims = DEFAULT_ACCESS_TOKEN_CLAIMS;
        }

        public Builder accessToken() {
            return withType(ACCESS_TOKEN_TYPE).withClaims(DEFAULT_ACCESS_TOKEN_CLAIMS);
        }

        public Builder refreshToken() {
            return withType(REFRESH_TOKEN_TYPE).withClaims(DEFAULT_REFRESH_TOKEN_CLAIMS);
        }

        public Builder withType(String type) {
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

        public DecodedJwt build() {
            var encodedToken = anEncodedTokenBuilder().withType(type)
                                                      .withSubject(subject)
                                                      .withClaims(claims)
                                                      .build();
            return new DecodedJwt(encodedToken, type, subject, claims);
        }

    }

}

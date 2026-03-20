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

package com.bcn.asapp.users.testutil.fixture;

import static com.bcn.asapp.users.infrastructure.security.JwtClaimNames.ROLE;
import static com.bcn.asapp.users.infrastructure.security.JwtTypeNames.ACCESS_TOKEN_TYPE;
import static com.bcn.asapp.users.infrastructure.security.JwtTypeNames.REFRESH_TOKEN_TYPE;
import static com.bcn.asapp.users.testutil.fixture.TestFactoryConstants.DEFAULT_ACCESS_TOKEN_CLAIMS;
import static com.bcn.asapp.users.testutil.fixture.TestFactoryConstants.DEFAULT_REFRESH_TOKEN_CLAIMS;
import static com.bcn.asapp.users.testutil.fixture.TestFactoryConstants.DEFAULT_SUBJECT;

import java.util.HashMap;
import java.util.Map;

import com.bcn.asapp.users.infrastructure.security.DecodedJwt;

/**
 * Provides test data builders for DecodedJwt with fluent API.
 *
 * @since 0.2.0
 */
public final class DecodedJwtFactory {

    private DecodedJwtFactory() {}

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

        private String encodedToken;

        private String type;

        private String subject;

        private Map<String, Object> claims;

        Builder() {
            this.encodedToken = EncodedTokenFactory.encodedAccessToken();
            this.type = ACCESS_TOKEN_TYPE;
            this.subject = DEFAULT_SUBJECT;
            this.claims = DEFAULT_ACCESS_TOKEN_CLAIMS;
        }

        public Builder accessToken() {
            this.encodedToken = EncodedTokenFactory.encodedAccessToken();
            this.type = ACCESS_TOKEN_TYPE;
            this.claims = DEFAULT_ACCESS_TOKEN_CLAIMS;
            return this;
        }

        public Builder refreshToken() {
            this.encodedToken = EncodedTokenFactory.encodedRefreshToken();
            this.type = REFRESH_TOKEN_TYPE;
            this.claims = DEFAULT_REFRESH_TOKEN_CLAIMS;
            return this;
        }

        public Builder withEncodedToken(String encodedToken) {
            this.encodedToken = encodedToken;
            return this;
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

        public Builder withRole(String role) {
            this.claims = new HashMap<>(this.claims);
            this.claims.put(ROLE, role);
            return this;
        }

        public Builder withoutRoleClaim() {
            this.claims = new HashMap<>(this.claims);
            this.claims.remove(ROLE);
            return this;
        }

        public DecodedJwt build() {
            return new DecodedJwt(encodedToken, type, subject, claims);
        }

    }

}

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

import com.bcn.asapp.authentication.domain.authentication.Jwt;
import com.bcn.asapp.authentication.domain.authentication.JwtPair;

/**
 * Provides test data builders for JwtPair domain value objects with fluent API.
 * <p>
 * This factory simplifies creation of {@link JwtPair} instances in tests, avoiding verbose inline construction. Supports both default pre-configured token
 * pairs and customizable builders.
 * <p>
 * Note: Wither methods accept {@link Jwt} value objects instead of primitives. This is an intentional trade-off where factory composition (delegating to
 * {@link JwtMother}), avoiding duplication of all JwtFactory builder fields.
 *
 * @since 0.2.0
 */
public final class JwtPairMother {

    private JwtPairMother() {}

    public static JwtPair aJwtPair() {
        return aJwtPairBuilder().build();
    }

    public static Builder aJwtPairBuilder() {
        return new Builder();
    }

    public static class Builder {

        private Jwt accessToken;

        private Jwt refreshToken;

        Builder() {
            this.accessToken = JwtMother.anAccessToken();
            this.refreshToken = JwtMother.aRefreshToken();
        }

        public Builder withTokens(Jwt accessToken, Jwt refreshToken) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            return this;
        }

        public Builder withAccessToken(Jwt accessToken) {
            this.accessToken = accessToken;
            return this;
        }

        public Builder withRefreshToken(Jwt refreshToken) {
            this.refreshToken = refreshToken;
            return this;
        }

        public Builder expired() {
            this.accessToken = JwtMother.aJwtBuilder()
                                        .accessToken()
                                        .expired()
                                        .build();
            this.refreshToken = JwtMother.aJwtBuilder()
                                         .refreshToken()
                                         .expired()
                                         .build();
            return this;
        }

        public JwtPair build() {
            return JwtPair.of(accessToken, refreshToken);
        }

    }

}

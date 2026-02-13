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

import static com.bcn.asapp.authentication.testutil.TestFactoryConstants.DEFAULT_ROLE;
import static com.bcn.asapp.authentication.testutil.TestFactoryConstants.DEFAULT_USERNAME;

import java.util.UUID;

import com.bcn.asapp.authentication.domain.authentication.UserAuthentication;
import com.bcn.asapp.authentication.domain.user.Role;
import com.bcn.asapp.authentication.domain.user.UserId;
import com.bcn.asapp.authentication.domain.user.Username;

/**
 * Provides test data builders for UserAuthentication domain entities with fluent API.
 * <p>
 * UserAuthentication represents in-memory authentication state (not persisted entities). Useful for mocking authentication results in application service
 * tests.
 *
 * @since 0.2.0
 */
public final class UserAuthenticationFactory {

    private UserAuthenticationFactory() {}

    public static UserAuthentication anAuthenticatedUser() {
        return aUserAuthenticationBuilder().build();
    }

    public static UserAuthentication anAuthenticatedUser(UUID userId, String username, Role role) {
        return aUserAuthenticationBuilder().withUserId(userId)
                                           .withUsername(username)
                                           .withRole(role)
                                           .build();
    }

    public static Builder aUserAuthenticationBuilder() {
        return new Builder();
    }

    public static class Builder {

        private UUID userId;

        private String username;

        private Role role;

        Builder() {
            this.userId = UUID.randomUUID();
            this.username = DEFAULT_USERNAME;
            this.role = Role.valueOf(DEFAULT_ROLE);
        }

        public Builder withUserId(UUID userId) {
            this.userId = userId;
            return this;
        }

        public Builder withUsername(String username) {
            this.username = username;
            return this;
        }

        public Builder withRole(Role role) {
            this.role = role;
            return this;
        }

        public UserAuthentication build() {
            var userIdVO = UserId.of(userId);
            var usernameVO = Username.of(username);
            return UserAuthentication.authenticated(userIdVO, usernameVO, role);
        }

    }

}

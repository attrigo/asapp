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

package com.bcn.asapp.authentication.testutil.fixture;

import static com.bcn.asapp.authentication.testutil.fixture.TestFactoryConstants.DEFAULT_PASSWORD;
import static com.bcn.asapp.authentication.testutil.fixture.TestFactoryConstants.DEFAULT_ROLE;
import static com.bcn.asapp.authentication.testutil.fixture.TestFactoryConstants.DEFAULT_USERNAME;
import static com.bcn.asapp.authentication.testutil.fixture.TestFactoryConstants.DEFAULT_USER_ID;

import java.util.UUID;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.bcn.asapp.authentication.domain.user.EncodedPassword;
import com.bcn.asapp.authentication.domain.user.Role;
import com.bcn.asapp.authentication.domain.user.User;
import com.bcn.asapp.authentication.domain.user.UserId;
import com.bcn.asapp.authentication.domain.user.Username;
import com.bcn.asapp.authentication.infrastructure.user.persistence.JdbcUserEntity;

/**
 * Provides test data builders for User domain entities and JdbcUserEntity instances with fluent API.
 *
 * @since 0.2.0
 */
public final class UserFactory {

    private UserFactory() {}

    public static User anActiveUser() {
        return aUserBuilder().active()
                             .build();
    }

    public static User anInactiveUser() {
        return aUserBuilder().inactive()
                             .build();
    }

    public static User anAdminUser() {
        return aUserBuilder().active()
                             .asAdmin()
                             .build();
    }

    public static User aUser() {
        return aUserBuilder().build();
    }

    public static JdbcUserEntity aJdbcUser() {
        return aUserBuilder().buildJdbc();
    }

    public static Builder aUserBuilder() {
        return new Builder();
    }

    public static class Builder {

        private UUID userId;

        private String username;

        private String password;

        private Role role;

        private String passwordEncoderPrefix;

        private PasswordEncoder passwordEncoder;

        Builder() {
            this.userId = DEFAULT_USER_ID;
            this.username = DEFAULT_USERNAME;
            this.password = DEFAULT_PASSWORD;
            this.role = Role.valueOf(DEFAULT_ROLE);
            this.passwordEncoderPrefix = "{bcrypt}";
            this.passwordEncoder = new BCryptPasswordEncoder();
        }

        public Builder withUserId(UUID userId) {
            this.userId = userId;
            return this;
        }

        public Builder withUsername(String username) {
            this.username = username;
            return this;
        }

        public Builder withPassword(String password) {
            this.password = password;
            return this;
        }

        public Builder withRole(Role role) {
            this.role = role;
            return this;
        }

        /**
         * Some IT requires varying the encoding algorithm itself (argon2, scrypt, pbkdf2, noop), so the encoder is the thing under test.
         */
        public Builder withPasswordEncoder(String passwordEncoderPrefix, PasswordEncoder passwordEncoder) {
            this.passwordEncoderPrefix = passwordEncoderPrefix;
            this.passwordEncoder = passwordEncoder;
            return this;
        }

        public Builder inactive() {
            this.userId = null;
            return this;
        }

        public Builder active() {
            this.userId = DEFAULT_USER_ID;
            return this;
        }

        public Builder asAdmin() {
            this.role = Role.ADMIN;
            return this;
        }

        public User build() {
            var usernameVO = Username.of(username);

            if (userId == null) {
                var encodedPasswordVO = EncodedPassword.of(passwordEncoderPrefix + passwordEncoder.encode(password));
                return User.inactiveUser(usernameVO, encodedPasswordVO, role);
            } else {
                var userIdVO = UserId.of(userId);
                return User.activeUser(userIdVO, usernameVO, role);
            }
        }

        public JdbcUserEntity buildJdbc() {
            var encodedPasswordEntity = passwordEncoderPrefix + passwordEncoder.encode(password);
            return new JdbcUserEntity(null, username, encodedPasswordEntity, role.name());
        }

    }

}

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

package com.bcn.asapp.users.testutil.fixture;

import static com.bcn.asapp.users.testutil.fixture.TestFactoryConstants.DEFAULT_EMAIL;
import static com.bcn.asapp.users.testutil.fixture.TestFactoryConstants.DEFAULT_FIRST_NAME;
import static com.bcn.asapp.users.testutil.fixture.TestFactoryConstants.DEFAULT_LAST_NAME;
import static com.bcn.asapp.users.testutil.fixture.TestFactoryConstants.DEFAULT_PHONE_NUMBER;
import static com.bcn.asapp.users.testutil.fixture.TestFactoryConstants.DEFAULT_USER_ID;

import java.util.UUID;

import com.bcn.asapp.users.domain.user.Email;
import com.bcn.asapp.users.domain.user.FirstName;
import com.bcn.asapp.users.domain.user.LastName;
import com.bcn.asapp.users.domain.user.PhoneNumber;
import com.bcn.asapp.users.domain.user.User;
import com.bcn.asapp.users.domain.user.UserId;
import com.bcn.asapp.users.infrastructure.user.persistence.JdbcUserEntity;

/**
 * Provides test data builders for User domain entities and JdbcUserEntity instances with fluent API.
 *
 * @since 0.2.0
 */
public final class UserMother {

    private UserMother() {}

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

        private String firstName;

        private String lastName;

        private String email;

        private String phoneNumber;

        Builder() {
            this.userId = DEFAULT_USER_ID;
            this.firstName = DEFAULT_FIRST_NAME;
            this.lastName = DEFAULT_LAST_NAME;
            this.email = DEFAULT_EMAIL;
            this.phoneNumber = DEFAULT_PHONE_NUMBER;
        }

        public Builder withUserId(UUID userId) {
            this.userId = userId;
            return this;
        }

        public Builder withFirstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public Builder withLastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public Builder withEmail(String email) {
            this.email = email;
            return this;
        }

        public Builder withPhoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
            return this;
        }

        public User build() {
            var firstNameVO = FirstName.of(firstName);
            var lastNameVO = LastName.of(lastName);
            var emailVO = Email.of(email);
            var phoneNumberVO = PhoneNumber.of(phoneNumber);

            if (userId == null) {
                return User.create(firstNameVO, lastNameVO, emailVO, phoneNumberVO);
            } else {
                var userIdVO = UserId.of(userId);
                return User.reconstitute(userIdVO, firstNameVO, lastNameVO, emailVO, phoneNumberVO);
            }
        }

        public JdbcUserEntity buildJdbc() {
            return new JdbcUserEntity(null, firstName, lastName, email, phoneNumber);
        }

    }

}

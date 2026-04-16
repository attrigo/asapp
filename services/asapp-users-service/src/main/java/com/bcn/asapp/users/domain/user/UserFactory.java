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

package com.bcn.asapp.users.domain.user;

import java.util.UUID;

/**
 * Creates {@link User} instances from raw primitive values.
 * <p>
 * Constructs domain value objects internally and delegates to {@link User}'s package-private factory methods, keeping primitive-to-value-object translation in
 * one place.
 *
 * @since 0.2.0
 * @author attrigo
 */
public final class UserFactory {

    private UserFactory() {}

    /**
     * Creates a new user from raw values without a persistent ID.
     *
     * @param firstName   the user's first name
     * @param lastName    the user's last name
     * @param email       the user's email
     * @param phoneNumber the user's phone number
     * @return a new {@link User} instance
     * @throws IllegalArgumentException if any parameter is invalid
     */
    public static User create(String firstName, String lastName, String email, String phoneNumber) {
        var firstNameVO = FirstName.of(firstName);
        var lastNameVO = LastName.of(lastName);
        var emailVO = Email.of(email);
        var phoneNumberVO = PhoneNumber.of(phoneNumber);

        return User.create(firstNameVO, lastNameVO, emailVO, phoneNumberVO);
    }

    /**
     * Reconstitutes a user from raw values with a persistent ID.
     *
     * @param id          the user's unique identifier
     * @param firstName   the user's first name
     * @param lastName    the user's last name
     * @param email       the user's email
     * @param phoneNumber the user's phone number
     * @return a reconstituted {@link User} instance
     * @throws IllegalArgumentException if any parameter is invalid
     */
    public static User reconstitute(UUID id, String firstName, String lastName, String email, String phoneNumber) {
        var idVO = UserId.of(id);
        var firstNameVO = FirstName.of(firstName);
        var lastNameVO = LastName.of(lastName);
        var emailVO = Email.of(email);
        var phoneNumberVO = PhoneNumber.of(phoneNumber);

        return User.reconstitute(idVO, firstNameVO, lastNameVO, emailVO, phoneNumberVO);
    }

}

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

package com.bcn.asapp.users.domain.user;

import java.util.Objects;

/**
 * Represents a user entity.
 * <p>
 * This aggregate root encapsulates user identity and common user information.
 * <p>
 * Users can exist in two states: new (transient, without ID) and reconstructed (persistent, with ID).
 * <p>
 * Equality is based on ID; new instances are not considered equal to any other instance.
 *
 * @since 0.2.0
 * @author attrigo
 */
public final class User {

    private final UserId id;

    private FirstName firstName;

    private LastName lastName;

    private Email email;

    private PhoneNumber phoneNumber;

    /**
     * Constructs a new {@code User} instance and validates its integrity.
     *
     * @param firstName   the user's first name
     * @param lastName    the user's last name
     * @param email       the user's email
     * @param phoneNumber the user's phone number
     * @throws IllegalArgumentException if any parameter is {@code null}
     */
    private User(FirstName firstName, LastName lastName, Email email, PhoneNumber phoneNumber) {
        validateFirstNameIsNotNull(firstName);
        validateLastNameIsNotNull(lastName);
        validateEmailIsNotNull(email);
        validatePhoneNumberIsNotNull(phoneNumber);
        this.id = null;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phoneNumber = phoneNumber;
    }

    /**
     * Constructs a new reconstructed {@code User} instance and validates its integrity.
     *
     * @param id          the user's unique identifier
     * @param firstName   the user's first name
     * @param lastName    the user's last name
     * @param email       the user's email
     * @param phoneNumber the user's phone number
     * @throws IllegalArgumentException if any parameter is {@code null}
     */
    private User(UserId id, FirstName firstName, LastName lastName, Email email, PhoneNumber phoneNumber) {
        validateIdIsNotNull(id);
        validateFirstNameIsNotNull(firstName);
        validateLastNameIsNotNull(lastName);
        validateEmailIsNotNull(email);
        validatePhoneNumberIsNotNull(phoneNumber);
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phoneNumber = phoneNumber;
    }

    /**
     * Factory method to create a user without a persistent ID.
     * <p>
     * Typically used when registering a new user before persistence.
     *
     * @param firstName   the user's first name
     * @param lastName    the user's last name
     * @param email       the user's email
     * @param phoneNumber the user's phone number
     * @return a new {@code User} instance
     * @throws IllegalArgumentException if any parameter is {@code null}
     */
    // TODO: Think better name
    public static User newUser(FirstName firstName, LastName lastName, Email email, PhoneNumber phoneNumber) {
        return new User(firstName, lastName, email, phoneNumber);
    }

    /**
     * Factory method to create a user with a persistent ID.
     * <p>
     * Typically used when reconstituting a user from the database.
     *
     * @param id          the user's unique identifier
     * @param firstName   the user's first name
     * @param lastName    the user's last name
     * @param email       the user's email
     * @param phoneNumber the user's phone number
     * @return a new reconstructed {@code User} instance
     * @throws IllegalArgumentException if any parameter is {@code null}
     */
    // TODO: Think better name
    public static User reconstructedUser(UserId id, FirstName firstName, LastName lastName, Email email, PhoneNumber phoneNumber) {
        return new User(id, firstName, lastName, email, phoneNumber);
    }

    /**
     * Updates the user's information.
     *
     * @param firstName   the new first name
     * @param lastName    the new last name
     * @param email       the new email
     * @param phoneNumber the new phoneNumber
     * @throws IllegalArgumentException if any parameter is {@code null}
     */
    public void update(FirstName firstName, LastName lastName, Email email, PhoneNumber phoneNumber) {
        validateFirstNameIsNotNull(firstName);
        validateLastNameIsNotNull(lastName);
        validateEmailIsNotNull(email);
        validatePhoneNumberIsNotNull(phoneNumber);
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phoneNumber = phoneNumber;
    }

    /**
     * Determines equality based on user state.
     * <p>
     * Two {@code User} instances are equal only if both have non-null IDs that match.
     * <p>
     * Non-persisted instances are never equal to any other instance.
     *
     * @param object the object to compare
     * @return {@code true} if equal, {@code false} otherwise
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        User other = (User) object;
        if (this.id == null || other.id == null) {
            return false;
        }
        return Objects.equals(this.id, other.id);
    }

    /**
     * Generates hash code based on user ID.
     * <p>
     * Uses ID for reconstructed instances and identity hash code for new instances.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return this.id != null ? Objects.hashCode(this.id) : System.identityHashCode(this);
    }

    /**
     * Returns the user's unique identifier.
     *
     * @return the {@link UserId}, or {@code null} for new users
     */
    public UserId getId() {
        return this.id;
    }

    /**
     * Returns the user's first name.
     *
     * @return the {@link FirstName}
     */
    public FirstName getFirstName() {
        return this.firstName;
    }

    /**
     * Returns the user's last name.
     *
     * @return the {@link LastName}
     */
    public LastName getLastName() {
        return this.lastName;
    }

    /**
     * Returns the user's email.
     *
     * @return the {@link Email}
     */
    public Email getEmail() {
        return this.email;
    }

    /**
     * Returns the user's phone number.
     *
     * @return the {@link PhoneNumber}
     */
    public PhoneNumber getPhoneNumber() {
        return this.phoneNumber;
    }

    /**
     * Validates that the user ID is not {@code null}.
     *
     * @param id the ID to validate
     * @throws IllegalArgumentException if the ID is {@code null}
     */
    private static void validateIdIsNotNull(UserId id) {
        if (id == null) {
            throw new IllegalArgumentException("ID must not be null");
        }
    }

    /**
     * Validates that the first name is not {@code null}.
     *
     * @param firstName the first name to validate
     * @throws IllegalArgumentException if the first name is {@code null}
     */
    private static void validateFirstNameIsNotNull(FirstName firstName) {
        if (firstName == null) {
            throw new IllegalArgumentException("First name must not be null");
        }
    }

    /**
     * Validates that the last name is not {@code null}.
     *
     * @param lastName the last name to validate
     * @throws IllegalArgumentException if the last name is {@code null}
     */
    private static void validateLastNameIsNotNull(LastName lastName) {
        if (lastName == null) {
            throw new IllegalArgumentException("Last name must not be null");
        }
    }

    /**
     * Validates that the email is not {@code null}.
     *
     * @param email the email to validate
     * @throws IllegalArgumentException if the email is {@code null}
     */
    private static void validateEmailIsNotNull(Email email) {
        if (email == null) {
            throw new IllegalArgumentException("Email must not be null");
        }
    }

    /**
     * Validates that the phoneNumber is not {@code null}.
     *
     * @param phoneNumber the phoneNumber to validate
     * @throws IllegalArgumentException if the phoneNumber is {@code null}
     */
    private static void validatePhoneNumberIsNotNull(PhoneNumber phoneNumber) {
        if (phoneNumber == null) {
            throw new IllegalArgumentException("Phone number must not be null");
        }
    }

}

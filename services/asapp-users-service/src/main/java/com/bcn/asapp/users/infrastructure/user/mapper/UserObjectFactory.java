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

package com.bcn.asapp.users.infrastructure.user.mapper;

import org.mapstruct.ObjectFactory;
import org.springframework.stereotype.Component;

import com.bcn.asapp.users.domain.user.Email;
import com.bcn.asapp.users.domain.user.FirstName;
import com.bcn.asapp.users.domain.user.LastName;
import com.bcn.asapp.users.domain.user.PhoneNumber;
import com.bcn.asapp.users.domain.user.User;
import com.bcn.asapp.users.domain.user.UserId;
import com.bcn.asapp.users.infrastructure.user.persistence.JdbcUserEntity;

/**
 * MapStruct object factory for mapping between {@link User} domain entities and {@link JdbcUserEntity} database entities.
 * <p>
 * Ensures that domain entities are created through their proper factory methods with complete validation, maintaining domain integrity during mapping.
 *
 * @since 0.2.0
 * @author attrigo
 */
@Component
public class UserObjectFactory {

    /**
     * Creates a domain {@link User} from a database {@link JdbcUserEntity} entity.
     * <p>
     * Maps entity fields to value objects and reconstitutes a user using the domain's factory method.
     *
     * @param source the {@link JdbcUserEntity} database entity
     * @return the {@link User} domain entity
     */
    @ObjectFactory
    public User toUser(JdbcUserEntity source) {
        var userId = UserId.of(source.id());
        var firstName = FirstName.of(source.firstName());
        var lastName = LastName.of(source.lastName());
        var email = Email.of(source.email());
        var phoneNumber = PhoneNumber.of(source.phoneNumber());

        return User.reconstitute(userId, firstName, lastName, email, phoneNumber);
    }

}

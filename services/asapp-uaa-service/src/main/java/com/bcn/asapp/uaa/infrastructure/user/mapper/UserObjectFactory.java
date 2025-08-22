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

package com.bcn.asapp.uaa.infrastructure.user.mapper;

import org.mapstruct.ObjectFactory;
import org.springframework.stereotype.Component;

import com.bcn.asapp.uaa.domain.user.User;
import com.bcn.asapp.uaa.infrastructure.user.out.entity.UserEntity;

/**
 * MapStruct object factory for constructing {@link User} domain entities from database entities.
 * <p>
 * Ensures that domain entities are created through their proper factory methods with complete validation, maintaining domain integrity during mapping.
 *
 * @since 0.2.0
 * @author attrigo
 */
@Component
public class UserObjectFactory {

    private final UserIdMapper userIdMapper;

    private final UsernameMapper usernameMapper;

    private final RoleMapper roleMapper;

    /**
     * Constructs a new {@code UserObjectFactory} with required mappers.
     *
     * @param userIdMapper   the mapper for user IDs
     * @param usernameMapper the mapper for usernames
     * @param roleMapper     the mapper for roles
     */
    public UserObjectFactory(UserIdMapper userIdMapper, UsernameMapper usernameMapper, RoleMapper roleMapper) {
        this.userIdMapper = userIdMapper;
        this.usernameMapper = usernameMapper;
        this.roleMapper = roleMapper;
    }

    /**
     * Creates a domain {@link User} from a database {@link UserEntity} entity.
     * <p>
     * Maps entity fields to value objects and constructs an active user using the domain's factory method.
     *
     * @param source the {@link UserEntity} database entity
     * @return the {@link User} domain entity
     */
    @ObjectFactory
    public User toUser(UserEntity source) {
        var userId = userIdMapper.toUserId(source.id());
        var username = usernameMapper.toUsername(source.username());
        var role = roleMapper.toRole(source.role());

        return User.activeUser(userId, username, role);
    }

}

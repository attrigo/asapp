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

package com.bcn.asapp.users.infrastructure.user.mapper;

import java.util.UUID;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.bcn.asapp.users.application.user.in.command.CreateUserCommand;
import com.bcn.asapp.users.application.user.in.command.UpdateUserCommand;
import com.bcn.asapp.users.application.user.in.result.UserWithTasksResult;
import com.bcn.asapp.users.domain.user.User;
import com.bcn.asapp.users.infrastructure.user.in.request.CreateUserRequest;
import com.bcn.asapp.users.infrastructure.user.in.request.UpdateUserRequest;
import com.bcn.asapp.users.infrastructure.user.in.response.CreateUserResponse;
import com.bcn.asapp.users.infrastructure.user.in.response.GetAllUsersResponse;
import com.bcn.asapp.users.infrastructure.user.in.response.GetUserByIdResponse;
import com.bcn.asapp.users.infrastructure.user.in.response.UpdateUserResponse;
import com.bcn.asapp.users.infrastructure.user.persistence.JdbcUserEntity;

/**
 * MapStruct mapper for mapping between user-related objects.
 * <p>
 * Handles mappings between REST requests, commands, domain entities, database entities, and responses.
 * <p>
 * Uses custom object factories and component mappers for complex value object transformations.
 *
 * @since 0.2.0
 * @author attrigo
 */
@Mapper(componentModel = "spring", uses = { UserObjectFactory.class, UserIdMapper.class, FirstNameMapper.class, LastNameMapper.class, EmailMapper.class,
        PhoneNumberMapper.class })
public interface UserMapper {

    /**
     * Maps a {@link CreateUserRequest} to a {@link CreateUserCommand}.
     *
     * @param request the {@link CreateUserRequest}
     * @return the {@link CreateUserCommand}
     */
    CreateUserCommand toCreateUserCommand(CreateUserRequest request);

    /**
     * Maps a {@link UpdateUserRequest} and the user ID to a {@link UpdateUserCommand}.
     *
     * @param userId  the user's unique identifier
     * @param request the {@link UpdateUserRequest}
     * @return the {@link UpdateUserCommand}
     */
    UpdateUserCommand toUpdateUserCommand(UUID userId, UpdateUserRequest request);

    /**
     * Maps a domain {@link User} to a database {@link JdbcUserEntity} entity.
     *
     * @param user the {@link User} domain entity
     * @return the {@link JdbcUserEntity} database entity
     */
    JdbcUserEntity toJdbcUserEntity(User user);

    /**
     * Maps a database {@link JdbcUserEntity} entity to a domain {@link User}.
     * <p>
     * Uses {@link UserObjectFactory} to construct the domain entity with proper validation.
     *
     * @param jdbcUserEntity the {@link JdbcUserEntity} database entity
     * @return the {@link User} domain entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "firstName", ignore = true)
    @Mapping(target = "lastName", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "phoneNumber", ignore = true)
    User toUser(JdbcUserEntity jdbcUserEntity);

    /**
     * Maps a {@link UserWithTasksResult} to a {@link GetUserByIdResponse}.
     * <p>
     * Combines user information with task references into a single response.
     *
     * @param result the {@link UserWithTasksResult} containing user and task information
     * @return the {@link GetUserByIdResponse} with user data and task IDs
     */
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "firstName", source = "user.firstName")
    @Mapping(target = "lastName", source = "user.lastName")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "phoneNumber", source = "user.phoneNumber")
    @Mapping(target = "taskIds", source = "taskIds")
    GetUserByIdResponse toGetUserByIdResponse(UserWithTasksResult result);

    /**
     * Maps a domain {@link User} to a {@link GetAllUsersResponse}.
     *
     * @param user the {@link User} domain entity
     * @return the {@link GetAllUsersResponse}
     */
    @Mapping(target = "userId", source = "id")
    GetAllUsersResponse toGetAllUsersResponse(User user);

    /**
     * Maps a domain {@link User} to a {@link CreateUserResponse}.
     *
     * @param user the {@link User} domain entity
     * @return the {@link CreateUserResponse}
     */
    @Mapping(target = "userId", source = "id")
    CreateUserResponse toCreateUserResponse(User user);

    /**
     * Maps a domain {@link User} to a {@link UpdateUserResponse}.
     *
     * @param user the {@link User} domain entity
     * @return the {@link UpdateUserResponse}
     */
    @Mapping(target = "userId", source = "id")
    UpdateUserResponse toUpdateUserResponse(User user);

}

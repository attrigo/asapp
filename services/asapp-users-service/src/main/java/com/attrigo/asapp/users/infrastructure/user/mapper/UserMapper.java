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

package com.attrigo.asapp.users.infrastructure.user.mapper;

import java.util.List;
import java.util.UUID;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import com.attrigo.asapp.users.application.user.in.command.CreateUserCommand;
import com.attrigo.asapp.users.application.user.in.command.UpdateUserCommand;
import com.attrigo.asapp.users.application.user.in.result.UserWithTasksResult;
import com.attrigo.asapp.users.domain.user.User;
import com.attrigo.asapp.users.infrastructure.user.in.request.CreateUserRequest;
import com.attrigo.asapp.users.infrastructure.user.in.request.UpdateUserRequest;
import com.attrigo.asapp.users.infrastructure.user.in.response.CreateUserResponse;
import com.attrigo.asapp.users.infrastructure.user.in.response.GetUserByIdResponse;
import com.attrigo.asapp.users.infrastructure.user.in.response.GetUsersResponse;
import com.attrigo.asapp.users.infrastructure.user.in.response.UpdateUserResponse;
import com.attrigo.asapp.users.infrastructure.user.in.response.WarningDetail;
import com.attrigo.asapp.users.infrastructure.user.persistence.JdbcUserEntity;

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
     * @return the {@link GetUserByIdResponse} with user data, task IDs, and any degradation warnings
     */
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "firstName", source = "user.firstName")
    @Mapping(target = "lastName", source = "user.lastName")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "phoneNumber", source = "user.phoneNumber")
    @Mapping(target = "taskIds", source = "taskIds", qualifiedByName = "toTaskIdsUUID")
    @Mapping(target = "warnings", source = "tasksServiceAvailable")
    GetUserByIdResponse toGetUserByIdResponse(UserWithTasksResult result);

    /**
     * Maps a domain {@link User} to a {@link GetUsersResponse}.
     *
     * @param user the {@link User} domain entity
     * @return the {@link GetUsersResponse}
     */
    @Mapping(target = "userId", source = "id")
    GetUsersResponse toGetUsersResponse(User user);

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

    /**
     * Maps the task identifiers to the response task ID list.
     *
     * @param taskIds the task identifiers
     * @return the task identifiers, or an empty list when {@code null}
     */
    @Named("toTaskIdsUUID")
    default List<UUID> toTaskIdsUUID(List<UUID> taskIds) {
        return taskIds != null ? taskIds : List.of();
    }

    /**
     * Maps the tasks-service availability flag to the response warnings.
     *
     * @param tasksServiceAvailable the tasks-service availability flag
     * @return an empty list when available, or a single {@link WarningDetail} when not
     */
    default List<WarningDetail> toWarningDetails(boolean tasksServiceAvailable) {
        return tasksServiceAvailable ? List.of() : List.of(WarningDetail.Reason.TASK_IDS_UNAVAILABLE.toDetail());
    }

}

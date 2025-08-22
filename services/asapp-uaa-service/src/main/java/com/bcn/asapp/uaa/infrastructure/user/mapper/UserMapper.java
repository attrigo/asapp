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

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.bcn.asapp.dto.user.UserDTO;
import com.bcn.asapp.uaa.domain.user.User;
import com.bcn.asapp.uaa.infrastructure.user.api.resource.CreateUserRequest;
import com.bcn.asapp.uaa.infrastructure.user.api.resource.CreateUserResponse;
import com.bcn.asapp.uaa.infrastructure.user.api.resource.GetAllUsersResponse;
import com.bcn.asapp.uaa.infrastructure.user.api.resource.GetUserByIdResponse;
import com.bcn.asapp.uaa.infrastructure.user.api.resource.UpdateUserRequest;
import com.bcn.asapp.uaa.infrastructure.user.api.resource.UpdateUserResponse;
import com.bcn.asapp.uaa.infrastructure.user.entity.UserEntity;

/**
 * Mapper interface for converting between {@link User} entities and {@link UserDTO} data transfer objects.
 * <p>
 * This interface leverages <a href="https://mapstruct.org/">MapStruct</a> for generating type-safe and performant mapping implementations at compile time.
 *
 * @author ttrigo
 * @since 0.2.0
 */
@Mapper(componentModel = "spring", uses = UserIdMapper.class)
public interface UserMapper {

    // Request -> User
    User toUser(CreateUserRequest request);

    User toUser(UpdateUserRequest request);

    // User -> UserEntity
    UserEntity toUserEntity(User user);

    // UserEntity -> User
    User toUser(UserEntity userEntity);

    // User-> Response
    @Mapping(target = "password", constant = "*****")
    GetUserByIdResponse toGetUserByIdResponse(User user);

    @Mapping(target = "password", constant = "*****")
    GetAllUsersResponse toGetAllUsersResponse(User user);

    @Mapping(target = "password", constant = "*****")
    CreateUserResponse toCreateUserResponse(User user);

    @Mapping(target = "password", constant = "*****")
    UpdateUserResponse toUpdateUserResponse(User user);

}

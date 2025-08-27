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

import com.bcn.asapp.uaa.domain.user.Role;
import com.bcn.asapp.uaa.domain.user.User;
import com.bcn.asapp.uaa.domain.user.UserId;
import com.bcn.asapp.uaa.infrastructure.user.api.resource.CreateUserRequest;
import com.bcn.asapp.uaa.infrastructure.user.api.resource.UpdateUserRequest;
import com.bcn.asapp.uaa.infrastructure.user.entity.UserEntity;

@Component
public class UserObjectFactory {

    private final UserIdMapper userIdMapper;

    public UserObjectFactory(UserIdMapper userIdMapper) {
        this.userIdMapper = userIdMapper;
    }

    @ObjectFactory
    public User create(CreateUserRequest request) {
        return new User(request.username(), request.password(), Role.valueOf(request.role()));
    }

    @ObjectFactory
    public User create(UpdateUserRequest request) {
        return new User(request.username(), request.password(), Role.valueOf(request.role()));
    }

    @ObjectFactory
    public User create(UserEntity entity) {
        UserId userId = userIdMapper.toUserId(entity.id());
        return new User(userId, entity.username(), entity.password(), entity.role());
    }

}

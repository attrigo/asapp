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

package com.bcn.asapp.uaa.infrastructure.user.spi;

import java.util.Collection;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.bcn.asapp.uaa.application.user.spi.UserRepository;
import com.bcn.asapp.uaa.domain.user.User;
import com.bcn.asapp.uaa.domain.user.UserId;
import com.bcn.asapp.uaa.infrastructure.user.mapper.UserMapper;

@Component
public class UserRepositoryAdapter implements UserRepository {

    private final UserJdbcRepository userRepository;

    private final UserMapper userMapper;

    public UserRepositoryAdapter(UserJdbcRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Override
    public Optional<User> findById(UserId userId) {
        return userRepository.findById(userId.id())
                             .map(userMapper::toUser);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username)
                             .map(userMapper::toUser);
    }

    @Override
    public Collection<User> findAll() {
        return userRepository.findAll()
                             .stream()
                             .map(userMapper::toUser)
                             .toList();
    }

    @Override
    public User save(User user) {
        var userEntity = userMapper.toUserEntity(user);
        var userEntitySaved = userRepository.save(userEntity);
        return userMapper.toUser(userEntitySaved);
    }

    @Override
    public Long deleteById(UserId userId) {
        return userRepository.deleteUserById(userId.id());
    }

}

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

package com.attrigo.asapp.users.infrastructure.user.out;

import java.util.Collection;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.attrigo.asapp.users.application.user.out.UserRepository;
import com.attrigo.asapp.users.domain.user.User;
import com.attrigo.asapp.users.domain.user.UserId;
import com.attrigo.asapp.users.infrastructure.user.mapper.UserMapper;
import com.attrigo.asapp.users.infrastructure.user.persistence.JdbcUserRepository;

/**
 * Adapter implementation of {@link UserRepository} for JDBC persistence.
 * <p>
 * Bridges the application layer with the infrastructure layer by translating domain operations to JDBC repository calls and mapping between domain entities and
 * database entities.
 *
 * @since 0.2.0
 * @author attrigo
 */
@Component
public class UserRepositoryAdapter implements UserRepository {

    private final JdbcUserRepository userRepository;

    private final UserMapper userMapper;

    /**
     * Constructs a new {@code UserRepositoryAdapter} with required dependencies.
     *
     * @param userRepository the Spring Data JDBC repository
     * @param userMapper     the mapper for converting between domain and database entities
     */
    public UserRepositoryAdapter(JdbcUserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Override
    public Optional<User> findById(UserId userId) {
        return userRepository.findById(userId.value())
                             .map(userMapper::toUser);
    }

    @Override
    public Collection<User> findByIds(Collection<UserId> userIds) {
        var ids = userIds.stream()
                         .map(UserId::value)
                         .toList();

        return userRepository.findAllById(ids)
                             .stream()
                             .map(userMapper::toUser)
                             .toList();
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
        var userToSave = userMapper.toJdbcUserEntity(user);

        var userSaved = userRepository.save(userToSave);

        return userMapper.toUser(userSaved);
    }

    @Override
    public Boolean deleteById(UserId userId) {
        return userRepository.deleteUserById(userId.value()) > 0;
    }

}

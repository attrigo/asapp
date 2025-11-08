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

package com.bcn.asapp.users.infrastructure.user.out;

import java.util.Collection;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.bcn.asapp.users.application.user.out.UserRepository;
import com.bcn.asapp.users.domain.user.User;
import com.bcn.asapp.users.domain.user.UserId;
import com.bcn.asapp.users.infrastructure.user.mapper.UserMapper;

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

    private final UserJdbcRepository userRepository;

    private final UserMapper userMapper;

    /**
     * Constructs a new {@code UserRepositoryAdapter} with required dependencies.
     *
     * @param userRepository the Spring Data JDBC repository
     * @param userMapper     the mapper for converting between domain and database entities
     */
    public UserRepositoryAdapter(UserJdbcRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    /**
     * Finds a user by their unique identifier.
     *
     * @param userId the user's unique identifier
     * @return an {@link Optional} containing the {@link User} if found, {@link Optional#empty} otherwise
     */
    @Override
    public Optional<User> findById(UserId userId) {
        return userRepository.findById(userId.value())
                             .map(userMapper::toUser);
    }

    /**
     * Retrieves all users from the repository.
     *
     * @return a {@link Collection} of all {@link User} entities
     */
    @Override
    public Collection<User> findAll() {
        return userRepository.findAll()
                             .stream()
                             .map(userMapper::toUser)
                             .toList();
    }

    /**
     * Saves a user to the repository.
     * <p>
     * If the user is new (without ID), it will be persisted and returned with a generated ID.
     * <p>
     * If the user is reconstituted (with ID), it will be updated.
     *
     * @param user the {@link User} to save
     * @return the saved {@link User} with a persistent ID
     */
    @Override
    public User save(User user) {
        var userEntity = userMapper.toUserEntity(user);

        var userEntitySaved = userRepository.save(userEntity);

        return userMapper.toUser(userEntitySaved);
    }

    /**
     * Deletes a user by their unique identifier.
     *
     * @param userId the user's unique identifier
     * @return {@code true} if the user was deleted, {@code false} if not found
     */
    @Override
    public Boolean deleteById(UserId userId) {
        return userRepository.deleteUserById(userId.value()) > 0;
    }

}

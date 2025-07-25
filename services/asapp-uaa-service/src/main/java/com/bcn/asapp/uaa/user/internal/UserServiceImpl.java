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
package com.bcn.asapp.uaa.user.internal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.bcn.asapp.dto.user.UserDTO;
import com.bcn.asapp.uaa.user.UserMapper;
import com.bcn.asapp.uaa.user.UserRepository;
import com.bcn.asapp.uaa.user.UserService;

/**
 * Service implementation for managing user operations.
 *
 * @author ttrigo
 * @since 0.2.0
 */
@Service
public class UserServiceImpl implements UserService {

    /**
     * Encoder used for hashing passwords
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * Repository for performing CRUD operations on user entities.
     */
    private final UserRepository userRepository;

    /**
     * Mapper for converting between {@link UserDTO} and {@link com.bcn.asapp.uaa.user.User} entities.
     */
    private final UserMapper userMapper;

    /**
     * Constructs a new {@code UserServiceImpl} with required dependencies.
     *
     * @param passwordEncoder the encoder used for hashing passwords
     * @param userMapper      the mapper for converting between user entities and DTOs
     * @param userRepository  the repository for performing CRUD operations on user entities
     */
    public UserServiceImpl(PasswordEncoder passwordEncoder, UserMapper userMapper, UserRepository userRepository) {
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        this.userRepository = userRepository;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<UserDTO> findById(UUID id) {
        return this.userRepository.findById(id)
                                  .map(userMapper::toUserDTO);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<UserDTO> findAll() {
        return userRepository.findAll()
                             .stream()
                             .map(userMapper::toUserDTO)
                             .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UserDTO create(UserDTO user) {
        var passwordEncoded = passwordEncoder.encode(user.password());

        var userWithoutId = userMapper.toUserIgnoreId(user, passwordEncoded);
        var userCreated = userRepository.save(userWithoutId);

        return userMapper.toUserDTO(userCreated);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<UserDTO> updateById(UUID id, UserDTO newUserData) {
        var userExists = userRepository.existsById(id);

        if (!userExists) {
            return Optional.empty();
        }

        var passwordEncoded = passwordEncoder.encode(newUserData.password());

        var user = userMapper.toUser(newUserData, id, passwordEncoded);
        var userUpdated = userRepository.save(user);

        var userDTOUpdated = userMapper.toUserDTO(userUpdated);
        return Optional.of(userDTOUpdated);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Boolean deleteById(UUID id) {
        return userRepository.deleteUserById(id) > 0;
    }

}

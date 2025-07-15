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
import com.bcn.asapp.uaa.security.core.UserRepository;
import com.bcn.asapp.uaa.user.UserMapper;
import com.bcn.asapp.uaa.user.UserService;

@Service
public class UserServiceImpl implements UserService {

    private final PasswordEncoder passwordEncoder;

    private final UserRepository userRepository;

    private final UserMapper userMapper;

    public UserServiceImpl(PasswordEncoder passwordEncoder, UserMapper userMapper, UserRepository userRepository) {
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        this.userRepository = userRepository;
    }

    @Override
    public Optional<UserDTO> findById(UUID id) {
        return this.userRepository.findById(id)
                                  .map(userMapper::toUserDTO);
    }

    @Override
    public List<UserDTO> findAll() {
        return userRepository.findAll()
                             .stream()
                             .map(userMapper::toUserDTO)
                             .toList();
    }

    @Override
    public UserDTO create(UserDTO user) {
        var passwordEncoded = passwordEncoder.encode(user.password());

        var userWithoutId = userMapper.toUserIgnoreId(user, passwordEncoded);
        var userCreated = userRepository.save(userWithoutId);

        return userMapper.toUserDTO(userCreated);
    }

    @Override
    public Optional<UserDTO> updateById(UUID id, UserDTO newUserData) {
        var userExists = userRepository.existsById(id);

        if (!userExists) {
            return Optional.empty();
        }

        var user = userMapper.toUser(newUserData, id);
        var userUpdated = userRepository.save(user);

        var userDTOUpdated = userMapper.toUserDTO(userUpdated);
        return Optional.of(userDTOUpdated);
    }

    @Override
    public Boolean deleteById(UUID id) {
        return userRepository.deleteUserById(id) > 0;
    }

}

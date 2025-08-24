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

package com.bcn.asapp.uaa.application.user.api.service;

import java.util.Optional;

import com.bcn.asapp.uaa.application.ApplicationService;
import com.bcn.asapp.uaa.application.user.api.UpdateUserUseCase;
import com.bcn.asapp.uaa.application.user.spi.UserPasswordEncoder;
import com.bcn.asapp.uaa.application.user.spi.UserRepository;
import com.bcn.asapp.uaa.domain.user.User;
import com.bcn.asapp.uaa.domain.user.UserId;

@ApplicationService
public class UpdateUserService implements UpdateUserUseCase {

    private final UserPasswordEncoder passwordEncoder;

    private final UserRepository userRepository;

    public UpdateUserService(UserPasswordEncoder passwordEncoder, UserRepository userRepository) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }

    @Override
    public Optional<User> updateUserById(UserId userId, User user) {
        var optionalUser = userRepository.findById(userId);
        if (optionalUser.isEmpty()) {
            return Optional.empty();
        }

        // TODO: What happens when the password could not be encoded
        var passwordEncoded = passwordEncoder.encode(user.getPassword());

        var currentUser = optionalUser.get();
        currentUser.setUsername(user.getUsername());
        currentUser.setPassword(passwordEncoded);
        currentUser.setRole(user.getRole());

        var userUpdated = userRepository.save(currentUser);

        return Optional.of(userUpdated);
    }

}

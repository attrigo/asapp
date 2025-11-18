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

package com.bcn.asapp.authentication.infrastructure.user.persistence;

import static com.bcn.asapp.authentication.testutil.TestFactory.TestUserFactory.defaultTestUser;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import com.bcn.asapp.authentication.infrastructure.authentication.persistence.JdbcJwtAuthenticationRepository;
import com.bcn.asapp.authentication.testutil.TestContainerConfiguration;

@DataJdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ TestContainerConfiguration.class, JacksonAutoConfiguration.class })
class JdbcUserRepositoryIT {

    @Autowired
    private JdbcJwtAuthenticationRepository jwtAuthenticationRepository;

    @Autowired
    private JdbcUserRepository userRepository;

    @BeforeEach
    void beforeEach() {
        jwtAuthenticationRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Nested
    class FindByUsername {

        @Test
        void DoesNotFindUserAndReturnsEmptyOptional_UserNotExists() {
            // When
            var actualUser = userRepository.findByUsername("not_exist_user@asapp.com");

            // Then
            assertThat(actualUser).isEmpty();
        }

        @Test
        void FindsUserAndReturnsTasksFound_UserExists() {
            // Given
            var user = defaultTestUser();
            var userCreated = userRepository.save(user);
            assertThat(userCreated).isNotNull();

            // When
            var username = userCreated.username();

            var actualUser = userRepository.findByUsername(username);

            // Then
            assertThat(actualUser).get()
                                  .isEqualTo(userCreated);
        }

    }

    @Nested
    class DeleteUserById {

        @Test
        void DoesNotDeleteUserAndReturnsZero_UserNotExists() {
            // When
            var userId = UUID.fromString("4a9d8f7e-3c2b-4e1f-a6d9-8e7f6c5d4b3a");

            var actual = userRepository.deleteUserById(userId);

            // Then
            assertThat(actual).isZero();
        }

        @Test
        void DeletesUserAndReturnsAmountOfUsersDeleted_UserExists() {
            // Given
            var user = defaultTestUser();
            var userCreated = userRepository.save(user);
            assertThat(userCreated).isNotNull();

            // When
            var userId = userCreated.id();

            var actual = userRepository.deleteUserById(userId);

            // Then
            assertThat(actual).isGreaterThan(0);
        }

    }

}

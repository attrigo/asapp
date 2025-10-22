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

import static com.bcn.asapp.users.testutil.TestDataFaker.UserDataFaker.defaultFakeUser;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import com.bcn.asapp.users.testutil.TestContainerConfiguration;

@DataJdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestContainerConfiguration.class)
class UserJdbcRepositoryIT {

    @Autowired
    private UserJdbcRepository userRepository;

    @BeforeEach
    void beforeEach() {
        userRepository.deleteAll();
    }

    @Nested
    class DeleteUserById {

        @Test
        void DoesNotDeleteUserAndReturnsZero_UserNotExists() {
            // When
            var userId = UUID.randomUUID();

            var actual = userRepository.deleteUserById(userId);

            // Then
            assertThat(actual).isZero();
        }

        @Test
        void DeletesUserAndReturnsAmountOfUsersDeleted_UserExists() {
            // Given
            var user = defaultFakeUser();
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

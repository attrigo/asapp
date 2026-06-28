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

package com.attrigo.asapp.users.infrastructure.user.in;

import static com.attrigo.asapp.users.testutil.fixture.UserMother.aUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.any;

import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.attrigo.asapp.users.application.user.in.ReadUserUseCase;
import com.attrigo.asapp.users.domain.user.User;
import com.attrigo.asapp.users.infrastructure.user.in.response.GetUsersResponse;
import com.attrigo.asapp.users.infrastructure.user.mapper.UserMapper;

/**
 * Tests {@link UserRestController} request dispatch between retrieving all users and retrieving users by identifiers.
 * <p>
 * Coverage:
 * <li>Retrieves all users when no identifiers are supplied</li>
 * <li>Retrieves only the requested users when identifiers are supplied</li>
 * <li>Skips the unused retrieval path on each branch</li>
 */
@ExtendWith(MockitoExtension.class)
class UserRestControllerTests {

    @Mock
    private ReadUserUseCase readUserUseCase;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserRestController userRestController;

    @Nested
    class GetUsers {

        @Test
        void ReturnsAllUsers_NoIds() {
            // Given
            var user = aUser();
            var response = buildGetUsersResponse(user);

            given(readUserUseCase.getAllUsers()).willReturn(List.of(user));
            given(userMapper.toGetUsersResponse(user)).willReturn(response);

            // When
            var actual = userRestController.getUsers(null);

            // Then
            assertThat(actual).containsExactly(response);

            then(readUserUseCase).should()
                                 .getAllUsers();
            then(readUserUseCase).should(never())
                                 .getUsersByIds(any());
        }

        @Test
        void ReturnsUsersByIds_WithIds() {
            // Given
            var user = aUser();
            var userIds = List.of(user.getId()
                                      .value());
            var response = buildGetUsersResponse(user);

            given(readUserUseCase.getUsersByIds(userIds)).willReturn(List.of(user));
            given(userMapper.toGetUsersResponse(user)).willReturn(response);

            // When
            var actual = userRestController.getUsers(userIds);

            // Then
            assertThat(actual).containsExactly(response);

            then(readUserUseCase).should()
                                 .getUsersByIds(userIds);
            then(readUserUseCase).should(never())
                                 .getAllUsers();
        }

    }

    private static GetUsersResponse buildGetUsersResponse(User user) {
        var id = user.getId();
        var firstName = user.getFirstName();
        var lastName = user.getLastName();
        var email = user.getEmail();
        var phoneNumber = user.getPhoneNumber();
        return new GetUsersResponse(id.value(), firstName.value(), lastName.value(), email.value(), phoneNumber.value());
    }

}

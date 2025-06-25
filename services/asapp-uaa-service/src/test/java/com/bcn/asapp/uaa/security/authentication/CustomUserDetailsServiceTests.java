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
package com.bcn.asapp.uaa.security.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.bcn.asapp.uaa.security.core.Role;
import com.bcn.asapp.uaa.security.core.User;
import com.bcn.asapp.uaa.security.core.UserRepository;

@ExtendWith(SpringExtension.class)
class CustomUserDetailsServiceTests {

    @Mock
    private UserRepository userRepositoryMock;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    private String fakeUsername;

    private String fakePassword;

    @BeforeEach
    void beforeEach() {
        this.fakeUsername = "TEST USERNAME";
        this.fakePassword = "TEST PASSWORD";
    }

    @Nested
    class LoadUserByUsername {

        @Test
        @DisplayName("GIVEN username does not exists WHEN load user by username THEN does not find the user And throws UsernameNotFoundException")
        void UsernameNotExists_LoadUserByUsername_DoesNotFindUserAndThrowsUsernameNotFoundException() {
            // Given
            given(userRepositoryMock.findByUsername(anyString())).willReturn(Optional.empty());

            // When
            var usernameToLoad = fakeUsername;

            Executable executable = () -> customUserDetailsService.loadUserByUsername(usernameToLoad);

            // Then
            assertThrows(UsernameNotFoundException.class, executable);

            then(userRepositoryMock).should(times(1))
                                    .findByUsername(anyString());
        }

        @Test
        @DisplayName("GIVEN username exists with USER role WHEN load user by username THEN finds the user And returns the UserDetails with USER authorities")
        void UsernameExistsWithUserRole_LoadUserByUsername_FindsUserAndReturnsTheUserDetails() {
            // Given
            User fakeUser = new User(UUID.randomUUID(), fakeUsername, fakePassword, Role.USER);
            given(userRepositoryMock.findByUsername(anyString())).willReturn(Optional.of(fakeUser));

            // When
            var usernameToLoad = fakeUsername;

            var actualUserDetails = customUserDetailsService.loadUserByUsername(usernameToLoad);

            // Then
            assertEquals(fakeUsername, actualUserDetails.getUsername());
            assertEquals(fakePassword, actualUserDetails.getPassword());
            assertTrue(actualUserDetails.getAuthorities()
                                        .stream()
                                        .findFirst()
                                        .isPresent());
            assertEquals(Role.USER.name(), actualUserDetails.getAuthorities()
                                                            .stream()
                                                            .findFirst()
                                                            .get()
                                                            .toString());

            then(userRepositoryMock).should(times(1))
                                    .findByUsername(anyString());
        }

        @Test
        @DisplayName("GIVEN username exists with ADMIN role WHEN load user by username THEN finds the user And returns the UserDetails with ADMIN authorities")
        void UsernameExistsWithAdminRole_LoadUserByUsername_FindsUserAndReturnsTheUserDetails() {
            // Given
            User fakeUser = new User(UUID.randomUUID(), fakeUsername, fakePassword, Role.ADMIN);
            given(userRepositoryMock.findByUsername(anyString())).willReturn(Optional.of(fakeUser));

            // When
            var usernameToLoad = fakeUsername;

            var actualUserDetails = customUserDetailsService.loadUserByUsername(usernameToLoad);

            // Then
            assertEquals(fakeUsername, actualUserDetails.getUsername());
            assertEquals(fakePassword, actualUserDetails.getPassword());
            assertTrue(actualUserDetails.getAuthorities()
                                        .stream()
                                        .findFirst()
                                        .isPresent());
            assertEquals(Role.ADMIN.name(), actualUserDetails.getAuthorities()
                                                             .stream()
                                                             .findFirst()
                                                             .get()
                                                             .toString());

            then(userRepositoryMock).should(times(1))
                                    .findByUsername(anyString());
        }

    }

}

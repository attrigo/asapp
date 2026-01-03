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

package com.bcn.asapp.authentication.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import java.util.Optional;
import java.util.UUID;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.bcn.asapp.authentication.domain.user.Role;
import com.bcn.asapp.authentication.infrastructure.user.persistence.JdbcUserEntity;
import com.bcn.asapp.authentication.infrastructure.user.persistence.JdbcUserRepository;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTests {

    @Mock
    private JdbcUserRepository userRepositoryMock;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    private final String usernameValue = "user@asapp.com";

    @Nested
    class LoadUserByUsername {

        @Test
        void ThrowsUsernameNotFoundException_UsernameNotExists() {
            // Given
            given(userRepositoryMock.findByUsername(usernameValue)).willReturn(Optional.empty());

            // When
            var thrown = catchThrowable(() -> customUserDetailsService.loadUserByUsername(usernameValue));

            // Then
            assertThat(thrown).isInstanceOf(UsernameNotFoundException.class)
                              .hasMessage("User not exists by username: user@asapp.com");

            then(userRepositoryMock).should(times(1))
                                    .findByUsername(usernameValue);
        }

        @ParameterizedTest
        @EnumSource(value = Role.class)
        void ReturnsCustomUserDetails_UsernameExistsWithRole(Role role) {
            // Given
            var userId = UUID.fromString("36f65be6-b9b7-43ef-9f89-b49a724aea0a");
            var password = "{bcrypt}password";
            var user = new JdbcUserEntity(userId, usernameValue, password, role.name());
            given(userRepositoryMock.findByUsername(usernameValue)).willReturn(Optional.of(user));

            // When
            var actual = customUserDetailsService.loadUserByUsername(usernameValue);

            // Then
            assertThat(actual).isNotNull()
                              .isInstanceOf(CustomUserDetails.class);
            assertThat(actual).asInstanceOf(InstanceOfAssertFactories.type(CustomUserDetails.class))
                              .extracting(CustomUserDetails::getUserId)
                              .isEqualTo(userId);
            assertThat(actual.getUsername()).isEqualTo(usernameValue);
            assertThat(actual.getPassword()).isEqualTo(password);
            assertThat(actual.getAuthorities()).hasSize(1)
                                               .extracting(GrantedAuthority::getAuthority)
                                               .containsExactly(role.name());

            then(userRepositoryMock).should(times(1))
                                    .findByUsername(usernameValue);
        }

    }

}

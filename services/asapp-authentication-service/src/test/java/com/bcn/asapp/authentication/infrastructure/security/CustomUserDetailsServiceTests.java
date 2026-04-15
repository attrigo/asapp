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

package com.bcn.asapp.authentication.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.times;

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

/**
 * Tests {@link CustomUserDetailsService} credential lookup and Spring Security UserDetails translation.
 * <p>
 * Coverage:
 * <li>Retrieves user by username from repository</li>
 * <li>Translates repository entity to Spring Security user details with authorities</li>
 * <li>Throws an exception when the username is not registered</li>
 * <li>Supports all role types (USER, ADMIN)</li>
 */
@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTests {

    @Mock
    private JdbcUserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Nested
    class LoadUserByUsername {

        @ParameterizedTest
        @EnumSource(value = Role.class)
        void ReturnsCustomUserDetails_UsernameExistsWithRole(Role role) {
            // Given
            var userId = UUID.fromString("36f65be6-b9b7-43ef-9f89-b49a724aea0a");
            var username = "user@asapp.com";
            var password = "{bcrypt}password";
            var user = new JdbcUserEntity(userId, username, password, role.name());

            given(userRepository.findByUsername(username)).willReturn(Optional.of(user));

            // When
            var actual = customUserDetailsService.loadUserByUsername(username);

            // Then
            assertSoftly(softly -> {
                // @formatter:off
                softly.assertThat(actual).as("user details").isNotNull().isInstanceOf(CustomUserDetails.class);
                softly.assertThat(actual).as("user ID").asInstanceOf(InstanceOfAssertFactories.type(CustomUserDetails.class)).extracting(CustomUserDetails::getUserId).isEqualTo(userId);
                softly.assertThat(actual.getUsername()).as("username").isEqualTo(username);
                softly.assertThat(actual.getPassword()).as("password").isEqualTo(password);
                softly.assertThat(actual.getAuthorities()).as("authorities").hasSize(1).extracting(GrantedAuthority::getAuthority).containsExactly(role.name());
                // @formatter:on
            });

            then(userRepository).should(times(1))
                                .findByUsername(username);
        }

        @Test
        void ThrowsUsernameNotFoundException_UsernameNotExists() {
            // Given
            var username = "user@asapp.com";

            given(userRepository.findByUsername(username)).willReturn(Optional.empty());

            // When
            var actual = catchThrowable(() -> customUserDetailsService.loadUserByUsername(username));

            // Then
            assertThat(actual).isInstanceOf(UsernameNotFoundException.class)
                              .hasMessage("User not exists by username: user@asapp.com");

            then(userRepository).should(times(1))
                                .findByUsername(username);
        }

    }

}

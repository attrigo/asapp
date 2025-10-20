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

import static com.bcn.asapp.authentication.domain.user.Role.ADMIN;
import static com.bcn.asapp.authentication.domain.user.Role.USER;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.bcn.asapp.authentication.domain.user.Role;

public class CustomUserDetailsTests {

    private final UUID userIdValue = UUID.randomUUID();

    private final String usernameValue = "user@asapp.com";

    private final String passwordValue = "{bcrypt}encodedPassword";

    private final Collection<? extends GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(USER.name()));

    @Nested
    class CreateCustomUserDetails {

        @ParameterizedTest
        @EnumSource(value = Role.class)
        void ThenReturnsCustomUserDetails_GivenAllParametersAreValid(Role role) {
            // When
            var actual = new CustomUserDetails(userIdValue, usernameValue, passwordValue, AuthorityUtils.createAuthorityList(role.name()));

            // Then
            assertThat(actual).isNotNull();
            assertThat(actual.getUserId()).isEqualTo(userIdValue);
            assertThat(actual.getUsername()).isEqualTo(usernameValue);
            assertThat(actual.getPassword()).isEqualTo(passwordValue);
            assertThat(actual.getAuthorities()).hasSize(1)
                                               .extracting(GrantedAuthority::getAuthority)
                                               .containsExactly(role.name());
        }

        @Test
        void ThenReturnsCustomUserDetailsWithMultipleAuthorities_GivenMultipleAuthorities() {
            // Given
            var multipleAuthorities = AuthorityUtils.createAuthorityList(USER.name(), ADMIN.name());

            // When
            var actual = new CustomUserDetails(userIdValue, usernameValue, passwordValue, multipleAuthorities);

            // Then
            assertThat(actual.getAuthorities()).hasSize(2)
                                               .extracting(GrantedAuthority::getAuthority)
                                               .containsExactlyInAnyOrder(USER.name(), ADMIN.name());
        }

        @Test
        void ThenReturnsCustomUserDetailsWithEmptyAuthorities_GivenWithEmptyAuthorities() {
            // Given
            var emptyAuthorities = AuthorityUtils.NO_AUTHORITIES;

            // When
            var actual = new CustomUserDetails(userIdValue, usernameValue, passwordValue, emptyAuthorities);

            // Then
            assertThat(actual.getAuthorities()).isEmpty();
        }

    }

    @Nested
    class GetUserId {

        @Test
        void ThenReturnsUserId() {
            // Given
            var userDetails = new CustomUserDetails(userIdValue, usernameValue, passwordValue, authorities);

            // When
            var actual = userDetails.getUserId();

            // Then
            assertThat(actual).isEqualTo(userIdValue);
        }

    }

}
